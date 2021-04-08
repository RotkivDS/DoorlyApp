package de.viktor.doorlyapp.ui.main.pages

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.preference.*
import de.viktor.doorlyapp.ApplicationProvider
import de.viktor.doorlyapp.R
import de.viktor.doorlyapp.SensorCollection.getAllSensors
import de.viktor.doorlyapp.Utility
import de.viktor.doorlyapp.ui.main.PageViewModel

/**
 * A placeholder fragment containing a simple view.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel =
            ViewModelProvider(ApplicationProvider.Application).get(PageViewModel::class.java)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
        //val listPreference = this.findPreference<ListPreference>("list_preference_plotting_sensor")
        //listPreference?.let { createListPreferenceEntries(it) }
    }

/*    private fun createListPreferenceEntries(listPreference: ListPreference) {
        if(getAllSensors().isEmpty())
            return

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val selected = prefs.getString("list_preference_plotting_sensor", "0")

        val sensors = getAllSensors()
        val entries = sensors.map { sensor -> sensor.name }
        val entryValues = sensors.map { sensor -> sensors.indexOf(sensor).toString() }
        listPreference.entries = entries.toTypedArray()
        listPreference.setDefaultValue("1")
        listPreference.entryValues = entryValues.toTypedArray()
        selected!!.toIntOrNull()?.let { listPreference.setValueIndex(it) }
    }*/

    companion object {
        @JvmStatic
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}

class MySensorSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_sensors, rootKey)
        //val container = activity?.findViewById(R.id.main) as ViewGroup
        //container.removeAllViews()
        createSensorList()
    }

    private fun createSensorList() {
        val preferenceCategory = PreferenceCategory(preferenceScreen.context)
        preferenceCategory.title = "Sensors"
        preferenceScreen.addPreference(preferenceCategory)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val list = Utility.getSensorListFileNamesReversed()

        getAllSensors().forEach { sensor ->
            val name = list[sensor.type]
            val preference = CheckBoxPreference(preferenceScreen.context)

            preference.title = name
            preference.key = name
            preference.isChecked = prefs.getBoolean(name, true)
            preferenceCategory.addPreference(preference)
        }
    }
}