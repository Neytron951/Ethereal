package com.neytron.ethereal.call.dialer

import android.content.Context
import android.provider.CallLog
import java.util.Calendar

object CallLogRepository {

    fun getRecentCalls(context: Context, limit: Int = 500): List<CallLogEntry> {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.CACHED_NAME
        )

        val sortOrder = "${CallLog.Calls.DATE} DESC"

        val result = mutableListOf<CallLogEntry>()

        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)

                while (cursor.moveToNext() && result.size < limit) {
                    val rawType = cursor.getInt(typeIdx)
                    val type = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallType.MISSED
                        else -> continue
                    }

                    result.add(
                        CallLogEntry(
                            id = cursor.getLong(idIdx),
                            number = cursor.getString(numIdx) ?: "",
                            contactName = null,
                            type = type,
                            timestamp = cursor.getLong(dateIdx),
                            cachedName = cursor.getString(nameIdx)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    fun groupByDay(entries: List<CallLogEntry>): List<Pair<String, List<CallLogEntry>>> {
        if (entries.isEmpty()) return emptyList()

        val now = Calendar.getInstance()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterday = today - 24L * 60L * 60L * 1000L

        val grouped = entries.groupBy { entry ->
            when {
                entry.timestamp >= today -> "Сегодня"
                entry.timestamp >= yesterday -> "Вчера"
                else -> {
                    val cal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
                    val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                    val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                    val year = cal.get(Calendar.YEAR)
                    val currentYear = now.get(Calendar.YEAR)
                    if (year == currentYear) "$day.$month" else "$day.$month.$year"
                }
            }
        }

        return grouped.entries.map { it.key to it.value }
    }
}