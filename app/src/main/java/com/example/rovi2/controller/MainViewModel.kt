package com.example.rovi2.controller


import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rovi2.MyApplication // Import da raiz
import com.example.rovi2.model.Roteiro // Import do 'model'
import com.example.rovi2.model.RoteiroRepository // Import do 'model'
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RoteiroRepository = (application as MyApplication).repository

    val roteiros: StateFlow<List<Roteiro>> = repository.allRoteiros
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    //!!!!!!!!!!
    fun importarRoteiro(roteiro: Roteiro) {
        viewModelScope.launch {
            //O COPY ID TEM Q SER ZERO SE PRA FAZER ELE CRIAR um NOVO APARANETEMTNE
            repository.insert(roteiro.copy(id = 0))
        }
    }

    //USANDO O SHARED PREFERENCES (para salvar o nome apenas)
    //Não é necessário usar o ROOM apenas para isso

    //Acesso ao SharedPreferences (banco de dados simples para configurações)
    private val sharedPrefs = application.getSharedPreferences("rovi_prefs", Context.MODE_PRIVATE)
    //Estado do Nome do Usuário (começa lendo o que está salvo, ou vazio se for a 1 vez)
    private val _nomeUsuario = MutableStateFlow(sharedPrefs.getString("user_name", "") ?: "")
    val nomeUsuario: StateFlow<String> = _nomeUsuario.asStateFlow()
    //Função para salvar o nome novo
    fun atualizarNomeUsuario(novoNome: String) {
        _nomeUsuario.value = novoNome
        sharedPrefs.edit().putString("user_name", novoNome).apply()
    }
}
