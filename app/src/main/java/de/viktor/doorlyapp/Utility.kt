package de.viktor.doorlyapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.text.TextUtils
import org.apache.commons.io.IOUtils
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import java.net.*
import java.nio.channels.DatagramChannel


class LogHandle(private val logFile: File) {
    private var isClosed: Boolean = false
    var onCloseCallback: () -> Unit = {}
    private val writer =
        OutputStreamWriter(BufferedOutputStream(FileOutputStream(logFile, true), 65536))

    fun append(values: List<String>, skipLabelAppend: Boolean = false) {
        if (isClosed)
            return

        val mutableValues = values.toMutableList()

        try {
            if (!skipLabelAppend)
                mutableValues.add(LogFileHandler.appendLabelAll)

            writer.append("${TextUtils.join(",", mutableValues)}\n")
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun getContent(): String = IOUtils.toString(logFile.inputStream())
    fun close() {
        if (isClosed)
            return

        isClosed = true
        onCloseCallback()
        writer.flush()
        writer.close()
    }
}

class LogFileHandler(private val targetDir: File) {
    init {
        assert(targetDir.isDirectory)
        LogFileHandler.instance = this
    }

    var currentTime: Long = 0
        private set

    private var currentFolder: File = targetDir
    private val openLogHandles: MutableMap<String, LogHandle> = mutableMapOf()
    fun startLogTransition(transitionName: String, extra: String = "", startTime: Long): File {
        if (currentFolder != targetDir)
            throw java.lang.Exception("Transition is already running")

        currentTime = startTime

        val c = Calendar.getInstance().time
        val df = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS", Locale.getDefault())
        val formattedDate = df.format(c)
        val folderName = "${formattedDate}_${android.os.Build.MODEL}_${transitionName}_$extra"
        val folder = targetDir.toPath().resolve(folderName).toFile()

        if (folder.exists())
            throw FileAlreadyExistsException(folder)

        folder.mkdir()
        currentFolder = folder
        return currentFolder
    }

    fun endLogTransition() {
        currentFolder = targetDir
        openLogHandles.values.forEach { it.close() }
        openLogHandles.clear()
    }

    fun openLog(logName: String, columns: List<String>): LogHandle {
        if (currentFolder == targetDir)
            throw java.lang.Exception("Missing Transition")

        val handle = openLogHandles[logName];

        if (handle != null)
            return handle

        val logFile = currentFolder.resolve(File("$logName.csv"))
        val logHandle = LogHandle(logFile)

        val mutableCols = columns.toMutableList()
        mutableCols.add("label")
        logHandle.append(mutableCols, true)
        openLogHandles[logName] = logHandle
        return logHandle
    }

    fun readAllLogs(): List<File> = targetDir.listFiles { file -> file.isDirectory }.toList()

    companion object {
        var instance: LogFileHandler? = null
            private set

        var appendLabelAll: String = ""
        val LOG = "LogFileHandler"
    }
}


object Utility {
    fun getPurposeText(sharedPreferences: SharedPreferences): String =
        sharedPreferences.getString("purpose_text", "")!!

    fun getUsePollingBoolean(sharedPreferences: SharedPreferences): Boolean =
        sharedPreferences.getBoolean("use_polling", false)

    fun getSensorListFileNames() = SENSOR_LIST_FILE_NAME
    fun getSensorListFileNamesReversed() = SENSOR_LIST_FILE_NAME
    fun getStaticSensorList() = SENSOR_LIST
    val SENSOR_LIST = hashMapOf(
        "TYPE_ACCELEROMETER" to 1,
        "TYPE_MAGNETIC_FIELD" to 2,
        "TYPE_ORIENTATION" to 3,
        "TYPE_GYROSCOPE" to 4,
        "TYPE_LIGHT" to 5,
        "TYPE_PRESSURE" to 6,
        "TYPE_TEMPERATURE" to 7,
        "TYPE_PROXIMITY" to 8,
        "TYPE_GRAVITY" to 9,
        "TYPE_LINEAR_ACCELERATION" to 10,
        "TYPE_ROTATION_VECTOR" to 11,
        //"TYPE_RELATIVE_HUMIDITY" to 12,
        //"TYPE_AMBIENT_TEMPERATURE" to 13,
        //"TYPE_MAGNETIC_FIELD_UNCALIBRATED" to 14,
        "TYPE_GAME_ROTATION_VECTOR" to 15,
        //"//TYPE_GYROSCOPE_UNCALIBRATED" to 16,
        //"TYPE_SIGNIFICANT_MOTION" to 17,
        //"TYPE_STEP_DETECTOR" to 18,
        "TYPE_STEP_COUNTER" to 19,
        "TYPE_GEOMAGNETIC_ROTATION_VECTOR" to 20
        //"TYPE_HEART_RATE" to 21,
        //"TYPE_TILT_DETECTOR" to 22,
        //"TYPE_WAKE_GESTURE" to 23,
        //"TYPE_GLANCE_GESTURE" to 24,
        //"TYPE_PICK_UP_GESTURE" to 25,
        //"TYPE_WRIST_TILT_GESTURE" to 26,
        //"TYPE_DEVICE_ORIENTATION" to 27,
        //"TYPE_STATIONARY_DETECT" to 29,
        //"TYPE_MOTION_DETECT" to 30,
        //"TYPE_HEART_BEAT" to 31,
        //"TYPE_DYNAMIC_SENSOR_META" to 32,
        //"TYPE_LOW_LATENCY_OFFBODY_DETECT" to 34,
        //"TYPE_ACCELEROMETER_UNCALIBRATED" to 35
    )

    val SENSOR_LIST_FILE_NAME = hashMapOf(
        1 to "Accelerometer",
        2 to "MagneticField",
        3 to "Compass",
        4 to "Gyroscope",
        5 to "Light",
        6 to "Pressure",
        7 to "Temperature",
        8 to "Proximity",
        9 to "Gravity",
        10 to "AccelerometerLinear",
        11 to "RotationVector",
        12 to "Humidity",
        13 to "AmbientTemperature",
        15 to "GameRotationVector",
        17 to "SignificantMotion",
        19 to "StepCounter",
        20 to "GeomagneticRotationVector",
        21 to "HeartRate",
        22 to "TiltDetector",
        23 to "WakeGesture",
        24 to "GlanceGesture",
        25 to "PickUpGesture",
        27 to "DeviceOrientation",
        29 to "StationaryDetect",
        30 to "MotionDetect",
        31 to "HeartBeat",
        34 to "LowLatencyOffbodyDetect",
        35 to "AccelerometerUncalibrated"
    )

    val SENSOR_LIST_BY_TYPE = hashMapOf(
        1 to "TYPE_ACCELEROMETER",
        2 to "TYPE_MAGNETIC_FIELD",
        3 to "TYPE_ORIENTATION",
        4 to "TYPE_GYROSCOPE",
        5 to "TYPE_LIGHT",
        6 to "TYPE_PRESSURE",
        7 to "TYPE_TEMPERATURE",
        8 to "TYPE_PROXIMITY",
        9 to "TYPE_GRAVITY",
        10 to "TYPE_LINEAR_ACCELERATION",
        11 to "TYPE_ROTATION_VECTOR",
        12 to "TYPE_RELATIVE_HUMIDITY",
        13 to "TYPE_AMBIENT_TEMPERATURE",
        14 to "TYPE_MAGNETIC_FIELD_UNCALIBRATED",
        15 to "TYPE_GAME_ROTATION_VECTOR",
        16 to "TYPE_GYROSCOPE_UNCALIBRATED",
        17 to "TYPE_SIGNIFICANT_MOTION",
        19 to "TYPE_STEP_COUNTER",
        20 to "TYPE_GEOMAGNETIC_ROTATION_VECTOR",
        21 to "TYPE_HEART_RATE",
        22 to "TYPE_TILT_DETECTOR",
        23 to "TYPE_WAKE_GESTURE",
        24 to "TYPE_GLANCE_GESTURE",
        25 to "TYPE_PICK_UP_GESTURE",
        27 to "TYPE_DEVICE_ORIENTATION",
        29 to "TYPE_STATIONARY_DETECT",
        30 to "TYPE_MOTION_DETECT",
        31 to "TYPE_HEART_BEAT",
        34 to "TYPE_LOW_LATENCY_OFFBODY_DETECT",
        35 to "TYPE_ACCELEROMETER_UNCALIBRATED"
    )

    val SENSOR_LIST_FILE_NAME_REVERSED = hashMapOf(
        "Accelerometer" to 1,
        "MagneticField" to 2,
        "Compass" to 3,
        "Gyroscope" to 4,
        "Light" to 5,
        "Pressure" to 6,
        "Temperature" to 7,
        "Proximity" to 8,
        "Gravity" to 9,
        "AccelerometerLinear" to 10,
        "RotationVector" to 11,
        "Humidity" to 12,
        "AmbientTemperature" to 13,
        "GameRotationVector" to 15,
        "SignificantMotion" to 17,
        "StepCounter" to 19,
        "GeomagneticRotationVector" to 20,
        "HeartRate" to 21,
        "TiltDetector" to 22,
        "WakeGesture" to 23,
        "GlanceGesture" to 24,
        "PickUpGesture" to 25,
        "DeviceOrientation" to 27,
        "StationaryDetect" to 29,
        "MotionDetect" to 30,
        "HeartBeat" to 31,
        "LowLatencyOffbodyDetect" to 34,
        "AccelerometerUncalibrated" to 35
    )

    val SENSOR_COLUMN_NAMES = hashMapOf(
        "TYPE_LINEAR_ACCELERATION" to listOf("X", "Y", "Z"),
        "TYPE_ACCELEROMETER" to listOf("X", "Y", "Z"),
        "TYPE_GRAVITY" to listOf("X", "Y", "Z"),
        "TYPE_GYROSCOPE" to listOf("X", "Y", "Z"),
        "TYPE_MAGNETIC_FIELD" to listOf("X", "Y", "Z"),
        "TYPE_PROXIMITY" to listOf("Centimeter"),
        "TYPE_PRESSURE" to listOf("Millibars"),
        "TYPE_TEMPERATURE" to listOf("Degrees"),
        "TYPE_ROTATION_VECTOR" to listOf("X", "Y", "Z", "cos"),
        "TYPE_GAME_ROTATION_VECTOR" to listOf("X", "Y", "Z", "cos")
    )

    val WIFI_LOG_COLUMNS = listOf("Timestamp", "Milliseconds", "SSID", "Level")

    val SENSOR_COLUMN_NAMES_BY_TYPE = hashMapOf(
        10 to listOf("Timestamp", "Milliseconds", "X", "Y", "Z"),
        1 to listOf("Timestamp", "Milliseconds", "X", "Y", "Z"),
        9 to listOf("Timestamp", "Milliseconds", "X", "Y", "Z"),
        4 to listOf("Timestamp", "Milliseconds", "X", "Y", "Z"),
        2 to listOf("Timestamp", "Milliseconds", "X", "Y", "Z"),
        8 to listOf("Timestamp", "Milliseconds", "Centimeter"),
        6 to listOf("Timestamp", "Milliseconds", "Millibars"),
        7 to listOf("Timestamp", "Milliseconds", "Degrees"),
        11 to listOf("Timestamp", "Milliseconds", "X", "Y", "Z", "cos", "acc"),
        15 to listOf("Timestamp", "Milliseconds", "X", "Y", "Z", "cos"),
        3 to listOf("Timestamp", "Milliseconds", "X", "Y", "Z"),
        5 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        12 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        13 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        14 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        16 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        17 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        19 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        20 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        21 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        22 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        23 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        24 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        25 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        27 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        29 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        30 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        31 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        34 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
        35 to listOf("Timestamp", "Milliseconds", "1", "2", "3", "4", "5", "6", "7", "8", "9")
    )
}

typealias SensorCollectionEventListener = (SensorCollectionState) -> Unit
typealias ActivityCallback = () -> FragmentActivity?

data class SensorCollectionEventListenerMeta(
    val listener: SensorCollectionEventListener = {},
    val caller: () -> FragmentActivity? = { null }
)

class SensorCollectionState {
    var intent: Intent? = null
}


object SensorCollection {
    private const val TAG = "SensorCollection"

    val State: SensorCollectionState = SensorCollectionState()
    fun startLocalRecording(
        logName: String = "",
        sensorList: List<Sensor>,
        purpose: String = ""
    ): Intent {
        if (State.intent != null)
            throw Exception("Already running")

        val bundle = Bundle()
        bundle.putStringArrayList("service_sensor_list",
            sensorList.map { sensor -> sensor.name } as ArrayList<String>)

        return Intent(
            ApplicationProvider.getContext(),
            SensorCollectionService::class.java
        ).also { intent ->
            intent.putExtras(bundle)
            val startTime = System.currentTimeMillis()
            ApplicationProvider.logFileHandler.startLogTransition(logName, purpose, startTime)
            ApplicationProvider.Application.startForegroundService(intent)

            val localIntent = Intent(BroadcastIntentCodes.STATE_CHANGED)
            LocalBroadcastManager.getInstance(ApplicationProvider.getContext())
                .sendBroadcast(localIntent)

            State.intent = intent
        }
    }

    fun stopLocalRecording() {
        if (State.intent == null) {
            Log.e(TAG, "Can't stop twice...")
            return
        }

        ApplicationProvider.Application.stopService(State.intent)
        ApplicationProvider.logFileHandler.endLogTransition()

        val localIntent = Intent(BroadcastIntentCodes.STATE_CHANGED)
        LocalBroadcastManager.getInstance(ApplicationProvider.getContext())
            .sendBroadcast(localIntent)

        State.intent = null
    }

    fun startNetworkRecording(logName: String, sensorList: List<Sensor>, purpose: String): Intent {
        val intent = startLocalRecording(logName, sensorList, purpose)
        return intent
    }

    fun stopNetworkRecording() {
        stopLocalRecording()
    }

    fun getActualAllSensors(): List<Sensor> {
        val sensorManager =
            ApplicationProvider.getSensorManager()
        return sensorManager.getSensorList(Sensor.TYPE_ALL)
            .filter { sensor -> Utility.SENSOR_LIST.values.contains(sensor.type) }
    }

    fun getAllSensors(): List<Sensor> {
        val sensorManager = ApplicationProvider.getSensorManager()
        return Utility.getStaticSensorList().values.map { type ->
            sensorManager.getDefaultSensor(
                type
            )
        }
            .filterNotNull()
    }

    fun getAllSensors(sharedPreferences: SharedPreferences): List<Sensor> {
        val list = Utility.getSensorListFileNamesReversed()
        return getAllSensors()
            .filter { sensor ->
                val filename = list[sensor.type]
                sharedPreferences.getBoolean(filename, true)
            }
    }
}

typealias NetworkEventListener = () -> Unit

object BroadcastIntentCodes {
    const val STATE_CHANGED = "dde.viktor.doorlyapp.STATE_CHANGED"
}

enum class Opcodes(i: Int) {
    START_RECORDING(1), STOP_RECORDING(2), NOP(0)
}

open class BroadcastMessage(val opcode: Opcodes)
class StartBroadcastMessage(val purpose: String) : BroadcastMessage(Opcodes.START_RECORDING)
class StopBroadcastMessage : BroadcastMessage(Opcodes.STOP_RECORDING)

fun toast(message: String) {
    val mainHandler = Handler(ApplicationProvider.Application.mainLooper);
    val myRunnable = Runnable {
        Toast.makeText(ApplicationProvider.getContext(), message, Toast.LENGTH_SHORT).show()
    }

    mainHandler.post(myRunnable)
}

object UI {
    fun _openSharePrompt(context: Context, file: File) {
        val uri =
            FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/zip"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share"))
    }

