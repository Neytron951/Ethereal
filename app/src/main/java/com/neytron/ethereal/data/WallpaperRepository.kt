package com.neytron.ethereal.data

import android.content.Context
import android.net.Uri
import java.io.File

object WallpaperRepository {

    private const val CURRENT_FILE = "wallpaper_current.jpg"
    private const val USER_PREFIX = "wallpaper_user_"

    private const val PREF_INSTALLED = "builtin_wallpapers_installed"

    fun getCurrentFile(context: Context): File =
        File(context.filesDir, CURRENT_FILE)

    fun hasWallpaper(context: Context): Boolean =
        getCurrentFile(context).exists()


    private fun userDir(context: Context): File {
        val dir = File(context.filesDir, "wallpapers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveUserWallpaper(context: Context, uri: Uri): File? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "$USER_PREFIX${System.currentTimeMillis()}.jpg"
            val file = File(userDir(context), fileName)
            input.use { ins ->
                file.outputStream().use { outs ->
                    ins.copyTo(outs)
                }
            }
            // Сразу делаем их текущими
            applyAsCurrent(context, file)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun applyAsCurrent(context: Context, source: File) {
        try {
            source.copyTo(getCurrentFile(context), overwrite = true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getUserWallpapers(context: Context): List<File> {
        return userDir(context).listFiles()
            ?.filter { it.name.startsWith(USER_PREFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun deleteUserWallpaper(context: Context, file: File) {
        val isCurrent = getCurrentFile(context).let { current ->
            try {
                current.exists() && file.length() == current.length()
            } catch (e: Exception) {
                false
            }
        }
        file.delete()
        if (isCurrent) {
            // Если удалили текущие — переключаемся на последние сохранённые
            getUserWallpapers(context).firstOrNull()?.let {
                applyAsCurrent(context, it)
            } ?: clearWallpaper(context)
        }
    }

    fun clearWallpaper(context: Context) {
        getCurrentFile(context).delete()
    }

    fun installBuiltinWallpapers(context: Context, builtinResIds: List<Int>) {
        val prefs = context.getSharedPreferences("ethereal_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_INSTALLED, false)) return

        builtinResIds.forEachIndexed { index, resId ->
            try {
                val fileName = "${USER_PREFIX}builtin_${System.currentTimeMillis()}_$index.jpg"
                val file = File(userDir(context), fileName)
                context.resources.openRawResource(resId).use { ins ->
                    file.outputStream().use { outs ->
                        ins.copyTo(outs)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        prefs.edit().putBoolean(PREF_INSTALLED, true).apply()
    }

    fun applyToLockScreen(context: Context, file: File): Boolean {
        return try {
            val input = file.inputStream()
            val wm = android.app.WallpaperManager.getInstance(context)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                wm.setStream(input, null, true, android.app.WallpaperManager.FLAG_LOCK)
            } else {
                // До Android 7 — только общие обои
                wm.setStream(input)
            }
            input.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun applyEverywhere(context: Context, file: File): Boolean {
        applyAsCurrent(context, file)
        return applyToLockScreen(context, file)
    }


}