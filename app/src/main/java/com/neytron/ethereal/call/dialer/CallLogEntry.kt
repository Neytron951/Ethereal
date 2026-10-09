package com.neytron.ethereal.call.dialer

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED
}

data class CallLogEntry(
    val id: Long,
    val number: String,
    val contactName: String?,
    val type: CallType,
    val timestamp: Long,
    val cachedName: String?  // имя из CallLog.CachedName (если есть)
) {
    val displayName: String
        get() = contactName
            ?: cachedName?.takeIf { it.isNotBlank() }
            ?: number.ifBlank { "Неизвестный" }
}