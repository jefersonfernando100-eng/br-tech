package com.brtech.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        StepService.startIfPermitted(this)
        setContent {
            val mode by Store.theme.collectAsState()
            BrTechTheme(mode) { BrTechApp(mode) }
        }
    }
}

@Composable
fun BrTechApp(mode: Int) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("📶" to "Conectividade", "🔋" to "Carregamento", "👟" to "Pedômetro")
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = Orange, fontWeight = FontWeight.Black)) { append("Br") }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Light)) { append(" Tech") }
                }, fontSize = 26.sp)
                TextButton(onClick = { Store.cycleTheme() }) {
                    Text(listOf("◐ Auto", "☀ Claro", "☾ Escuro")[mode], color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Text(icon, fontSize = 20.sp) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Orange.copy(alpha = 0.25f),
                            selectedTextColor = Orange, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) { 0 -> WifiScreen(); 1 -> ChargeScreen(); else -> PedometerScreen() }
        }
    }
}
