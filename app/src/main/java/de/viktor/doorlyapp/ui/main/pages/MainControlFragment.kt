package de.viktor.doorlyapp.ui.main.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import de.viktor.doorlyapp.ApplicationProvider
import de.viktor.doorlyapp.LogFileHandler
import de.viktor.doorlyapp.R
import de.viktor.doorlyapp.ui.main.PageViewModel
import com.google.android.material.button.MaterialButton

/**
 * A placeholder fragment containing a simple view.
 */
class MainControlFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(ApplicationProvider.Application).get(PageViewModel::class.java)
            .apply {

            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_main, container, false)
        val textView: TextView = root.findViewById(R.id.section_label)
        val indoor: MaterialButton = root.findViewById(R.id.one)
        val outdoor: MaterialButton = root.findViewById(R.id.two)
        pageViewModel.isRunning.observe(viewLifecycleOwner, Observer<Boolean> {
            // TODO: 07.04.21 disable ui
        })

        indoor.setOnClickListener { LogFileHandler.appendLabelAll = "1" }
        outdoor.setOnClickListener { LogFileHandler.appendLabelAll = "2" }

        return root
    }

    companion object {
        @JvmStatic
        fun newInstance(): MainControlFragment {
            return MainControlFragment()
        }
    }


    /*    companion object {
        fun newInstance() = SensorControlFragment()
        const val LOG = "SensorControlFragment"
        const val REQUEST_ACCESS = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sensor_control, container, false)
        init(view)

        LocalBroadcastManager.getInstance(ApplicationProvider.getContext())
            .registerReceiver(
                CollectionStateReceiver { updateView(view, SensorCollection.State) },
                IntentFilter(BroadcastIntentCodes.STATE_CHANGED)
            )

        return view
    }

    private fun showAddItemDialog(callback: (String) -> Unit) {
        val taskEditText = EditText(this.activity!!)
        val dialog = AlertDialog.Builder(this.activity!!)
            .setTitle("Add a log name")
            .setMessage("What do you want to do next?")
            .setView(taskEditText)
            .setPositiveButton("Add") { _, _ -> callback(taskEditText.text.toString()) }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun init(view: View) {
        val stopRec = view.findViewById<Button>(R.id.button_stop_recording)
        val startButton = view.findViewById<Button>(R.id.button_start_recording)
        val lastValueTextView = view.findViewById<TextView>(R.id.lastValueTextView)

        var stopCallback: StopCallback = {}
        stopRec.setOnClickListener {
            stopCallback()
            stopNetworkRecording()
        }

        startButton.setOnClickListener {
            stopCallback = startInterval({
                activity?.runOnUiThread {
                    //lastValueTextView.text =
                    //    "${ESP32DataReceiver.lastBrakeState.first}, ${ESP32DataReceiver.lastBrakeState.second}"
                    //lastValueTextView.text = SensorCollectionService.

                }
            },1000)
            showAddItemDialog { logName ->
                try {
                    startNetworkRecording(
                        logName,
                        getAllSensors(ApplicationProvider.getSharedPreference()),
                        getPurposeText(ApplicationProvider.getSharedPreference())
                    )
                } catch (e: Exception) {
                    Log.e(LOG, e.toString())
                }

            }
        }
    }

    private fun updateView(view: View, state: SensorCollectionState) {
        val stopRec = view.findViewById<Button>(R.id.button_stop_recording)
        val startButton = view.findViewById<Button>(R.id.button_start_recording)

        when (state.intent != null) {
            true -> {
                startButton.visibility = View.GONE
                stopRec.visibility = View.VISIBLE
            }
            else -> {
                startButton.visibility = View.VISIBLE
                stopRec.visibility = View.GONE
            }
        }
    }*/



}