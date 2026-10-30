package com.brtech.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChargeScreen() {
    val ctx = LocalContext.current
    var b by remember { mutableStateOf(readBattery(ctx)) }
    val samples = remember { mutableStateListOf<Float>() }
    val sessions by Store.sessions.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            b = readBattery(ctx)
            samples.add(b.powerW)
            if (samples.size > 45) samples.removeAt(0)
            delay(2000)
        }
    }

    val speed = when {
        !b.charging -> "—"
        b.powerW >= 15f -> "Carga rápida"
        b.powerW >= 5f -> "Carga normal"
        else -> "Carga lenta"
    }
    val hot = b.tempC >= 42f
    val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val full = sessions.filter { it.complete }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BrCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${b.level}%", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(b.status, style = MaterialTheme.typography.titleMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Pill(b.source, MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    if (b.charging) Pill(speed, Color(0xFF2ECC71))
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { b.level / 100f }, Modifier.fillMaxWidth().height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
        BrCard {
            SectionTitle("MEDIÇÕES")
            Spacer(Modifier.height(6.dp))
            MetricGrid(listOf(
                "Corrente recebida" to "${b.currentMa} mA",
                "Tensão" to "%.2f V".format(b.voltage),
                "Potência" to "%.1f W".format(b.powerW),
                "Temperatura" to ("%.1f °C".format(b.tempC) + if (hot) " ⚠" else ""),
                "Saúde da bateria" to b.health,
                "Status" to b.status,
            ))
        }
        BrCard {
            SectionTitle("POTÊNCIA DE CARREGAMENTO (W)")
            Spacer(Modifier.height(8.dp))
            LineChart(samples.toList(), 0f, maxOf(5f, samples.maxOrNull() ?: 5f))
            Text("Atualiza a cada 2 s. Conecte o carregador para ver a curva.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BrCard {
            SectionTitle("CARREGAMENTOS COMPLETOS")
            Spacer(Modifier.height(8.dp))
            if (full.isEmpty()) {
                Text("Nenhum ainda. Registramos quando você desconecta o carregador com ≥ 95% de bateria.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            } else full.forEach {
                val mins = (it.end - it.start) / 60000
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmt.format(Date(it.end)), fontWeight = FontWeight.SemiBold)
                    Text("${it.startLevel}% → ${it.endLevel}%  •  ${mins / 60}h${"%02d".format(mins % 60)}min",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
