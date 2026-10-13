package com.neytron.ethereal.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(
    val packageName: String,
    val label: String
)

object IconPackRepository {

    private val themeActions = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "ginlemon.iconpack.THEME",
        "com.dlto.fusion.icon.THEME",
        "com.pie.iconpack.THEME",
        "com.iconpackstudio.THEME"
    )
    private val iconMaps = mutableMapOf<String, Map<String, String>>()

    fun getInstalledIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val packages = linkedMapOf<String, IconPackInfo>()
        themeActions.forEach { action ->
            pm.queryIntentActivities(Intent(action), 0)
                .forEach { result ->
                    val packageName = result.activityInfo?.packageName ?: return@forEach
                    if (packageName == context.packageName || packages.containsKey(packageName)) {
                        return@forEach
                    }
                    val label = runCatching {
                        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                    }.getOrNull() ?: packageName
                    packages[packageName] = IconPackInfo(packageName, label)
                }
        }
        return packages.values.sortedBy { it.label.lowercase() }
    }

    fun clearCache() {
        synchronized(iconMaps) { iconMaps.clear() }
    }

    fun loadIcon(
        context: Context,
        iconPackPackage: String,
        appPackage: String,
        activityName: String
    ): Drawable? {
        return try {
            val drawableName = synchronized(iconMaps) {
                iconMaps.getOrPut(iconPackPackage) { loadIconMap(context, iconPackPackage) }
            }["$appPackage/$activityName"] ?: return null
            val resources = context.packageManager.getResourcesForApplication(iconPackPackage)
            val resourceId = resources.getIdentifier(drawableName, "drawable", iconPackPackage)
                .takeIf { it != 0 }
                ?: resources.getIdentifier(drawableName, "mipmap", iconPackPackage)
            if (resourceId == 0) null else resources.getDrawable(resourceId, context.theme)
        } catch (e: PackageManager.NameNotFoundException) {
            synchronized(iconMaps) { iconMaps.remove(iconPackPackage) }
            Log.w("EtherealIconPack", "Selected icon pack is no longer installed", e)
            null
        } catch (e: Resources.NotFoundException) {
            Log.w("EtherealIconPack", "Icon pack references a missing drawable", e)
            null
        } catch (e: Exception) {
            Log.w("EtherealIconPack", "Could not load an icon from the selected pack", e)
            null
        }
    }

    private fun loadIconMap(context: Context, packageName: String): Map<String, String> {
        return try {
            val resources = context.packageManager.getResourcesForApplication(packageName)
            val assetParser = try {
                resources.assets.open("appfilter.xml")
            } catch (_: Exception) {
                null
            }
            if (assetParser != null) {
                assetParser.use { stream ->
                    val xml = Xml.newPullParser().apply { setInput(stream, "UTF-8") }
                    parseIconMap(xml)
                }
            } else {
                val resourceId = resources.getIdentifier("appfilter", "xml", packageName)
                if (resourceId == 0) emptyMap()
                else resources.getXml(resourceId).use(::parseIconMap)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w("EtherealIconPack", "Selected icon pack is no longer installed", e)
            return emptyMap()
        } catch (e: Exception) {
            Log.w("EtherealIconPack", "Could not parse the selected icon pack", e)
            emptyMap()
        }
    }

    private fun parseIconMap(parser: XmlPullParser): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && (parser.name == "item" || parser.name == "icon" || parser.name == "iconitem")) {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    parseComponent(component)?.let { key -> result[key] = drawable }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseComponent(value: String): String? {
        val component = value.removePrefix("ComponentInfo{").removeSuffix("}")
        val separator = component.indexOf('/')
        if (separator <= 0 || separator == component.lastIndex) return null

        val packageName = component.substring(0, separator)
        val className = component.substring(separator + 1).let {
            if (it.startsWith(".")) packageName + it else it
        }
        return "$packageName/$className"
    }
}
