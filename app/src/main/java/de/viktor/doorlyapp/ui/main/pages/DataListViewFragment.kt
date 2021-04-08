package de.viktor.doorlyapp.ui.main.pages

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.viktor.doorlyapp.*
import de.viktor.doorlyapp.UI.openSharePrompt
import de.viktor.doorlyapp.ui.main.LogAdapter
import de.viktor.doorlyapp.ui.main.PageViewModel
import java.io.File

/**
 * A placeholder fragment containing a simple view.
 */
class DataListViewFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel =
            ViewModelProvider(ApplicationProvider.Application).get(PageViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        LocalBroadcastManager.getInstance(ApplicationProvider.getContext())
            .registerReceiver(
                CollectionStateReceiver {
                    val actualContext = context ?: return@CollectionStateReceiver
                    val actualView = view ?: return@CollectionStateReceiver
                    updateView(actualContext, actualView)
                },
                IntentFilter(BroadcastIntentCodes.STATE_CHANGED)
            )
        return inflater.inflate(R.layout.fragment_logs, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val actualContext = context ?: return
        updateView(actualContext, view)
    }

    private fun updateView(actualContext: Context, view: View) {
        val listView = view.findViewById(R.id.ui_log_list) as ListView
        val logs = ApplicationProvider.logFileHandler.readAllLogs()
        val reversed = logs.reversed().toMutableList()
        val adapter = LogAdapter(actualContext, reversed)

        listView.adapter = adapter
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                val log = reversed[position]
                openSharePrompt(view.context, log)
            }

        val uploadButton = view.findViewById(R.id.upload_all) as Button
        val delButton = view.findViewById(R.id.delete_logs) as Button
        uploadButton.setOnClickListener { openSharePrompt(uploadButton.context, logs) }
        delButton.setOnClickListener {
            reversed.forEach { file: File -> file.deleteRecursively() }
            reversed.clear()
            adapter.notifyDataSetChanged()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): DataListViewFragment {
            return DataListViewFragment()
        }
    }
}