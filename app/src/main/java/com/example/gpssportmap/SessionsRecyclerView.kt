package com.example.gpssportmap

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SessionsRecyclerView(val context: Context, val repo: MapRepository): RecyclerView.Adapter<SessionsRecyclerView.ViewHolder>()  {

    lateinit var dataSet: Array<SessionDto?>
    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    fun refreshData() {
        dataSet = repo.getSessions()
    }

    init {
        refreshData()
    }

    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rowView = layoutInflater.inflate(R.layout.row_view, parent, false)
        return ViewHolder(rowView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sessionData = dataSet[position]

        val textViewSessionName = holder.itemView.findViewById<TextView>(R.id.textViewSessioName)
        textViewSessionName.text = sessionData!!.sessionName

        val textViewTimeSpent = holder.itemView.findViewById<TextView>(R.id.textViewSessionSaveDate)
        textViewTimeSpent.text = sessionData.dateSaved

        val imageButtonSessionView = holder.itemView.findViewById<ImageButton>(R.id.imageButtonSessionView)
        imageButtonSessionView.setOnClickListener {
            val intent = Intent(context, ViewPastSessionActivity::class.java)
            intent.putExtra(Constants.SESSION_ID, sessionData.sessionId)
            context.startActivity(intent)
        }

        val imageButtonDeleteSession = holder.itemView.findViewById<ImageButton>(R.id.imageButtonSessionDelete)
        imageButtonDeleteSession.setOnClickListener {
            repo.deleteSession(sessionData.sessionId)
            refreshData()
            this.notifyItemRemoved(holder.layoutPosition)
            this.notifyItemRangeChanged(0, dataSet.size)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}