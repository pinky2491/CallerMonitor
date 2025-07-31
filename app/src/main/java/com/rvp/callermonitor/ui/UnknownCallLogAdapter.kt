package com.rvp.callermonitor.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rvp.callermonitor.R
import com.rvp.callermonitor.model.UnknownCallLog
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying unknown call logs.
 */
class UnknownCallLogAdapter(
    private val onItemClick: (UnknownCallLog) -> Unit = {}
) : ListAdapter<UnknownCallLog, UnknownCallLogAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_unknown_call_log, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(itemView: View, val onItemClick: (UnknownCallLog) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val phoneText: TextView = itemView.findViewById(R.id.tvPhoneNumber)
        private val timeText: TextView = itemView.findViewById(R.id.tvTimestamp)
        private var currentLog: UnknownCallLog? = null
        
        init {
            itemView.setOnClickListener {
                currentLog?.let { 
                    onItemClick(it) 
                }
            }
        }
        
        fun bind(log: UnknownCallLog) {
            currentLog = log
            phoneText.text = log.phoneNumber
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            timeText.text = sdf.format(Date(log.timestamp))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<UnknownCallLog>() {
        override fun areItemsTheSame(oldItem: UnknownCallLog, newItem: UnknownCallLog): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: UnknownCallLog, newItem: UnknownCallLog): Boolean = oldItem == newItem
    }
} 