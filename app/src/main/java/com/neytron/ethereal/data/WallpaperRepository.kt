package com.neytron.ethereal.data

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.os.Build
import java.io.File
import java.security.MessageDigest

object WallpaperRepository {

    private const val CURRENT_FILE = "wallpaper_current.jpg"
    private const val USER_PREFIX = "wallpaper_user_"
    private const val BUILTIN_PREFIX = "wallpaper_builtin_"

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
        val files = userDir(context).listFiles()?.filter { it.isFile } ?: return emptyList()
        val customWallpapers = files
            .filter { it.name.startsWith(USER_PREFIX) }
            .sortedByDescending { it.lastModified() }
        val builtinWallpapers = files
            .filter { it.name.startsWith(BUILTIN_PREFIX) }
            .sortedBy { it.name }
        return customWallpapers + builtinWallpapers
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

    private fun sha256(bytes: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(bytes)
            hash.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    private fun sha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(file.readBytes())
            hash.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    fun installBuiltinWallpapers(context: Context, builtinResIds: List<Int>) {
        val dir = userDir(context)

        // 1. Формируем хеши и официальные имена всех встроенных ресурсов
        val builtinInfoList = builtinResIds.mapIndexed { index, resId ->
            val bytes = try {
                context.resources.openRawResource(resId).use { it.readBytes() }
            } catch (_: Exception) {
                ByteArray(0)
            }
            val hash = sha256(bytes)
            val resName = try {
                context.resources.getResourceEntryName(resId)
            } catch (_: Exception) {
                "builtin_${index + 1}"
            }
            val fileName = "${BUILTIN_PREFIX}${index + 1}_${resName}.jpg"
            Triple(resId, fileName, hash)
        }

        val builtinHashesToOfficialName = builtinInfoList
            .filter { it.third.isNotEmpty() }
            .associate { it.third to it.second }

        val officialFileNames = builtinInfoList.map { it.second }.toSet()

        // 2. Дедупликация: удаляем из папки ЛЮБЫЕ файлы, содержимое которых совпадает
        // с одним из встроенных ресурсов, но имя отличается от официального
        dir.listFiles()?.forEach { file ->
            if (file.isFile) {
                val fileHash = sha256(file)
                if (fileHash.isNotEmpty() && fileHash in builtinHashesToOfficialName) {
                    val correctName = builtinHashesToOfficialName[fileHash]
                    if (file.name != correctName) {
                        file.delete()
                    }
                } else if (file.name.startsWith(BUILTIN_PREFIX) && file.name !in officialFileNames) {
                    // Удаляем устаревшие файлы с префиксом wallpaper_builtin_, если ресурс удален
                    file.delete()
                }
            }
        }

        // 3. Создаем официальные файлы для каждого встроенного обоя, если их еще нет
        builtinInfoList.forEach { (resId, fileName, _) ->
            val file = File(dir, fileName)
            if (!file.exists()) {
                try {
                    context.resources.openRawResource(resId).use { ins ->
                        file.outputStream().use { outs ->
                            ins.copyTo(outs)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun applyToLockScreen(context: Context, file: File): Boolean {
        return try {
            val wm = WallpaperManager.getInstance(context)
            file.inputStream().use { input ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wm.setStream(input, null, true, WallpaperManager.FLAG_LOCK)
                } else {
                    wm.setStream(input)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun applyToHomeScreen(context: Context, file: File): Boolean {
        applyAsCurrent(context, file)
        return try {
            val wm = WallpaperManager.getInstance(context)
            file.inputStream().use { input ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wm.setStream(input, null, true, WallpaperManager.FLAG_SYSTEM)
                } else {
                    wm.setStream(input)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun applyEverywhere(context: Context, file: File): Boolean {
        applyAsCurrent(context, file)
        return try {
            val wm = WallpaperManager.getInstance(context)
            file.inputStream().use { input ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wm.setStream(
                        input,
                        null,
                        true,
                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    )
                } else {
                    wm.setStream(input)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}
