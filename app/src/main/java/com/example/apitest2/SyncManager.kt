package com.example.apitest2

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.*
import android.util.Log
import kotlin.math.abs

class SyncManager(private val context: Context) {
    private val metronomeTimes = mutableListOf<Long>() // 메트로놈 이벤트 시간
    private val stepTimes = mutableListOf<Long>() // 걸음 이벤트 시간
    private val allowedInterval = 300L // 0.4초 이상 간격일 때 진동

    // 메트로놈 카운트 발생 시 호출
    fun updateMetronomeCount() {
        val currentTime = System.currentTimeMillis()
        metronomeTimes.add(currentTime)
        Log.d("SyncManager", "Metronome time updated: $currentTime")
        checkInterval()
    }

    // 만보기 카운트 발생 시 호출
    fun updateGyroSensorCount() {
        val currentTime = System.currentTimeMillis()
        stepTimes.add(currentTime)
        Log.d("SyncManager", "GyroSensor time updated: $currentTime")
        checkInterval()
    }


    // 간격 확인 및 진동
    private fun checkInterval() {

        if (metronomeTimes.isNotEmpty() && stepTimes.isNotEmpty()) {
            val latestStepTime = stepTimes.last()
            val previousMetronomeTime = metronomeTimes.lastOrNull { it <= latestStepTime }
            val nearestMetronomeTime =
                metronomeTimes.minByOrNull { abs(it - latestStepTime) } ?: return
            if (previousMetronomeTime != null) {
                val interval = abs(previousMetronomeTime - latestStepTime)
                Log.d("SyncManager", "Time difference: $interval ms")

                if (interval > allowedInterval) {
                    triggerVibration()
                }
            }
        }
    }

    // 진동 실행
    private fun triggerVibration() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {

            val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
            Log.d("SyncManager", "Vibration triggered!")
        }
    }
}
