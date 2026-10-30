package com.brtech.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class User(val id: String, val name: String, val age: Int, val weight: Float, val height: Int)
data class ChargeSession(val start: Long, val end: Long, val startLevel: Int, val endLevel: Int) {
    val complete get() = endLevel >= 95
}

object Store {
    private lateinit var sp: SharedPreferences
    private var hist = JSONObject()

    private val _theme = MutableStateFlow(0)
    val theme: StateFlow<Int> = _theme.asStateFlow()
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
    private val _selected = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selected.asStateFlow()
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps.asStateFlow()
    private val _week = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val week: StateFlow<List<Pair<String, Int>>> = _week.asStateFlow()
    private val _sessions = MutableStateFlow<List<ChargeSession>>(emptyList())
    val sessions: StateFlow<List<ChargeSession>> = _sessions.asStateFlow()

    @Synchronized
    fun init(c: Context) {
        if (::sp.isInitialized) return
        sp = c.applicationContext.getSharedPreferences("brtech", Context.MODE_PRIVATE)
        _theme.value = sp.getInt("theme", 0)
        try {
            val a = JSONArray(sp.getString("users", "[]"))
            _users.value = (0 until a.length()).map {
                val o = a.getJSONObject(it)
                User(o.getString("id"), o.getString("name"), o.getInt("age"), o.getDouble("w").toFloat(), o.getInt("h"))
            }
        } catch (_: Exception) {}
        val sel = sp.getString("sel", null)
        _selected.value = if (_users.value.any { it.id == sel }) sel else _users.value.firstOrNull()?.id
        try { hist = JSONObject(sp.getString("hist", "{}")) } catch (_: Exception) {}
        _steps.value = if (sp.getString("date", null) == LocalDate.now().toString()) sp.getInt("today", 0) else 0
        refreshWeek()
        try {
            val a = JSONArray(sp.getString("sessions", "[]"))
            _sessions.value = (0 until a.length()).map {
                val o = a.getJSONObject(it)
                ChargeSession(o.getLong("s"), o.getLong("e"), o.getInt("sl"), o.getInt("el"))
            }
        } catch (_: Exception) {}
    }

    // ---- Tema ----
    fun cycleTheme() {
        _theme.value = (_theme.value + 1) % 3
        sp.edit().putInt("theme", _theme.value).apply()
    }

    // ---- Usuários ----
    private fun saveUsers() {
        val a = JSONArray()
        _users.value.forEach {
            a.put(JSONObject().put("id", it.id).put("name", it.name).put("age", it.age)
                .put("w", it.weight.toDouble()).put("h", it.height))
        }
        sp.edit().putString("users", a.toString()).putString("sel", _selected.value).apply()
    }
    fun addUser(u: User) { _users.value = _users.value + u; _selected.value = u.id; saveUsers() }
    fun updateUser(u: User) { _users.value = _users.value.map { if (it.id == u.id) u else it }; saveUsers() }
    fun deleteUser(id: String) {
        _users.value = _users.value.filter { it.id != id }
        if (_selected.value == id) _selected.value = _users.value.firstOrNull()?.id
        saveUsers()
    }
    fun select(id: String) { _selected.value = id; saveUsers() }

    // ---- Passos ----
    @Synchronized
    fun onStepTotal(total: Float) {
        val today = LocalDate.now().toString()
        val date = sp.getString("date", null)
        var base = sp.getFloat("base", -1f)
        val last = sp.getFloat("last", -1f)
        var cur = sp.getInt("today", 0)
        if (date != today || base < 0f) { base = total; cur = 0 }
        else if (total < last) { base = total - cur } // aparelho reiniciou
        cur = (total - base).toInt().coerceAtLeast(0)
        hist.put(today, cur)
        val keys = hist.keys().asSequence().sorted().toList()
        if (keys.size > 30) keys.take(keys.size - 30).forEach { hist.remove(it) }
        sp.edit().putString("date", today).putFloat("base", base).putFloat("last", total)
            .putInt("today", cur).putString("hist", hist.toString()).apply()
        _steps.value = cur
        refreshWeek()
    }

    private fun refreshWeek() {
        val names = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
        _week.value = (6 downTo 0).map {
            val d = LocalDate.now().minusDays(it.toLong())
            names[d.dayOfWeek.value - 1] to hist.optInt(d.toString(), 0)
        }
    }

    // ---- Carregamento ----
    @Synchronized
    fun chargeStart(level: Int) {
        sp.edit().putLong("cs_t", System.currentTimeMillis()).putInt("cs_l", level).apply()
    }
    @Synchronized
    fun chargeEnd(level: Int) {
        val t = sp.getLong("cs_t", 0L)
        if (t == 0L) return
        val s = ChargeSession(t, System.currentTimeMillis(), sp.getInt("cs_l", 0), level)
        _sessions.value = (listOf(s) + _sessions.value).take(50)
        val a = JSONArray()
        _sessions.value.forEach { a.put(JSONObject().put("s", it.start).put("e", it.end).put("sl", it.startLevel).put("el", it.endLevel)) }
        sp.edit().putString("sessions", a.toString()).remove("cs_t").apply()
    }

    // ---- Wi-Fi: desde quando o app viu esta rede ----
    @Synchronized
    fun wifiSince(key: String): Long {
        if (sp.getString("wk", null) != key) {
            sp.edit().putString("wk", key).putLong("wt", System.currentTimeMillis()).apply()
        }
        return sp.getLong("wt", System.currentTimeMillis())
    }
}
