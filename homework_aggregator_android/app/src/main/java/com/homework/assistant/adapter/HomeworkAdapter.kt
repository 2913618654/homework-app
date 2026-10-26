package com.homework.assistant.adapter

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.homework.assistant.R
import com.homework.assistant.model.HomeworkItem

class HomeworkAdapter(
    private var list: List<HomeworkItem> = emptyList()
) : RecyclerView.Adapter<HomeworkAdapter.ViewHolder>() {

    fun updateData(newList: List<HomeworkItem>) {
        this.list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_homework, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = list.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCourseName: TextView = itemView.findViewById(R.id.tvCourseName)
        private val tvPlatformBadge: TextView = itemView.findViewById(R.id.tvPlatformBadge)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDeadline: TextView = itemView.findViewById(R.id.tvDeadline)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)

        fun bind(item: HomeworkItem) {
            tvCourseName.text = item.courseName
            tvTitle.text = item.title
            tvDeadline.text = if (item.deadline.isNotBlank()) "截止: ${item.deadline}" else "无明确截止时间"

            // 平台样式
            if (item.platform == "学习通") {
                tvPlatformBadge.text = "学习通"
                tvPlatformBadge.setBackgroundColor(Color.parseColor("#EFF6FF"))
                tvPlatformBadge.setTextColor(Color.parseColor("#1D4ED8"))
            } else {
                tvPlatformBadge.text = "云班课"
                tvPlatformBadge.setBackgroundColor(Color.parseColor("#ECFDF5"))
                tvPlatformBadge.setTextColor(Color.parseColor("#047857"))
            }

            // 状态样式
            if (item.isCompleted) {
                tvStatusBadge.text = "✓ ${item.status}"
                tvStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"))
                tvStatusBadge.setTextColor(Color.parseColor("#15803D"))
            } else if (item.isUrgent) {
                tvStatusBadge.text = "⚡ ${item.remainingText}"
                tvStatusBadge.setBackgroundColor(Color.parseColor("#FEE2E2"))
                tvStatusBadge.setTextColor(Color.parseColor("#DC2626"))
            } else if (item.isExpired) {
                tvStatusBadge.text = "已截止"
                tvStatusBadge.setBackgroundColor(Color.parseColor("#F1F5F9"))
                tvStatusBadge.setTextColor(Color.parseColor("#94A3B8"))
            } else {
                tvStatusBadge.text = item.remainingText.ifBlank { item.status }
                tvStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"))
                tvStatusBadge.setTextColor(Color.parseColor("#B45309"))
            }

            // 点击跳转
            itemView.setOnClickListener {
                if (item.url.isNotBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                        itemView.context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
