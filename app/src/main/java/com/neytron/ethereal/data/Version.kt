package com.neytron.ethereal.data

/**
 * Семантическая версия.
 * Формат: major.minor.patch, например 1.2.3
 *
 * Парсится из строки вида:
 *  - "1.2.3"
 *  - "v1.2.3"
 *  - "v1.2.3-beta" (суффикс игнорируется)
 *  - "v1.2" → трактуется как 1.2.0
 *  - "v1" → трактуется как 1.0.0
 *
 * Сравнение по правилам semver: сначала major, потом minor, потом patch.
 * "1.10.0" > "1.9.5" — корректно.
 */
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<Version> {

    override fun compareTo(other: Version): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        return patch - other.patch
    }

    override fun toString(): String = "$major.$minor.$patch"

    /** Отображаемая строка с префиксом v. */
    fun display(): String = "v$this"

    companion object {
        /**
         * Парсит строку в Version. Возвращает null, если распарсить не удалось.
         *
         * Принимает: "v1.2.3", "1.2.3", "V1.2.3", "1.2", "1", "v1.2.3-rc1", "1.2.3+build".
         */
        fun parse(raw: String?): Version? {
            if (raw.isNullOrBlank()) return null

            // Убираем префикс "v" / "V" и суффиксы "-beta" / "+build"
            var s = raw.trim()
            if (s.startsWith("v", ignoreCase = true)) s = s.substring(1)
            s = s.substringBefore('-').substringBefore('+')

            if (s.isBlank()) return null

            val parts = s.split('.')
            return try {
                val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
                val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
                if (major < 0 || minor < 0 || patch < 0) return null
                Version(major, minor, patch)
            } catch (e: Exception) {
                null
            }
        }
    }
}