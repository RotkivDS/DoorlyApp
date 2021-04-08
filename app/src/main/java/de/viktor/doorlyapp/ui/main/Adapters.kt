package de.viktor.doorlyapp.ui.main

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.viktor.doorlyapp.R
import java.io.File

class LogAdapter(context: Context, logs: List<File>) :
    ArrayAdapter<File>(context, R.layout.listitem_log, logs) {
    private val logList = arrayListOf<File>()

    init {
        logList.addAll(logs)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val currentLog = logList[position]

        val listItem = when (convertView == null) {
            true -> LayoutInflater
                .from(context).inflate(R.layout.listitem_log, parent, false)
            false -> convertView
        }

        val textView = listItem.findViewById<View>(R.id.log_title) as TextView
        textView.text = currentLog.name

        when (position % 2 == 1) {
            true -> listItem.setBackgroundColor(Color.parseColor("#66e6e9ee"))
            false -> listItem.setBackgroundColor(Color.parseColor("#66ffffff"))
        }


        return listItem
    }
}