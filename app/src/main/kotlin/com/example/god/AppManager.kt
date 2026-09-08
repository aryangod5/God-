package com.example.god

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo

data class LaunchableApp(val label: String, val intent: Intent)

class AppManager(private val context: Context) {
    fun launchableApps(): List<LaunchableApp> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, 0)
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
            .map { info: ResolveInfo ->
                LaunchableApp(
                    info.loadLabel(pm).toString(),
                    Intent(query).setClassName(info.activityInfo.packageName, info.activityInfo.name)
                )
            }
    }
}
