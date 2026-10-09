package com.neytron.ethereal.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.neytron.ethereal.R

/**
 * ЗАКОНСЕРВИРОВАНО.
 *
 * InCallService для обработки звонков.
 * Сейчас НЕ активен в манифесте.
 *
 * Чтобы включить обратно:
 *  1. Раскомментировать в AndroidManifest.xml блок <service> и <receiver>.
 *  2. Раскомментировать разрешения MANAGE_OWN_CALLS, READ_PHONE_STATE и т.д.
 *  3. Вернуть запрос ROLE_DIALER в MainActivity.
 *  4. Вернуть VirtualAppsProvider.getVirtualApps() в AppRepository.
 */
class EtherealInCallService : InCallService() {

    companion object {
        const val CHANNEL_ID = "ethereal_calls_v2"
        const val NOTIFICATION_ID = 1001

        var currentCall: Call? = null
            private set

        private val _callState = mutableStateOf<Int?>(null)
        val callState: State<Int?> get() = _callState

        private val _audioState = mutableStateOf<CallAudioState?>(null)
        val audioState: State<CallAudioState?> get() = _audioState

        private var instance: EtherealInCallService? = null

        fun setMute(mute: Boolean) { instance?.setMuted(mute) }

        fun setSpeaker(on: Boolean) {
            setAudioRoute(
                if (on) CallAudioState.ROUTE_SPEAKER
                else CallAudioState.ROUTE_EARPIECE
            )
        }

        fun setAudioRoute(route: Int) { instance?.setAudioRoute(route) }
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopRinging()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        call.registerCallback(callCallback)

        val state = call.state
        _callState.value = state
        _audioState.value = callAudioState
        handleCallState(call, state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        if (currentCall == call) {
            currentCall = null
            _callState.value = null
            _audioState.value = null
        }
        call.unregisterCallback(callCallback)
        stopRinging()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        _audioState.value = audioState
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            _callState.value = state
            handleCallState(call, state)
        }
    }

    private fun handleCallState(call: Call, state: Int) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        when (state) {
            Call.STATE_RINGING -> {
                startRinging()
                showIncomingCallNotification(call)
            }
            Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_ACTIVE -> {
                stopRinging()
                val intent = Intent(this, CallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                nm.cancel(NOTIFICATION_ID)
            }
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                stopRinging()
                nm.cancel(NOTIFICATION_ID)
            }
        }
    }

    // ============================================================
    // Звук + вибрация
    // ============================================================

    private fun startRinging() {
        if (ringtone != null) return

        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(this, uri).apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 800, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRinging() {
        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone = null
        try { vibrator?.cancel() } catch (_: Exception) {}
        vibrator = null
    }

    // ============================================================
    // Определение имени звонящего
    // ============================================================

    private fun resolveCallerName(call: Call): String {
        val details = call.details

        val networkName = details.callerDisplayName?.toString()
        if (!networkName.isNullOrBlank()) return networkName

        val number = details.handle?.schemeSpecificPart
            ?: return "Неизвестный"

        return lookupContactName(number) ?: number
    }

    private fun lookupContactName(number: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        return try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
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

    // ============================================================
    // Нотификация
    // ============================================================

    private fun showIncomingCallNotification(call: Call) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Звонки",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Входящие звонки"
                setShowBadge(true)
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(channel)
        }

        val callerName = resolveCallerName(call)

        val fullScreenIntent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_ANSWER
        }
        val answerPendingIntent = PendingIntent.getBroadcast(
            this, 1, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_REJECT
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this, 2, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Входящий звонок")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "Ответить", answerPendingIntent)
            .addAction(0, "Сбросить", rejectPendingIntent)
            .setColorized(true)
            .setColor(android.graphics.Color.parseColor("#B8FF3D"))
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}