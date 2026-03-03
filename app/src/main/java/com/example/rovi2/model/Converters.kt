package com.example.rovi2.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

//Conversores (pra usar lista)
class Converters {
    private val gson = Gson()

//converter json pra lista
    @TypeConverter
    fun fromEtapaListString(value: String?): List<Etapa> {
        if (value == null) {
            return emptyList()
        }
        val listType = object : TypeToken<List<Etapa>>() {}.type
        return gson.fromJson(value, listType)
    }

  //converte lista pra json
    @TypeConverter
    fun toEtapaListString(list: List<Etapa>): String {
        return gson.toJson(list)
    }
}