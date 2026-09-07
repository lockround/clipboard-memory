package com.clipboardmemory

import android.app.Application
import com.clipboardmemory.data.ClipboardDatabase

class ClipboardApp : Application() {
    val database: ClipboardDatabase by lazy {
        ClipboardDatabase.getInstance(this)
    }
}