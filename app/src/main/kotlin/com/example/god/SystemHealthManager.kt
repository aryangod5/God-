package com.example.god

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.StatFs
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.max

data class SystemHealth(
    val batteryPercent: Int,
    val batteryTempC: Float?,
    val freeRamMb: Long,
    val totalRamMb: Long,
    val storageFreeGb: Double,
    val cpuPercent: Float?,
    val networkOnline: Boolean,
    val microphonePermission: Boolean
)

class SystemHealthManager(private val context: Context) {
    private var previousTotal = 0L
    private var previousIdle = 0L

    fun read(): SystemHealth {
        val battery = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val percent = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val tempRaw = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temp = if (tempRaw != null && tempRaw != Int.MIN_VALUE) tempRaw / 10f else null

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)

        val stat = StatFs(context.filesDir.absolutePath)
        val freeGb = stat.availableBytes / 1024.0 / 1024.0 / 1024.0

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val mic = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return SystemHealth(
            percent,
            temp,
            info.availMem / 1024 / 1024,
            info.totalMem / 1024 / 1024,
            freeGb,
            cpuPercent(),
            online,
            mic
        )
    }

    private fun cpuPercent(): Float? {
        return try {
            RandomAccessFile(File("/proc/stat"), "r").use { raf ->
                val line = raf.readLine() ?: return null
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 5 || parts[0] != "cpu") return null
                val user = parts[1].toLong()
                val nice = parts[2].toLong()
                val system = parts[3].toLong()
                val idle = parts[4].toLong()
                val iowait = parts.getOrNull(5)?.toLong() ?: 0
                val total = user + nice + system + idle + iowait
                if (previousTotal == 0L) {
                    previousTotal = total; previousIdle = idle
                    return 0f
                }
                val totalDelta = max(1L, total - previousTotal)
                val idleDelta = idle - previousIdle
                previousTotal = total
                previousIdle = idle
                ((totalDelta - idleDelta).toFloat() / totalDelta * 100f).coerceIn(0f, 100f)
            }
        } catch (_: Exception) {
            null
        }
    }
}
