package com.gameocr.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import com.gameocr.app.capture.CaptureStartRequestActivity
import com.gameocr.app.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 手势控制 Service（P4）。
 *
 * 摇晃（加速度计 magnitude 超阈值，约 2.5g）/ 翻转（重力 Z 轴方向反转）触发一次
 * 全屏翻译请求（走 [CaptureStartRequestActivity]，与首页开始按钮同一条链路）。
 *
 * 说明（专家 C4）：不做「挥手」——加速度计测不到隔空动作，需距离传感器或相机。
 * 手势触发依赖服务运行中；后台启动 Activity 依赖 SYSTEM_ALERT_WINDOW 豁免。
 */
@AndroidEntryPoint
class GestureSensorService : Service(), SensorEventListener {

    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gravity: Sensor? = null

    private var shakeEnabled = false
    private var flipEnabled = false

    private var lastTriggerAt = 0L
    private var lastGravitySign = 0

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        // 实时跟随设置开关
        scope.launch {
            settingsRepository.settings.collect { s ->
                shakeEnabled = s.gestureShakeEnabled
                flipEnabled = s.gestureFlipEnabled
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        gravity?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (!shakeEnabled) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                if (magnitude > SHAKE_THRESHOLD_G) triggerTranslation()
            }
            Sensor.TYPE_GRAVITY -> {
                if (!flipEnabled) return
                val sign = if (event.values[2] >= 0f) 1 else -1
                if (lastGravitySign != 0 && sign != lastGravitySign) triggerTranslation()
                lastGravitySign = sign
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun triggerTranslation() {
        val now = System.currentTimeMillis()
        if (now - lastTriggerAt < TRIGGER_COOLDOWN_MS) return
        lastTriggerAt = now
        startActivity(CaptureStartRequestActivity.newIntent(this))
    }

    companion object {
        /** 摇晃阈值（含重力），约 2.5g。 */
        private const val SHAKE_THRESHOLD_G = 25f
        /** 触发防抖间隔。 */
        private const val TRIGGER_COOLDOWN_MS = 3000L

        fun startIntent(context: Context): Intent =
            Intent(context, GestureSensorService::class.java)

        fun stopIntent(context: Context): Intent =
            Intent(context, GestureSensorService::class.java)
    }
}
