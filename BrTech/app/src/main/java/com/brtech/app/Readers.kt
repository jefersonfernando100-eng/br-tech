package com.brtech.app

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import kotlin.math.abs

data class WifiState(
    val connected: Boolean = false,
    val ssid: String = "",
    val rssi: Int = -127,
    val pct: Int = 0,
    val band: String = "—",
    val channel: Int = 0,
    val ip: String = "—",
    val gateway: String = "—",
    val dns: String = "—",
    val speedMbps: Int = 0,
    val since: Long = 0L,
) {
    val quality: String get() = when {
        rssi >= -55 -> "Excelente"
        rssi >= -67 -> "Boa"
        rssi >= -80 -> "Regular"
        else -> "Ruim"
    }
}

private fun ipStr(i: Int) =
    if (i == 0) "—" else "${i and 255}.${(i shr 8) and 255}.${(i shr 16) and 255}.${(i shr 24) and 255}"

@Suppress("DEPRECATION")
fun readWifi(ctx: Context): WifiState {
    return try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return WifiState()
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wm.connectionInfo ?: return WifiState()
        if (info.rssi <= -127) return WifiState()
        val raw = (info.ssid ?: "").removeSurrounding("\"")
        val ssid = if (raw.isBlank() || raw == "<unknown ssid>") "Nome oculto (permita a localização)" else raw
        val f = info.frequency
        val band: String
        val ch: Int
        when {
            f in 2400..2500 -> { band = "2.4 GHz"; ch = if (f == 2484) 14 else (f - 2407) / 5 }
            f in 4900..5900 -> { band = "5 GHz"; ch = (f - 5000) / 5 }
            f >= 5925 -> { band = "6 GHz"; ch = (f - 5950) / 5 }
            else -> { band = "—"; ch = 0 }
        }
        val dhcp = wm.dhcpInfo
        WifiState(
            connected = true, ssid = ssid, rssi = info.rssi,
            pct = ((info.rssi + 100) * 2).coerceIn(0, 100),
            band = band, channel = ch,
            ip = ipStr(info.ipAddress), gateway = ipStr(dhcp?.gateway ?: 0), dns = ipStr(dhcp?.dns1 ?: 0),
            speedMbps = info.linkSpeed.coerceAtLeast(0),
            since = Store.wifiSince((info.bssid ?: "") + raw),
        )
    } catch (e: Exception) { WifiState() }
}

data class BatteryState(
    val level: Int = 0,
    val charging: Boolean = false,
    val status: String = "—",
    val health: String = "—",
    val tempC: Float = 0f,
    val voltage: Float = 0f,
    val currentMa: Int = 0,
    val powerW: Float = 0f,
    val source: String = "Bateria",
)

fun readBattery(ctx: Context): BatteryState {
    val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return BatteryState()
    val lv = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val sc = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val pct = if (lv >= 0 && sc > 0) lv * 100 / sc else 0
    val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
    val charging = st == BatteryManager.BATTERY_STATUS_CHARGING
    val mv = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
    val volt = if (mv > 100) mv / 1000f else mv.toFloat()
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    val ma = if (raw == Int.MIN_VALUE || raw == 0) 0 else abs(raw).let { if (it >= 20000) it / 1000 else it }
    val recv = if (charging) ma else 0
    return BatteryState(
        level = pct, charging = charging,
        status = when (st) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Carregando"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descarregando"
            BatteryManager.BATTERY_STATUS_FULL -> "Completa"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Conectado, sem carregar"
            else -> "Desconhecido"
        },
        health = when (i.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Boa"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Superaquecida"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Degradada"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobretensão"
            BatteryManager.BATTERY_HEALTH_COLD -> "Fria"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Falha"
            else -> "Desconhecida"
        },
        tempC = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
        voltage = volt, currentMa = recv, powerW = volt * recv / 1000f,
        source = when {
            plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> "Tomada (AC)"
            plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> "USB"
            plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> "Sem fio"
            plugged != 0 -> "Dock"
            else -> "Bateria"
        },
    )
}
