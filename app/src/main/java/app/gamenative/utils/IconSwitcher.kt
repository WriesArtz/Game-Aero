package app.gamenative.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconSwitcher {

    private val ALIASES = listOf(
        "app.gamenative.MainActivityAliasDefault",
        "app.gamenative.MainActivityAliasAlt",
        "app.gamenative.MainActivityAliasAero",
    )

    fun applyLauncherIcon(context: Context, useAltIcon: Boolean) =
        applyLauncherVariant(context, if (useAltIcon) 1 else 0)

    /** Turns on the launcher alias of [variant] (0 default, 1 alternate, 2 Frutiger Aero) and off the others. */
    fun applyLauncherVariant(context: Context, variant: Int) {
        val packageManager = context.packageManager
        val chosen = variant.coerceIn(0, ALIASES.lastIndex)
        // enable the new alias first so the app never has no launcher entry
        ALIASES.withIndex().sortedByDescending { it.index == chosen }.forEach { (index, name) ->
            packageManager.setComponentEnabledSetting(
                ComponentName(context, name),
                if (index == chosen) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
