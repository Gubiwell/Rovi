package com.example.rovi2.model

import kotlinx.coroutines.flow.Flow

class RoteiroRepository(private val roteiroDao: RoteiroDao) {
    val allRoteiros: Flow<List<Roteiro>> = roteiroDao.getAllRoteiros()

    fun getRoteiroById(id: Int): Flow<Roteiro> {
        return roteiroDao.getRoteiroById(id)
    }

    suspend fun insert(roteiro: Roteiro) {
        roteiroDao.insertRoteiro(roteiro)
    }

    //update novo (no dao tbm)
    suspend fun update(roteiro: Roteiro) {
        roteiroDao.updateRoteiro(roteiro)
    }

    //delete novo tbm (no dao)
    suspend fun delete(roteiro: Roteiro) {
        roteiroDao.deleteRoteiro(roteiro)
    }
}