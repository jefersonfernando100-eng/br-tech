package com.brtech.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

fun fmtDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

@Composable
fun WifiScreen() {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var info by remember { mutableStateOf(readWifi(ctx)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hist = remember { mutableStateListOf<Float>() }

    LaunchedEffect(granted) {
        while (true) {
            info = readWifi(ctx)
            now = System.currentTimeMillis()
            if (info.connected) { hist.add(info.pct.toFloat()); if (hist.size > 40) hist.removeAt(0) }
            delay(2000)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!granted) {
            BrCard {
                Text("Para mostrar o nome da rede, o Android exige a permissão de localização.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) { Text("Permitir") }
            }
        }
        if (!info.connected) {
            BrCard {
                Text("Sem Wi-Fi conectado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Conecte-se a uma rede Wi-Fi para ver os dados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }
        val qc = qualityColor(info.quality)
        BrCard {
            Text(info.ssid, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Pill(info.quality, qc)
                Text("${info.pct}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = qc)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { info.pct / 100f }, Modifier.fillMaxWidth().height(8.dp), color = qc,
                trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
        BrCard {
            SectionTitle("DETALHES")
            Spacer(Modifier.height(6.dp))
            MetricGrid(listOf(
                "Intensidade do sinal" to "${info.rssi} dBm",
                "Frequência" to info.band,
                "Canal" to if (info.channel > 0) "${info.channel}" else "—",
                "Velocidade da conexão" to if (info.speedMbps > 0) "${info.speedMbps} Mbps" else "—",
                "Endereço IP" to info.ip,
                "Gateway" to info.gateway,
                "DNS" to info.dns,
                "Tempo conectado" to fmtDuration(now - info.since),
            ))
            Text("Tempo contado desde que o Br Tech detectou esta rede.", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BrCard {
            SectionTitle("QUALIDADE DO SINAL")
            Spacer(Modifier.height(8.dp))
            LineChart(hist.toList(), 0f, 100f)
            Text("Últimas medições (a cada 2 s)", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
