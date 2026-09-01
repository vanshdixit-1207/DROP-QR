package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.TransferRepository
import com.example.data.UserPreferencesRepository

class DropQRApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var transferRepository: TransferRepository
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        transferRepository = TransferRepository(database.transferDao())
        preferencesRepository = UserPreferencesRepository(this)
    }
}
