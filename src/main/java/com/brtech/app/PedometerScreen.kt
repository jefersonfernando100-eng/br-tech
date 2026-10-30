package com.brtech.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.UUID

private const val GOAL = 10000

@Composable
fun PedometerScreen() {
    val ctx = LocalContext.current
    val users by Store.users.collectAsState()
    val selId by Store.selectedId.collectAsState()
    val steps by Store.steps.collectAsState()
    val week by Store.week.collectAsState()
    val user = users.firstOrNull { it.id == selId }

    var dialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<User?>(null) }
    var toDelete by remember { mutableStateOf<User?>(null) }

    val hasSensor = remember {
        (ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }
    val perms = remember {
        buildList {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        StepService.startIfPermitted(ctx)
    }
    LaunchedEffect(Unit) {
        val ok = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        if (ok) StepService.startIfPermitted(ctx) else launcher.launch(perms)
    }

    val km = user?.let { steps * (it.height * 0.415f / 100f) / 1000f }
    val kcal = if (km != null && user != null) km * user.weight * 0.57f else null

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!hasSensor) {
            BrCard { Text("Este aparelho não possui sensor de passos.", color = MaterialTheme.colorScheme.error) }
        }
        BrCard {
            Text(user?.let { "Hoje • ${it.name}" } ?: "Hoje", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("%,d".format(steps), fontSize = 52.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("passos  •  meta $GOAL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { (steps / GOAL.toFloat()).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(12.dp))
            MetricGrid(listOf(
                "Distância" to (km?.let { "%.2f km".format(it) } ?: "—"),
                "Calorias" to (kcal?.let { "%.0f kcal".format(it) } ?: "—"),
            ))
            if (user == null) Text("Cadastre um usuário para calcular distância e calorias.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BrCard {
            SectionTitle("ÚLTIMOS 7 DIAS")
            Spacer(Modifier.height(8.dp))
            BarChart(week.map { it.second }, week.map { it.first })
        }
        BrCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("USUÁRIOS")
                Button(onClick = { editing = null; dialog = true }) { Text("+ Adicionar") }
            }
            if (users.isEmpty()) {
                Text("Nenhum usuário cadastrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            users.forEach { u ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = u.id == selId, onClick = { Store.select(u.id) })
                    Column(Modifier.weight(1f)) {
                        Text(u.name, fontWeight = FontWeight.SemiBold)
                        Text("${u.age} anos • ${"%.1f".format(u.weight)} kg • ${u.height} cm",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { editing = u; dialog = true }) { Text("Editar") }
                    TextButton(onClick = { toDelete = u }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (dialog) UserDialog(editing, onDismiss = { dialog = false }) {
        if (editing == null) Store.addUser(it) else Store.updateUser(it)
        dialog = false
    }
    toDelete?.let { u ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Excluir usuário?") },
            text = { Text("${u.name} será removido.") },
            confirmButton = { TextButton(onClick = { Store.deleteUser(u.id); toDelete = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun UserDialog(initial: User?, onDismiss: () -> Unit, onSave: (User) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var age by remember { mutableStateOf(initial?.age?.toString() ?: "") }
    var weight by remember { mutableStateOf(initial?.weight?.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() } ?: "") }
    var height by remember { mutableStateOf(initial?.height?.toString() ?: "") }
    val a = age.toIntOrNull()
    val w = weight.replace(',', '.').toFloatOrNull()
    val h = height.toIntOrNull()
    val valid = name.isNotBlank() && a != null && a in 1..120 && w != null && w in 10f..400f && h != null && h in 50..250
    val num = KeyboardOptions(keyboardType = KeyboardType.Number)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Novo usuário" else "Editar usuário") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(age, { age = it.filter(Char::isDigit) }, label = { Text("Idade (anos)") }, singleLine = true, keyboardOptions = num)
                OutlinedTextField(weight, { weight = it }, label = { Text("Peso (kg)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(height, { height = it.filter(Char::isDigit) }, label = { Text("Altura (cm)") }, singleLine = true, keyboardOptions = num)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(User(initial?.id ?: UUID.randomUUID().toString(), name.trim(), a!!, w!!, h!!))
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
