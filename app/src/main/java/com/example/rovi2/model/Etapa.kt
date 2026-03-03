package com.example.rovi2.model

import java.util.UUID

data class Etapa(
    val localId: String = UUID.randomUUID().toString(),
    val hora: String = "",
    val minuto: String = "",
    val descricao: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val localNome: String? = null
)