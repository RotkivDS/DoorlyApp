package de.viktor.doorlyapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CollectionStateReceiver : BroadcastReceiver {
    private var callback: () -> Unit = {}

    constructor(): super() {}

    constructor(callback: () -> Unit) : super() {
        this.callback = callback
    }

    override fun onReceive(context: Context, intent: Intent) {
        callback()
    }

    companion object {
        private const val TAG = "CollectionStateReceiver"
    }
}