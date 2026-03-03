package com.example.rovi2

import android.app.Application
import com.example.rovi2.model.AppDatabase
import com.example.rovi2.model.RoteiroRepository

class MyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { RoteiroRepository(database.roteiroDao()) }
}

