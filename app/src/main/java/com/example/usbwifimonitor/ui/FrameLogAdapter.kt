package com.example.usbwifimonitor.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.usbwifimonitor.core.FrameLog
import com.example.usbwifimonitor.databinding.ItemFrameLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FrameLogAdapter : RecyclerView.Adapter<FrameLogAdapter.ViewHolder>() {

    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val items = mutableListOf<FrameLog>()

    fun submit(frameLog: FrameLog) {
        items.add(0, frameLog)
        if (items.size > 200) {
            items.removeLast()
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFrameLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val binding: ItemFrameLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FrameLog) {
            binding.timestamp.text = formatter.format(Date(item.timestampMillis))
            binding.summary.text = item.summary
            binding.rssi.text = item.rssi?.let { "$it dBm" } ?: "—"
            binding.channel.text = item.channel?.toString() ?: "?"
        }
    }
}
