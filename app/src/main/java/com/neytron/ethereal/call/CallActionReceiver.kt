package com.neytron.ethereal.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * ЗАКОНСЕРВИРОВАНО.
 *
 * Receiver для кнопок "Ответить" / "Сбросить" в нотификации.
 * Сейчас НЕ активен в манифесте.
 *
 * Чтобы включить обратно — раскомментировать <receiver> в AndroidManifest.xml.
 */
class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ANSWER = "com.example.ethereal.call.ANSWER"
        const val ACTION_REJECT = "com.example.ethereal.call.REJECT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val call = EtherealInCallService.currentCall ?: return

        when (intent.action) {
            ACTION_ANSWER -> {
                call.answer(0)
            }
            ACTION_REJECT -> {
                call.disconnect()
            }
        }
    }
}