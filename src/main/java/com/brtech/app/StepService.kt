package com.brtech.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

class StepService : Service(), SensorEventListener {
    private var sm: SensorManager? = null

    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("steps", "Contagem de passos", NotificationManager.IMPORTANCE_LOW))
        try {
            val n = notif(Store.steps.value)
            if (Build.VERSION.SDK_INT >= 30) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            else startForeground(1, n)
        } catch (e: Exception) { stopSelf(); return }
        val m = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm = m
        m.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            m.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun notif(steps: Int): Notification =
        Notification.Builder(this, "steps")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Br Tech • Pedômetro")
            .setContentText("%,d passos hoje".format(steps))
            .setOngoing(true)
            .build()

    override fun onSensorChanged(e: SensorEvent) {
        Store.onStepTotal(e.values[0])
        getSystemService(NotificationManager::class.java).notify(1, notif(Store.steps.value))
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    override fun onDestroy() { sm?.unregisterListener(this); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        fun startIfPermitted(ctx: Context) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                try { ContextCompat.startForegroundService(ctx, Intent(ctx, StepService::class.java)) } catch (_: Exception) {}
            }
        }
    }
}
