package com.example.rovi2.controller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rovi2.MyApplication
import com.example.rovi2.model.Etapa
import com.example.rovi2.model.Roteiro
import com.example.rovi2.model.RoteiroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddRoteiroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RoteiroRepository = (application as MyApplication).repository

    //estado interno para a lista de etapas
    private val _etapas = MutableStateFlow<List<Etapa>>(emptyList())
    //estado publico pra ui
    val etapas: StateFlow<List<Etapa>> = _etapas.asStateFlow()

    fun salvarRoteiro(nome: String, descricao: String) {
        if (nome.isBlank() || descricao.isBlank()) {
            return
        }
        viewModelScope.launch {
            val novoRoteiro = Roteiro(
                nome = nome,
                descricao = descricao,
                etapas = _etapas.value
            )
            repository.insert(novoRoteiro)
        }
    }

    fun adicionarEtapa() {
        _etapas.update { listaAtual ->
            listaAtual + Etapa()
        }
    }

    fun removerEtapa(etapa: Etapa) {
        _etapas.update { listaAtual ->
            listaAtual.filterNot { it.localId == etapa.localId }
        }
    }

    fun atualizarEtapa(index: Int, etapaAtualizada: Etapa) {
        _etapas.update { listaAtual ->
            listaAtual.toMutableList().also {
                it[index] = etapaAtualizada
            }
        }
    }

    fun atualizarLocalizacaoEtapa(localId: String, latitude: Double, longitude: Double,localNome: String?) {
        _etapas.update { listaAtual ->
            listaAtual.map {
                if (it.localId == localId) {
                    it.copy(latitude = latitude, longitude = longitude,localNome = localNome)
                } else {
                    it
                }
            }
        }
    }

    //FUNCAO PARA MOVER OS ITENS
    fun moverEtapa(fromIndex: Int, toIndex: Int) {
        _etapas.update { listaAtual ->
            //VERIFICAR SE OS INDICES SAO VALIDOS
            if (toIndex in listaAtual.indices) {
                val novaLista = listaAtual.toMutableList()
                java.util.Collections.swap(novaLista, fromIndex, toIndex)
                novaLista //RETORNA NOVA LISTA
            } else {
                listaAtual
            }
        }
    }

}