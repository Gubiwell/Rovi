package com.example.rovi2.view

import android.annotation.SuppressLint
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rovi2.controller.AddRoteiroViewModel
import com.example.rovi2.model.Etapa
import com.example.rovi2.ui.theme.Rovi2Theme

class AddRoteiroActivity : ComponentActivity() {

    private val viewModel: AddRoteiroViewModel by viewModels()
    private val mapResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val latitude = data?.getDoubleExtra("SELECTED_LATITUDE", 0.0) ?: 0.0
                val longitude = data?.getDoubleExtra("SELECTED_LONGITUDE", 0.0) ?: 0.0
                val localNome = data?.getStringExtra("SELECTED_NAME")
                val etapaLocalId = data?.getStringExtra("ETAPA_LOCAL_ID")

                if (etapaLocalId != null) {
                    viewModel.atualizarLocalizacaoEtapa(etapaLocalId, latitude, longitude,localNome)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Rovi2Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background){
                    val etapas by viewModel.etapas.collectAsState()

                    AddRoteiroScreen(
                        viewModel = viewModel,
                        etapas = etapas,
                        onBackClicked = { finish() },
                        onLaunchMaps = { etapaLocalId ->
                            val intent = Intent(this, MapsActivity::class.java)
                            intent.putExtra("REQUEST_CODE", MapsActivity.REQUEST_SELECT_LOCATION)
                            intent.putExtra("ETAPA_LOCAL_ID", etapaLocalId)
                            mapResultLauncher.launch(intent)
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoteiroScreen(
    viewModel: AddRoteiroViewModel,
    etapas: List<Etapa>,
    onBackClicked: () -> Unit,
    onLaunchMaps: (String) -> Unit
) {
    val context = LocalContext.current as ComponentActivity
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Roteiro") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Roteiro") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Nome") }
                )
            }
            item {
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = "Descrição") }
                )
            }

            item {
                Text(
                    text = "Minha Rotina",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            itemsIndexed(etapas) { index, etapa ->
                EtapaItemCard(
                    etapa = etapa,
                    index = index,
                    totalItens = etapas.size,
                    onMoveUp = { viewModel.moverEtapa(index, index - 1) },
                    onMoveDown = { viewModel.moverEtapa(index, index + 1) },
                    onDescChange = { newDesc ->
                        viewModel.atualizarEtapa(index, etapa.copy(descricao = newDesc))
                    },
                    onTimeClick = {
                        val timePickerDialog = TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val horaStr = hourOfDay.toString().padStart(2, '0')
                                val minStr = minute.toString().padStart(2, '0')
                                viewModel.atualizarEtapa(index, etapa.copy(hora = horaStr, minuto = minStr))
                            },
                            etapa.hora.toIntOrNull() ?: 0,
                            etapa.minuto.toIntOrNull() ?: 0,
                            true
                        )
                        timePickerDialog.show()
                    },
                    onGpsClick = { onLaunchMaps(etapa.localId) },
                    onRemoveClick = { viewModel.removerEtapa(etapa) }
                )
            }

            item {
                Button(
                    onClick = { viewModel.adicionarEtapa() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Adicionar Etapa")
                }
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }

            item {
                Button(
                    onClick = {
                        if (nome.isNotBlank() && descricao.isNotBlank()) {
                            viewModel.salvarRoteiro(nome, descricao)
                            Toast.makeText(context, "Roteiro salvo!", Toast.LENGTH_SHORT).show()
                            context.finish()
                        } else {
                            Toast.makeText(context, "Preencha Nome e Descrição.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar Roteiro")
                }
            }
        }
    }
}
@Composable
fun EtapaItemCard(
    etapa: Etapa,
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
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onTimeClick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Schedule, contentDescription = "Horário")
                    Spacer(Modifier.width(8.dp))
                    val horaTexto = if (etapa.hora.isNotBlank() && etapa.minuto.isNotBlank()) {
                        "${etapa.hora}:${etapa.minuto}"
                    } else {
                        "Hora"
                    }
                    Text(horaTexto)
                }
                Row {
                    IconButton(onClick = onMoveUp, enabled = index > 0) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir")
                    }
                    IconButton(onClick = onMoveDown, enabled = index < totalItens - 1) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer")
                    }
                }
                IconButton(onClick = onGpsClick) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Local",
                        tint = if (etapa.latitude != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }

                IconButton(onClick = onRemoveClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = etapa.descricao,
                onValueChange = onDescChange,
                label = { Text("Descrição da atividade") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall
            )

            if (etapa.latitude != null && etapa.longitude != null) {
                Text(
                    text = etapa.localNome ?: "Local: Lat: ${"%.4f".format(etapa.latitude)}, Lon: ${"%.4f".format(etapa.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}