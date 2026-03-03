package com.example.rovi2.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "roteiros")
data class Roteiro(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nome: String,
    val descricao: String,
    val etapas: List<Etapa> = emptyList()
) : Serializable