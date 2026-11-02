package com.neytron.ethereal.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Провайдер наклона устройства (Pitch/Roll) для динамического отклика
 * бликов и отражений физического стекла (Liquid Glass) на датчиках ориентации.
 */
object DeviceTiltProvider : SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null

    private val _tilt = MutableStateFlow(Pair(0f, 0f))
    val tilt: StateFlow<Pair<Float, Float>> = _tilt

    fun register(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        }
        rotationVectorSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun unregister() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationValues = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationValues)
            val pitch = orientationValues[1].coerceIn(-1f, 1f)
            val roll = orientationValues[2].coerceIn(-1f, 1f)
            _tilt.value = Pair(roll * 14f, pitch * 14f)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
