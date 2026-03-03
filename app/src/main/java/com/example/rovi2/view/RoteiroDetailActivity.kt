package com.example.rovi2.view

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.rovi2.controller.RoteiroDetailViewModel
import com.example.rovi2.model.Etapa
import com.example.rovi2.ui.theme.Rovi2Theme
import java.util.UUID
import com.google.gson.Gson
import androidx.core.content.FileProvider
import java.io.File

class RoteiroDetailActivity : ComponentActivity() {

    private class RoteiroDetailViewModelFactory(
        private val activity: ComponentActivity,
        private val roteiroId: Int
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RoteiroDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RoteiroDetailViewModel(activity.application, roteiroId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private val viewModel: RoteiroDetailViewModel by viewModels {
        val roteiroId = intent.getIntExtra("ROTEIRO_ID", -1)
        if (roteiroId == -1) {
            Toast.makeText(this, "Erro: Roteiro não encontrado", Toast.LENGTH_LONG).show()
            finish()
        }
        RoteiroDetailViewModelFactory(this, roteiroId)
    }

    private var onEtapaLocalizacaoAtualizada: ((String, Double, Double, String?) -> Unit)? = null

    private val mapResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val latitude = data?.getDoubleExtra("SELECTED_LATITUDE", 0.0) ?: 0.0
                val longitude = data?.getDoubleExtra("SELECTED_LONGITUDE", 0.0) ?: 0.0
                val localNome = data?.getStringExtra("SELECTED_NAME")
                val etapaLocalId = data?.getStringExtra("ETAPA_LOCAL_ID")

                if (etapaLocalId != null) {
                    onEtapaLocalizacaoAtualizada?.invoke(etapaLocalId, latitude, longitude, localNome)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Rovi2Theme {
                val roteiroState by viewModel.roteiro.collectAsState()

                val etapasOriginais by viewModel.etapas.collectAsState()

                var modoEdicao by remember { mutableStateOf(false) }

                var nome by remember(roteiroState) { mutableStateOf(roteiroState?.nome ?: "") }
                var descricao by remember(roteiroState) { mutableStateOf(roteiroState?.descricao ?: "") }

                var etapasLocais by remember { mutableStateOf(listOf<Etapa>()) }

                //Sincroniza as listas quando o modo de edição muda
                LaunchedEffect(modoEdicao, etapasOriginais) {
                    if (modoEdicao) {
                        etapasLocais = etapasOriginais
                    }
                }

                onEtapaLocalizacaoAtualizada = { localId, lat, lon, nomeLocal ->
                    etapasLocais = etapasLocais.map {
                        if (it.localId == localId) {
                            it.copy(latitude = lat, longitude = lon, localNome = nomeLocal)
                        } else {
                            it
                        }
                    }
                }

                roteiroState?.let { roteiro ->
                    RoteiroDetailScreen(
                        nome = nome,
                        onNomeChange = { nome = it },
                        descricao = descricao,
                        onDescChange = { descricao = it },

                        etapas = if (modoEdicao) etapasLocais else etapasOriginais,

                        modoEdicao = modoEdicao,
                        onModoEdicaoChange = { modoEdicao = it },
                        onBackClicked = { finish() },
                        onDeleteClicked = {
                            viewModel.deleteRoteiro()
                            Toast.makeText(this, "Roteiro excluído", Toast.LENGTH_SHORT).show()
                            finish()
                        },

                        onSaveClicked = {
                            viewModel.updateRoteiro(nome, descricao, etapasLocais)
                            modoEdicao = false //Sai modo edição
                            Toast.makeText(this, "Roteiro salvo!", Toast.LENGTH_SHORT).show()
                        },

                        onAdicionarEtapa = {
                            etapasLocais = etapasLocais + Etapa()
                        },
                        onRemoverEtapa = { etapa ->
                            etapasLocais = etapasLocais.filterNot { it.localId == etapa.localId }
                        },
                        onAtualizarEtapa = { index, etapa ->
                            etapasLocais = etapasLocais.toMutableList().also {
                                if (index in it.indices) {
                                    it[index] = etapa
                                }
                            }
                        },

                        onMoverEtapa = { from, to ->
                            viewModel.moverEtapa(from, to)
                        },

                        onLaunchMaps = { etapaLocalId ->
                            val intent = Intent(this, MapsActivity::class.java)
                            intent.putExtra("REQUEST_CODE", MapsActivity.REQUEST_SELECT_LOCATION)
                            intent.putExtra("ETAPA_LOCAL_ID", etapaLocalId)
                            mapResultLauncher.launch(intent)
                        },
                        //TESTE INSANO (se isso funcionar eu vou me embebedar)
                        //Nao vais ser bluetooh mais aliás.Vai ser via nearbyshare
                        onShareClicked = {
                            //Toast.makeText(this, "Share ainda nao feito", Toast.LENGTH_SHORT).show()
                            try {
                                //Converter o roteiro para JSON
                                val gson = Gson()
                                val roteiroJson = gson.toJson(roteiro)

                                //Salvar o JSON em um arquivo .rovi no cache
                                val file = File(cacheDir, "${roteiro.nome.replace(" ", "_")}.rovi")
                                file.writeText(roteiroJson)

                                //Obter a URI segura usando o FileProvider
                                val uri = FileProvider.getUriForFile(
                                    this@RoteiroDetailActivity,
                                    "com.example.rovi2.fileprovider", // O 'authorities' do Manifest
                                    file
                                )

                                //Criar o Intent de compartilhamento
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream" // Tipo genérico de arquivo
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Compartilhando Roteiro: ${roteiro.nome}")
                                    //Dá permissão temporária de leitura
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                //Lançar o Sharesheet
                                startActivity(Intent.createChooser(shareIntent, "Compartilhar Roteiro via..."))

                            } catch (e: Exception) {
                                Toast.makeText(this, "Erro ao compartilhar: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

private fun launchGoogleMapsNavigation(context: Context, lat: Double?, lon: Double?) {
    if (lat == null || lon == null || (lat == 0.0 && lon == 0.0)) {
        Toast.makeText(context, "Localização não definida para esta etapa", Toast.LENGTH_SHORT).show()
        return
    }
    val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "App Google Maps não encontrado. Abrindo no navegador...", Toast.LENGTH_LONG).show()
        val webIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lon")
        val webIntent = Intent(Intent.ACTION_VIEW, webIntentUri)
        context.startActivity(webIntent)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoteiroDetailScreen(
    nome: String,
    onNomeChange: (String) -> Unit,
    descricao: String,
    onDescChange: (String) -> Unit,
    etapas: List<Etapa>,
    modoEdicao: Boolean,
    onModoEdicaoChange: (Boolean) -> Unit,
    onBackClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onAdicionarEtapa: () -> Unit,
    onRemoverEtapa: (Etapa) -> Unit,
    onAtualizarEtapa: (Int, Etapa) -> Unit,
    onMoverEtapa: (Int, Int) -> Unit,
    onLaunchMaps: (String) -> Unit
) {
    val context = LocalContext.current
    var mostrarDialogoExclusao by remember { mutableStateOf(false) }

    if (mostrarDialogoExclusao) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoExclusao = false },
            title = { Text(text = "Excluir Roteiro?") },
            text = { Text("Você tem certeza que deseja excluir este roteiro permanentemente?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoExclusao = false
                    onDeleteClicked()
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoExclusao = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (modoEdicao) "Editando Roteiro" else nome) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) { Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                actions = {
                    IconButton(onClick = onShareClicked) { Icon(Icons.Filled.Share, contentDescription = "Compartilhar") }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { mostrarDialogoExclusao = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = {
                        if (modoEdicao) onSaveClicked() else onModoEdicaoChange(true)
                    }) {
                        Icon(
                            if (modoEdicao) Icons.Filled.Save else Icons.Filled.Edit,
                            contentDescription = if (modoEdicao) "Salvar" else "Editar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = nome, onValueChange = onNomeChange, label = { Text("Nome do Roteiro") },
                    modifier = Modifier.fillMaxWidth(), readOnly = !modoEdicao
                )
            }
            item {
                OutlinedTextField(
                    value = descricao, onValueChange = onDescChange, label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth().height(120.dp), readOnly = !modoEdicao
                )
            }
            item { Text(text = "Minha Rotina", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }


            itemsIndexed(etapas) { index, etapa ->
                EtapaItemCard(
                    etapa = etapa,
                    modoEdicao = modoEdicao,
                    index = index,
                    totalItens = etapas.size,
                    onMoveUp = { if (modoEdicao) onMoverEtapa(index, index - 1) },
                    onMoveDown = { if (modoEdicao) onMoverEtapa(index, index + 1) },
                    onDescChange = { newDesc ->
                        if (modoEdicao) onAtualizarEtapa(index, etapa.copy(descricao = newDesc))
                    },
                    onTimeClick = {
                        if (modoEdicao) {
                            val timePickerDialog = TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val horaStr = hourOfDay.toString().padStart(2, '0')
                                    val minStr = minute.toString().padStart(2, '0')
                                    onAtualizarEtapa(index, etapa.copy(hora = horaStr, minuto = minStr))
                                },
                                etapa.hora.toIntOrNull() ?: 12,
                                etapa.minuto.toIntOrNull() ?: 0,
                                true
                            )
                            timePickerDialog.show()
                        }
                    },
                    onGpsClick = {
                        if (modoEdicao) {
                            onLaunchMaps(etapa.localId)
                        } else {
                            val uri = Uri.parse("google.navigation:q=${etapa.latitude},${etapa.longitude}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try { context.startActivity(mapIntent) } catch (e: Exception) { }
                        }
                    },
                    onRemoveClick = { if (modoEdicao) onRemoverEtapa(etapa) }
                )
            }
            if (modoEdicao) {
                item {
                    Button(onClick = onAdicionarEtapa, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Adicionar Etapa")
                    }
                }
            }
        }
    }
}


@Composable
fun EtapaItemCard(
    etapa: Etapa,
    modoEdicao: Boolean,
    index: Int,
    totalItens: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDescChange: (String) -> Unit,
    onTimeClick: () -> Unit,
    onGpsClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                //HORA
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = modoEdicao) { onTimeClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    val horaTexto = if (etapa.hora.isBlank() || etapa.minuto.isBlank()) "Definir Hora" else "${etapa.hora}:${etapa.minuto}"
                    Text(text = horaTexto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                // --- SETINHAS
                if (modoEdicao) {
                    Row {
                        IconButton(onClick = onMoveUp, enabled = index > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir")
                        }
                        IconButton(onClick = onMoveDown, enabled = index < totalItens - 1) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer")
                        }
                    }
                }

                if (modoEdicao) {
                    IconButton(onClick = onRemoveClick) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = etapa.descricao,
                onValueChange = onDescChange,
                label = { Text("Descrição da etapa") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = !modoEdicao
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .clickable { onGpsClick() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Localização", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Localização", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = etapa.localNome ?: "Nenhum local selecionado",
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1
                    )
                }
                if (!modoEdicao && etapa.latitude != null) {
                    Icon(Icons.Default.Navigation, contentDescription = "Iniciar Rota", tint = MaterialTheme.colorScheme.primary)
                } else if (modoEdicao) {
                    Icon(Icons.Default.EditLocation, contentDescription = "Editar Local")
                }
            }
        }
    }
}