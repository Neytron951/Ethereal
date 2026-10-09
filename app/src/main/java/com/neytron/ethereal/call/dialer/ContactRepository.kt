package com.neytron.ethereal.call.dialer

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactRepository {

    /**
     * Ищет имя контакта по номеру.
     * Возвращает null, если контакт не найден или нет разрешения.
     */
    fun lookupName(context: Context, number: String): String? {
        if (number.isBlank()) return null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class ContactMatch(
        val name: String,
        val number: String,
        val photoUri: String?
    )

    /**
     * Поиск контактов по подстроке (имя ИЛИ номер).
     * Используется в клавиатуре при наборе.
     */
    fun search(context: Context, query: String, limit: Int = 10): List<ContactMatch> {
        if (query.isBlank()) return emptyList()

        val result = mutableListOf<ContactMatch>()

        val uri = Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            Uri.encode(query)
        )
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        try {
            context.contentResolver.query(
                uri, projection, null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                val seen = mutableSetOf<String>()

                while (cursor.moveToNext() && result.size < limit) {
                    val name = cursor.getString(nameIdx) ?: continue
                    val number = cursor.getString(numIdx) ?: continue
                    val photo = cursor.getString(photoIdx)

                    val key = "$name|$number"
                    if (seen.add(key)) {
                        result.add(ContactMatch(name, number, photo))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }
}