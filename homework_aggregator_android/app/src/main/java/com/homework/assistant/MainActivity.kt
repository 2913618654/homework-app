package com.homework.assistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.homework.assistant.adapter.HomeworkAdapter
import com.homework.assistant.databinding.ActivityMainBinding
import com.homework.assistant.model.HomeworkItem
import com.homework.assistant.service.ChaoXingService
import com.homework.assistant.service.MosoteachService
import com.homework.assistant.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsHelper: PrefsHelper
    private val adapter = HomeworkAdapter()
    private var allHomeworks = listOf<HomeworkItem>()
    private var currentFilter = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsHelper = PrefsHelper(this)

        binding.rvHomeworks.layoutManager = LinearLayoutManager(this)
        binding.rvHomeworks.adapter = adapter

        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            loadHomeworks(forceRemote = true)
        }

        // 设置弹窗
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // 标签切换
        binding.chipGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = when (checkedId) {
                R.id.chipPending -> "pending"
                R.id.chipAll -> "all"
                R.id.chipChaoXing -> "chaoxing"
                R.id.chipMosoteach -> "mosoteach"
                R.id.chipCompleted -> "completed"
                else -> "pending"
            }
            applyFilter()
        }

        // 初始化加载本地缓存
        allHomeworks = prefsHelper.getCachedHomeworks()
        if (allHomeworks.isNotEmpty()) {
            applyFilter()
        }

        // 如果从未配置过，弹出设置；否则静默后台拉取最新数据
        if (prefsHelper.chaoXingCookie.isBlank() && prefsHelper.mosoteachCookie.isBlank()) {
            showSettingsDialog()
        } else {
            loadHomeworks(forceRemote = true)
        }
    }

    private fun loadHomeworks(forceRemote: Boolean) {
        if (!forceRemote) {
            allHomeworks = prefsHelper.getCachedHomeworks()
            applyFilter()
            return
        }

        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch(Dispatchers.IO) {
            val list = mutableListOf<HomeworkItem>()

            // 1. 抓取学习通
            val cxCookie = prefsHelper.chaoXingCookie
            if (cxCookie.isNotBlank()) {
                val cxService = ChaoXingService(cxCookie)
                val cxItems = cxService.getAllHomeworks()
                list.addAll(cxItems)
            }

            // 2. 抓取云班课
            val msCookie = prefsHelper.mosoteachCookie
            if (msCookie.isNotBlank()) {
                val msService = MosoteachService(msCookie)
                val msItems = msService.getAllHomeworks()
                list.addAll(msItems)
            }

            // 3. 排序：未完成的先排，已完成的后排
            list.sortWith(compareBy({ it.isCompleted }, { it.isExpired }, { it.sortKey }))

            withContext(Dispatchers.Main) {
                binding.swipeRefresh.isRefreshing = false
                if (list.isNotEmpty()) {
                    allHomeworks = list
                    prefsHelper.saveCachedHomeworks(list)
                    applyFilter()
                    Toast.makeText(this@MainActivity, "成功同步 ${list.size} 项作业", Toast.LENGTH_SHORT).show()
                } else if (allHomeworks.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun applyFilter() {
        val filtered = allHomeworks.filter { item ->
            when (currentFilter) {
                "pending" -> !item.isCompleted && !item.isExpired
                "all" -> true
                "chaoxing" -> item.platform == "学习通"
                "mosoteach" -> item.platform == "云班课"
                "completed" -> item.isCompleted
                else -> true
            }
        }

        adapter.updateData(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        // 更新统计卡片
        val pendingCount = allHomeworks.count { !it.isCompleted && !it.isExpired }
        val urgentCount = allHomeworks.count { it.isUrgent && !it.isCompleted }
        val completedCount = allHomeworks.count { it.isCompleted }

        binding.tvPendingCount.text = pendingCount.toString()
        binding.tvUrgentCount.text = urgentCount.toString()
        binding.tvCompletedCount.text = completedCount.toString()
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val etCx = dialogView.findViewById<EditText>(R.id.etChaoXingCookie)
        val etMs = dialogView.findViewById<EditText>(R.id.etMosoteachCookie)

        etCx.setText(prefsHelper.chaoXingCookie)
        etMs.setText(prefsHelper.mosoteachCookie)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("保存并同步") { _, _ ->
                val newCx = etCx.text.toString().trim()
                val newMs = etMs.text.toString().trim()
                prefsHelper.chaoXingCookie = newCx
                prefsHelper.mosoteachCookie = newMs
                Toast.makeText(this, "配置已保存，正在同步...", Toast.LENGTH_SHORT).show()
                loadHomeworks(forceRemote = true)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
