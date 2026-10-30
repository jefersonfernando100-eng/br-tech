package com.brtech.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ChargeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        val level = readBattery(context).level
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> Store.chargeStart(level)
            Intent.ACTION_POWER_DISCONNECTED -> Store.chargeEnd(level)
        }
    }
}
