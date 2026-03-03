package com.example.rovi2.controller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rovi2.MyApplication
import com.example.rovi2.model.Etapa
import com.example.rovi2.model.Roteiro
import com.example.rovi2.model.RoteiroRepository
import kotlinx. coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RoteiroDetailViewModel(application: Application, private val roteiroId: Int) : AndroidViewModel(application) {

    private val repository: RoteiroRepository = (application as MyApplication).repository

    //carrega roteiro do bd
    private val _roteiroOriginal = repository.getRoteiroById(roteiroId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // estado interno edicao
    private val _etapas = MutableStateFlow<List<Etapa>>(emptyList())
    val etapas: StateFlow<List<Etapa>> = _etapas.asStateFlow()

    //estado publico final do roteiro
    val roteiro: StateFlow<Roteiro?> = combine(_roteiroOriginal, _etapas) { roteiroDB, etapasEditadas ->
        roteiroDB?.let {
            // Se as etapas de edição ainda não foram inicializadas, usa as do banco
            if (_etapas.value.isEmpty() && roteiroDB.etapas.isNotEmpty()) {
                _etapas.value = roteiroDB.etapas
            }
            // Retorna o roteiro com as etapa
            roteiroDB.copy(etapas = _etapas.value)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    fun updateRoteiro(nome: String, descricao: String, etapas: List<Etapa>) {
        val roteiroAtual = _roteiroOriginal.value ?: return
        if (nome.isBlank() || descricao.isBlank()) return
        //ATUAlIZAR OS ESTaDO INTERNO
        _etapas.value = etapas
        viewModelScope.launch {
            val roteiroAtualizado = roteiroAtual.copy(
                nome = nome,
                descricao = descricao,
                etapas = etapas // Usa as etapas do estado de edição
            )
            repository.update(roteiroAtualizado)
        }
    }

    fun deleteRoteiro() {
        val roteiroAtual = _roteiroOriginal.value ?: return
        viewModelScope.launch {
            repository.delete(roteiroAtual)
        }
    }

    //FUNCAO PARA MOVER AS ETAPAS
    fun moverEtapa(fromIndex: Int, toIndex: Int) {
        _etapas.update { listaAtual ->
            if (toIndex in listaAtual.indices) {
                val novaLista = listaAtual.toMutableList()
                java.util.Collections.swap(novaLista, fromIndex, toIndex)
                novaLista
            } else {
                listaAtual
            }
        }
    }

    // funcoes iguais ao AddRoteiroViewModel caso seja necessário refazer a logica
    /*
    fun adicionarEtapa() {
        _etapas.update { it + Etapa() }
    }

    fun removerEtapa(etapa: Etapa) {
        _etapas.update { listaAtual ->
            listaAtual.filterNot { it.localId == etapa.localId }
        }
    }

    fun atualizarEtapa(index: Int, etapaAtualizada: Etapa) {
        _etapas.update { listaAtual ->
            listaAtual.toMutableList().also { it[index] = etapaAtualizada }
        }
    }

    fun atualizarLocalizacaoEtapa(localId: String, latitude: Double, longitude: Double, localNome: String?) {
        _etapas.update { listaAtual ->
            listaAtual.map {
                if (it.localId == localId) it.copy(latitude = latitude, longitude = longitude, localNome = localNome) else it
            }
        }
    }
    */
}