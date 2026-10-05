package com.example.amoledgallery.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.amoledgallery.ui.theme.DesignLanguage

/**
 * Keeps the home-screen icon in step with the chosen design.
 *
 * The manifest has one launcher entry (an activity-alias of MainActivity) per design, each with that
 * design's take on the Prism rosette; only the chosen design's entry is enabled. Switching enables the
 * new entry before disabling the old one, so the app never briefly has no launcher entry, and uses
 * DONT_KILL_APP so the running app is not closed. Some launchers drop a home-screen shortcut when its
 * entry is disabled; the icon in the app drawer always follows. Called when the app goes to the
 * background (MainActivity.onStop): disabling the entry a task was started from closes that task.
 */
object LauncherIcon {

    private fun aliasFor(language: DesignLanguage): String = when (language) {
        DesignLanguage.SAMSUNG_ONEUI -> "LauncherOneUi"
        DesignLanguage.GOOGLE_EXPRESSIVE -> "LauncherGemini"
        DesignLanguage.IOS_LIQUID_GLASS -> "LauncherIos"
        DesignLanguage.XIAOMI_HYPEROS -> "LauncherHyperOs"
    }

    /** The One UI entry is the one enabled in the manifest (One UI is the default design). */
    private val MANIFEST_DEFAULT = DesignLanguage.SAMSUNG_ONEUI

    fun apply(context: Context, language: DesignLanguage) {
        val pm = context.packageManager
        fun component(design: DesignLanguage) =
            ComponentName(context.packageName, "com.example.amoledgallery.${aliasFor(design)}")
        fun isEnabled(design: DesignLanguage): Boolean =
            when (runCatching { pm.getComponentEnabledSetting(component(design)) }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> design == MANIFEST_DEFAULT
                else -> false
            }
        fun set(design: DesignLanguage, enabled: Boolean) {
            if (isEnabled(design) == enabled) return
            runCatching {
                pm.setComponentEnabledSetting(
                    component(design),
                    if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
        set(language, true)
        DesignLanguage.entries.filter { it != language }.forEach { set(it, false) }
    }
}
