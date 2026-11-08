package com.gallery.app.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Keeps the One UI launcher entry enabled.
 *
 * The manifest has a single launcher entry (an activity-alias of MainActivity) with the One UI
 * version of the rosette. Older builds could enable one alias per design; those are disabled here
 * so an update from such a build leaves exactly one launcher entry. Called when the app goes to the
 * background (MainActivity.onStop): disabling the entry a task was started from closes that task.
 */
object LauncherIcon {

    private const val ONE_UI_ALIAS = "LauncherOneUi"

    fun apply(context: Context) {
        val pm = context.packageManager
        fun component(name: String) = ComponentName(context.packageName, "${context.packageName}.$name")

        runCatching {
            pm.setComponentEnabledSetting(
                component(ONE_UI_ALIAS),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        // Aliases from the old multi-design builds no longer exist in the manifest; ignore failures.
        listOf("LauncherGemini", "LauncherIos", "LauncherHyperOs").forEach { name ->
            runCatching {
                pm.setComponentEnabledSetting(
                    component(name),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
