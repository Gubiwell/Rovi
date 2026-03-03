package com.example.rovi2.view

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3. TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rovi2.controller.MainViewModel
import com.example.rovi2.model.Roteiro
import com.example.rovi2.ui.theme.Rovi2Theme
import com.google.gson.Gson
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verificarIntentDeImportacao(intent)
        setContent {
            Rovi2Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background){
                    val roteiros by viewModel.roteiros.collectAsStateWithLifecycle()
                    MainScreen(roteiros = roteiros)
                }
            }
        }
    }
    private fun verificarIntentDeImportacao(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri != null) {
                importarRoteiro(uri)
                setIntent(null)
            }
        }
    }


     //Lê a URI do arquivo, converte o JSON e salva no banco

    private fun importarRoteiro(uri: Uri) {
        try {
            //Abrir o arquivo usando ContentResolver
            val inputStream = contentResolver.openInputStream(uri)

            //Ler o texto do arquivo (JSON)
            val jsonString = inputStream?.bufferedReader().use { it?.readText() }

            if (jsonString.isNullOrBlank()) {
                Toast.makeText(this, "Erro: Arquivo vazio ou corrompido", Toast.LENGTH_LONG).show()
                return
            }

            //Converter o JSON de volta para um objeto Roteiro
            //Gson que o Converters.kt também usa
            val gson = Gson()
            val roteiroImportado = gson.fromJson(jsonString, Roteiro::class.java)

            //Salvar no ViewModel
            viewModel.importarRoteiro(roteiroImportado)

            Toast.makeText(this, "Roteiro '${roteiroImportado.nome}' importado!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Falha ao importar Roteiro: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(roteiros: List<Roteiro>) {
    val context = LocalContext.current as ComponentActivity

    val viewModel: MainViewModel = viewModel()
    val nomeUsuario by viewModel.nomeUsuario.collectAsStateWithLifecycle()

    var mostrarDialogoNome by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (nomeUsuario.isBlank()) {
            mostrarDialogoNome = true
        }
    }

    if (mostrarDialogoNome) {
        NomeUsuarioDialog(
            nomeAtual = nomeUsuario,
            onDismiss = { mostrarDialogoNome = false },
            onConfirm = { novoNome ->
                viewModel.atualizarNomeUsuario(novoNome)
                mostrarDialogoNome = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (nomeUsuario.isBlank()) "Meus Roteiros (Rovi)" else "Roteiros de: $nomeUsuario",
                        modifier = Modifier.clickable {
                            mostrarDialogoNome = true
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, AddRoteiroActivity::class.java)
                    context.startActivity(intent)
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar Roteiro")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (roteiros.isEmpty()) {
                item {
                    EmptyState()
                }
            }
            items(roteiros) { roteiro ->
                RoteiroItem(
                    roteiro = roteiro,
                    onClick = {
                        val intent = Intent(context, RoteiroDetailActivity::class.java)
                        intent.putExtra("ROTEIRO_ID", roteiro.id)
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun RoteiroItem(roteiro: Roteiro, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), //SOMBRA
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CardTravel,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = roteiro.nome,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = roteiro.descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver detalhes",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}


@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AddLocationAlt,
            contentDescription = "Sem roteiros",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Nenhum roteiro encontrado",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Clique no botão (+) para planejar sua primeira aventura!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NomeUsuarioDialog(
    nomeAtual: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var texto by remember { mutableStateOf(nomeAtual) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Como você gostaria de ser chamado?") },
        text = {
            Column {
                Text("Digite seu nome para personalizar o app:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Seu Nome") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(texto) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}