package de.viktor.doorlyapp

import android.Manifest
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentFactory
import androidx.preference.PreferenceManager

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.viktor.doorlyapp.R
import de.viktor.doorlyapp.SensorCollection.getAllSensors
import de.viktor.doorlyapp.Utility.getPurposeText
import de.viktor.doorlyapp.ui.main.SectionsPagerAdapter


class ApplicationProvider : Application(), ViewModelStoreOwner {
    override fun onCreate() {
        super.onCreate()
        Application = this
        logFileHandler =
                LogFileHandler(filesDir.toPath().resolve("Logs").toFile().also { it.mkdir() })
    }

    override fun getViewModelStore() = ViewModelStore()

    companion object {
        val LOG = "ApplicationProvider"

        lateinit var logFileHandler: LogFileHandler
            private set
        lateinit var Application: ApplicationProvider
            private set

        fun getSharedPreference() = PreferenceManager.getDefaultSharedPreferences(Application.baseContext)!!
        fun getContext() = this.Application.applicationContext!!
        fun getSensorManager() =
                Application.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        fun getPowerManager() =
                Application.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    }
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

/*        val myIntent = Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(myIntent);*/

        ActivityCompat.requestPermissions(this,
                arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1);

        setContentView(R.layout.activity_main)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
        val stop: FloatingActionButton = findViewById(R.id.stop)
        val start: FloatingActionButton = findViewById(R.id.start)

        val sensorList =
                SensorCollection.getAllSensors(ApplicationProvider.getSharedPreference())

        start.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

            try {
                SensorCollection.startLocalRecording(
                        sensorList = getAllSensors(ApplicationProvider.getSharedPreference()),
                        purpose = getPurposeText(ApplicationProvider.getSharedPreference()))
            } catch (e: Exception) {
                Log.e("Main", e.toString())
            }

        }


        stop.setOnClickListener {
            SensorCollection.stopLocalRecording()
        }

        LocalBroadcastManager.getInstance(ApplicationProvider.getContext())
                .registerReceiver(
                        CollectionStateReceiver { updateView(SensorCollection.State) },
                        IntentFilter(BroadcastIntentCodes.STATE_CHANGED)
                )

        updateView(SensorCollection.State)
    }

    private fun updateView(state: SensorCollectionState) {
        val stop: FloatingActionButton = findViewById(R.id.stop)
        val start: FloatingActionButton = findViewById(R.id.start)

        when (state.intent != null) {
            true -> {
                start.visibility = View.GONE
                stop.visibility = View.VISIBLE
            }
            else -> {
                start.visibility = View.VISIBLE
                stop.visibility = View.GONE
            }
        }
    }


    fun onSettingsClick(view: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
}