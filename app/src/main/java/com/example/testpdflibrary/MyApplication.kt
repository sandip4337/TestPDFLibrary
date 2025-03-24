package com.example.testpdflibrary

import android.app.Application
import com.example.powerpdflibrary.FileCleanupWorker

class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        // Schedule periodic file cleanup
        FileCleanupWorker.scheduleFileDeletion(this)
    }
}