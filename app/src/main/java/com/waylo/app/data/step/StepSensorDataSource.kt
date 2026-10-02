package com.waylo.app.data.step

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.waylo.app.BuildConfig

interface StepSensorDataSource {
    fun isAvailable(): Boolean
    fun start(onSensorValue: (Long) -> Unit): Boolean
    fun stop()
}

class AndroidStepSensorDataSource(context: Context) : StepSensorDataSource {

    private val sensorManager: SensorManager? =
        context.applicationContext.getSystemService(SensorManager::class.java)
    private val stepCounterSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var listener: SensorEventListener? = null

    override fun isAvailable(): Boolean = stepCounterSensor != null

    override fun start(onSensorValue: (Long) -> Unit): Boolean {
        val manager = sensorManager
        val sensor = stepCounterSensor
        if (manager == null || sensor == null) {
            debugLog("Step counter sensor not present")
            return false
        }
        if (listener != null) return true

        val newListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val value = event.values.firstOrNull() ?: return
                onSensorValue(value.toLong())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val registered = manager.registerListener(
            newListener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL,
        )
        if (registered) {
            listener = newListener
        } else {
            debugLog("Step counter registration failed")
        }
        return registered
    }

    override fun stop() {
        val current = listener ?: return
        sensorManager?.unregisterListener(current)
        listener = null
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "WayloSteps"
    }
}
