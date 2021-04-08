package de.viktor.doorlyapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.util.Log
import android.os.*
import android.os.Process.*
import android.widget.Toast
import de.viktor.doorlyapp.SensorCollection.getAllSensors
import de.viktor.doorlyapp.Utility.WIFI_LOG_COLUMNS
import java.util.*
import kotlin.reflect.KFunction3

fun getSamplingRate(): Long {
    val value = ApplicationProvider.getSharedPreference().getString("sampling_rate_int", "5")
    val longValue = value?.toLongOrNull()

    return when (longValue == null) {
        true -> 5.toLong()
        else -> longValue
    }
}

fun getColumnNameByType(type: Int) = when (Utility.SENSOR_LIST_BY_TYPE.containsKey(type)) {
    true -> Utility.SENSOR_COLUMN_NAMES_BY_TYPE.getOrDefault(type, listOf())
    false -> generateColumns()
}

fun generateColumns(): List<String> {
    val list = mutableListOf("Timestamp", "Millisecond")
    list.addAll((1..10).toList().map { it.toString() })
    return list.toList()
}

typealias StopCallback = () -> Unit

fun startIntervalWakeLock(
    callback: () -> Unit,
    samplingRate: Long = 10,
    delay: Long = 0,
    onClose: () -> Unit = {}
): StopCallback {
    var started = true
    val handlerThread = HandlerThread("interval thread", THREAD_PRIORITY_URGENT_DISPLAY)
    handlerThread.start()
    val handler =
        Handler(handlerThread.looper)


    handler.post {
        val wakeLock = ApplicationProvider.getPowerManager().run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag0").apply {
                acquire(
                    Long.MAX_VALUE
                )
            }
        }

        Thread.sleep(delay)
        while (started) {
            callback()
            try {
                Thread.sleep(samplingRate)
            }catch (e:InterruptedException){
                Log.i("WifiCollection", "Collection Stopped...")
            }
        }

        wakeLock.release()
        onClose()
    }

    return {
        started = false
        handler.looper.thread.interrupt()
        handlerThread.quitSafely()
        handlerThread.join()
    }
}

fun startInterval(callback: () -> Unit, samplingRate: Long = 10, delay: Long = 0): StopCallback {
    var started = true
    val handlerThread = HandlerThread("interval thread", THREAD_PRIORITY_URGENT_DISPLAY)
    handlerThread.start()
    val handler =
        Handler(handlerThread.looper)

    handler.post {
        Thread.sleep(delay)
        while (started) {
            Thread.sleep(samplingRate)
            callback()
        }
    }

    return {
        started = false
        handlerThread.quitSafely()
        handlerThread.join()
    }
}

typealias CustomSensorEventCallback = (SensorEvent) -> Unit

class SensorCollectionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    private var stopIntervalCallback: StopCallback = {}
    private var stopWifiCallback: StopCallback = {}
    private val logFileHandles = HashMap<SensorListener, LogHandle>()


    object WifiCollector {
        var stopHandle: StopCallback = {}
        private fun createLogForSensor(): LogHandle? {
            val columns = WIFI_LOG_COLUMNS

            return ApplicationProvider.logFileHandler
                .openLog("Wifi", columns)
        }

        private fun writeLog(logHandle: LogHandle, startTime: Long, values: List<String>) {
            val time = System.currentTimeMillis()
            val diff = time - startTime
            val actualValues = mutableListOf(time.toString(), diff.toString())
            actualValues.addAll(values)
            logHandle.append(actualValues)
        }

        fun start(): StopCallback {
            val wifiManager =
                ApplicationProvider.getContext()
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
            val startTime = ApplicationProvider.logFileHandler.currentTime
            val log = createLogForSensor() ?: return {}

            stopHandle = startIntervalWakeLock({
                wifiManager.startScan()
                val wifiList = wifiManager.scanResults
                for (scanResult in wifiList) {
                    val values = listOf(scanResult.SSID, scanResult.level.toString())
                    writeLog(log, startTime, values)
                }
            }, samplingRate = 30000, onClose = { log.close() })

            return stopHandle
        }

        fun stop() {
            stopHandle()
            stopHandle = {}
        }
    }


    class SensorListener(sensor: Sensor, var callback: CustomSensorEventCallback = { _ -> }) :
        SensorEventListener {

        class WakeLockerHandlerThread(val threadName: String, val flag: Int) :
            HandlerThread(threadName, flag) {
            var wakeLock: PowerManager.WakeLock =
                ApplicationProvider.getPowerManager().run {
                    newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "MyApp::MyWakelockTag1"
                    ).apply { acquire(Long.MAX_VALUE) }
                }

            override fun quitSafely(): Boolean {
                wakeLock.release()
                return super.quitSafely()
            }

            override fun quit(): Boolean {
                wakeLock.release()
                return super.quit()
            }
        }

        companion object {
            const val LOG = "SensorListener"
        }

        var wakeLock: PowerManager.WakeLock =
            ApplicationProvider.getPowerManager().run {
                newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "MyApp::MyWakelockTag1"
                ).apply { acquire(Long.MAX_VALUE) }
            }

        private var mSensorThread: WakeLockerHandlerThread
        private var mSensorHandler: Handler

        init {
            val sensorManager = ApplicationProvider.getSensorManager()
            mSensorThread =
                WakeLockerHandlerThread(
                    "Sensor thread",
                    THREAD_PRIORITY_URGENT_DISPLAY
                )
            mSensorThread.start()
            mSensorHandler =
                Handler(mSensorThread.looper)
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_FASTEST,
                mSensorHandler
            )
        }

        var lastEvent: SensorEvent? = null

        override fun onSensorChanged(event: SensorEvent) {
            callback(event)
            lastEvent = event
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        fun setEventCustomHandler(callback: () -> Unit) {

        }

        fun stop() {
            val sensorManager = ApplicationProvider.getSensorManager()
            wakeLock.release()
            mSensorThread.quitSafely()
            mSensorThread.join()
            sensorManager.unregisterListener(this)
        }
    }

    private var wakeLock: PowerManager.WakeLock =
        ApplicationProvider.getPowerManager().run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag2").apply {
                acquire(
                    Long.MAX_VALUE
                )
            }
        }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null)
            return START_NOT_STICKY

        val bundle = intent.extras ?: return START_NOT_STICKY
        val sensorList = when (bundle.containsKey("service_sensor_list")) {
            false -> arrayListOf<String>()
            true -> bundle.getStringArrayList("service_sensor_list")
        }

        Log.d(LOG, sensorList?.size.toString())
        //getAllSensors(ApplicationProvider.getSharedPreference()).filter { sensor -> sensorList.contains(sensor.name) }
        //    .forEach { sensor -> registerSensor(sensor) }

        getAllSensors(ApplicationProvider.getSharedPreference()).forEach { sensor ->
            registerSensor(
                sensor
            )
        }
        startRecording()
        return START_STICKY
    }

    private fun writeEvent(event: SensorEvent, logHandle: LogHandle, startTime: Long) {
        val time = System.currentTimeMillis()
        val diff = time - startTime
        val actualValues = mutableListOf(time.toString(), diff.toString())
        actualValues.addAll(event.values.map { "$it" })
        logHandle.append(actualValues)
    }

    private fun startRecording() {
        val usePolling = Utility.getUsePollingBoolean(ApplicationProvider.getSharedPreference())
        val startTime = ApplicationProvider.logFileHandler.currentTime

        stopWifiCallback = WifiCollector.start()

        if (!usePolling) {
            logFileHandles.entries.forEach { (listener, log) ->
                listener.callback = { writeEvent(it, log, startTime) }
            }
            return
        }

        var lastTimeStamp = Long.MIN_VALUE
        stopIntervalCallback = setIntervalNew({

            if (lastTimeStamp == Long.MIN_VALUE)
                lastTimeStamp = System.currentTimeMillis()

            logFileHandles.entries
                .forEach { (listener, log) ->
                    val event = listener.lastEvent ?: return@forEach
                    writeEvent(event, log, lastTimeStamp)
                }

        }, getSamplingRate())
    }

    private fun setInterval(
        callback: () -> Unit,
        period: Long = 10,
        delay: Long = 1000
    ): TimerTask {
        val task = object : TimerTask() {
            override fun run() {
                callback()
            }
        }
        Timer().scheduleAtFixedRate(task, delay, period)
        return task
    }

    private fun setIntervalAlternative(
        callback: () -> Unit,
        period: Long = 10,
        delay: Long = 1000
    ): HandlerThread {
        val handlerThread = HandlerThread("Sensor thread", THREAD_PRIORITY_URGENT_DISPLAY)
        handlerThread.start()
        val handler =
            Handler(handlerThread.looper)

        val recFunc = object : Runnable {
            override fun run() {
                callback()
                handler.postDelayed(this, period)
            }
        }

        handler.postDelayed(recFunc, delay)
        return handlerThread
    }

    private fun setIntervalNew(
        callback: () -> Unit,
        period: Long = 7,
        delay: Long = 1000
    ): StopCallback =
        startIntervalWakeLock(callback, period, delay)

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            // Create the NotificationChannel
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val intent = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
            );

            val notification: Notification = Notification.Builder(this, channel.id)
                .setSmallIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground))
                .setLargeIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground))
                .setContentTitle("Sensor Collection Service")
                .setContentText("Collecting Sensor Data...")
                .setCategory(Notification.CATEGORY_EVENT)
                .setAutoCancel(true).setContentIntent(intent)
                .build()

            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }
    }

    private fun closeLogs() {
        logFileHandles.entries
            .forEach { (listener, log) ->
                listener.stop()
                log.close()
            }
        logFileHandles.clear()
    }

    private fun stop() {
        stopIntervalCallback()
        stopWifiCallback()
        wakeLock.release()
        closeLogs()
        //serviceLooper.quit()
        Toast.makeText(this, "Sensor collection stopped", Toast.LENGTH_SHORT).show()
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stop()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        stop()
    }

    private fun createLogForSensor(sensor: Sensor): LogHandle? {
        val columns = getColumnNameByType(sensor.type)
        val actualName = Utility.getSensorListFileNamesReversed()[sensor.type] ?: return null

        return ApplicationProvider.logFileHandler
            .openLog(actualName, columns)
    }

    private fun registerSensor(sensor: Sensor) {
        val log = createLogForSensor(sensor) ?: return
        val listener =
            SensorListener(
                sensor
            )
        logFileHandles[listener] = log
    }

    private fun registerSensor(sensor: Sensor, callback: CustomSensorEventCallback) {
        val log = createLogForSensor(sensor) ?: return
        val listener =
            SensorListener(
                sensor,
                callback
            )
        logFileHandles[listener] = log
    }

    companion object {
        const val CHANNEL_NAME = "SensorCollectionChannel"
        const val CHANNEL_ID = "1"
        const val ONGOING_NOTIFICATION_ID = 1
        const val LOG = "SensorCollectionService"
        const val WAKE_LOCK_TAG = "sensorcatcher"
    }

}

data class SensorData(
    val callback: KFunction3<SensorEvent, LogHandle, Long, Unit>,
    val values: SensorEvent,
    val logHandle: LogHandle,
    val startTime: Long
)
//
//class WriteLogTask {
//    class WriteLogTaskHandlerThread(threadName: String, flag: Int) : HandlerThread(threadName, flag) {
//        var wakeLock: PowerManager.WakeLock =
//            ApplicationProvider.getPowerManager().run {
//                newWakeLock(
//                    PowerManager.PARTIAL_WAKE_LOCK,
//                    "MyApp::MyWakelockTag1"
//                ).apply { acquire(Long.MAX_VALUE) }
//            }
//
//        override fun quitSafely(): Boolean {
//            wakeLock.release()
//            return super.quitSafely()
//        }
//
//        override fun quit(): Boolean {
//            wakeLock.release()
//            return super.quit()
//        }
//    }
//
//    init {
//        Handler
//    }
//}
//
//typealias CloseLogTask = WriteLogTas