    fun openSharePrompt(context: Context, file: File) {
        val zipFile = ZipHelper.zipFolder(file) ?: return
        _openSharePrompt(context, zipFile)
    }

    fun openSharePrompt(context: Context, files: List<File>) {
        if (files.isEmpty())
            return

        val zipFile = ZipHelper.zipFiles(files) ?: return
        _openSharePrompt(context, zipFile)
    }
}


object ZipHelper {
    private const val BUFFER = 2048 * 10

    fun zipFiles(files: List<File>): File? {
        val path = ApplicationProvider.getContext().filesDir.toPath().resolve("Zips").toFile()
            .also { it.mkdir() }
        val zipFile = File(path, "all.zip")

        if (zipFile.exists())
            zipFile.delete()

        return try {
            val mapped = files.mapNotNull { file: File -> zipFolder(file) }
            val out = ZipOutputStream(FileOutputStream(zipFile, false))
            zipSubFolder(out, mapped, path.path.length)
            out.close()
            zipFile
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }


    fun zipFolder(toZipFolder: File): File? {
        val zipFile = File(toZipFolder.parent, "${toZipFolder.name}.zip")

        return try {
            val out = ZipOutputStream(FileOutputStream(zipFile, false))
            zipSubFolder(out, toZipFolder, toZipFolder.path.length)
            out.close()
            zipFile
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    private fun zipSubFolder(out: ZipOutputStream, folder: File, basePathLength: Int) {

        val fileList =
            folder.listFiles().filter { file -> file.name.endsWith(".csv") || file.isDirectory }
        var origin: BufferedInputStream? = null
        for (file in fileList) {
            if (file.isDirectory)
                return zipSubFolder(out, file, basePathLength)

            val data = ByteArray(BUFFER)

            val unmodifiedFilePath = file.path
            val relativePath = unmodifiedFilePath.substring(basePathLength + 1)

            val fi = FileInputStream(unmodifiedFilePath)
            origin = BufferedInputStream(fi, BUFFER)

            val entry = ZipEntry(relativePath)
            entry.time = file.lastModified()
            out.putNextEntry(entry)

            var count: Int = origin.read(data, 0, BUFFER)
            while (count != -1) {
                out.write(data, 0, count)
                count = origin.read(data, 0, BUFFER)
            }

            origin.close()
            out.closeEntry()
        }
    }

    private fun zipSubFolder(out: ZipOutputStream, files: List<File>, basePathLength: Int) {
        var origin: BufferedInputStream? = null
        for (file in files) {
            if (file.isDirectory)
                return zipSubFolder(out, file, basePathLength)

            val data = ByteArray(BUFFER)

            val unmodifiedFilePath = file.path
            val relativePath = unmodifiedFilePath.substring(basePathLength + 1)

            val fi = FileInputStream(unmodifiedFilePath)
            origin = BufferedInputStream(fi, BUFFER)

            val entry = ZipEntry(relativePath)
            entry.time = file.lastModified() // to keep modification time after unzipping
            out.putNextEntry(entry)

            var count: Int = origin.read(data, 0, BUFFER)
            while (count != -1) {
                out.write(data, 0, count)
                count = origin.read(data, 0, BUFFER)
            }

            origin.close()
            out.closeEntry()
        }
    }
}
