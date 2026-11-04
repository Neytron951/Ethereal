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
    private var activeSensor: Sensor? = null
    private var hasSample = false
    private var filteredX = 0f
    private var filteredY = 0f

    private val _tilt = MutableStateFlow(Pair(0f, 0f))
    val tilt: StateFlow<Pair<Float, Float>> = _tilt

    fun register(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }
        val manager = sensorManager ?: return
        if (activeSensor != null) return

        val rotationSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val sensor = rotationSensor
            ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: return
        if (manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)) {
            activeSensor = sensor
        } else if (rotationSensor != null) {
            val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelerometer != null && manager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_GAME
                )
            ) {
                activeSensor = accelerometer
            }
        }
    }

    fun unregister() {
        sensorManager?.unregisterListener(this)
        activeSensor = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sensorEvent = event ?: return
        val (targetX, targetY) = when (sensorEvent.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values)
                val orientationValues = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationValues)
                orientationValues[2].coerceIn(-1f, 1f) to
                    orientationValues[1].coerceIn(-1f, 1f)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                (sensorEvent.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f) to
                    (-sensorEvent.values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
            }
            else -> return
        }

        if (!hasSample) {
            filteredX = targetX
            filteredY = targetY
            hasSample = true
        } else {
            val filterFactor = 0.15f
            filteredX += (targetX - filteredX) * filterFactor
            filteredY += (targetY - filteredY) * filterFactor
        }
        _tilt.value = Pair(filteredX * 14f, filteredY * 14f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
