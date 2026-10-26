package com.homework.assistant.service

import com.google.gson.JsonParser
import com.homework.assistant.model.CourseItem
import com.homework.assistant.model.HomeworkItem
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ChaoXingService(private val cookieStr: String) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            private val cookieStore = mutableListOf<Cookie>()

            init {
                if (cookieStr.isNotBlank()) {
                    val parts = cookieStr.split(";")
                    for (part in parts) {
                        val item = part.trim()
                        if (item.contains("=")) {
                            val kv = item.split("=", limit = 2)
                            val name = kv[0].trim()
                            val value = kv[1].trim()
                            cookieStore.add(
                                Cookie.Builder()
                                    .name(name)
                                    .value(value)
                                    .domain("chaoxing.com")
                                    .build()
                            )
                        }
                    }
                }
            }

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore.addAll(cookies)
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore
            }
        })
        .build()

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    fun getCourses(): List<CourseItem> {
        val list = mutableListOf<CourseItem>()
        val url = "https://mooc1-api.chaoxing.com/mycourse/backclazzdata"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return list
            val json = JsonParser.parseString(body).asJsonObject
            if (json.has("result") && json.get("result").asInt == 1) {
                val channelList = json.getAsJsonArray("channelList")
                for (elem in channelList) {
                    val obj = elem.asJsonObject
                    val content = obj.getAsJsonObject("content") ?: continue
                    val classId = if (content.has("id")) content.get("id").asString else obj.get("key").asString
                    val cpi = if (content.has("cpi")) content.get("cpi").asString else ""
                    val courseObj = content.getAsJsonObject("course") ?: continue
                    val dataArr = courseObj.getAsJsonArray("data") ?: continue
                    if (dataArr.size() > 0) {
                        val cData = dataArr[0].asJsonObject
                        val courseId = cData.get("id").asString
                        val courseName = cData.get("name").asString
                        val teacher = if (cData.has("teacherfactor")) cData.get("teacherfactor").asString else ""
                        list.add(CourseItem(courseId, classId, cpi, courseName, teacher))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getAllHomeworks(): List<HomeworkItem> {
        val result = mutableListOf<HomeworkItem>()
        val courses = getCourses()
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val uncompletedKeywords = listOf("未交", "未做", "待做", "未提交", "未完成", "未参与", "退回", "需重做", "打回")
        val completedKeywords = listOf(
            "已完成", "已提交", "已交", "已参与",
            "待批阅", "待批改", "待评", "评阅中", "批改中", "批阅中",
            "已批改", "已批阅", "互评", "待互评", "已互评", "分"
        )

        for (c in courses) {
            try {
                // 1. 请求课程中间页
                val middleUrl = "https://mooc1.chaoxing.com/visit/stucoursemiddle?courseid=${c.courseId}&clazzid=${c.classId}&cpi=${c.cpi}"
                val midReq = Request.Builder().url(middleUrl).header("User-Agent", userAgent).build()
                val midRes = client.newCall(midReq).execute()
                val midHtml = midRes.body?.string() ?: ""

                var studentCourseUrl = "https://mooc1.chaoxing.com/mooc-ans/mycourse/studentcourse?courseId=${c.courseId}&clazzid=${c.classId}&cpi=${c.cpi}&v=2&fromMiddle=1"
                val scMatcher = Pattern.compile("href=[\"'](/mooc-ans/mycourse/studentcourse[^\"']+)[\"']").matcher(midHtml)
                if (scMatcher.find()) {
                    val rawPath = scMatcher.group(1)!!.replace("&amp;", "&")
                    studentCourseUrl = "https://mooc1.chaoxing.com$rawPath"
                }

                // 2. 提取专用的 workEnc
                val scReq = Request.Builder().url(studentCourseUrl).header("User-Agent", userAgent).build()
                val scRes = client.newCall(scReq).execute()
                val scHtml = scRes.body?.string() ?: ""

                val encMatcher = Pattern.compile("id=[\"']workEnc[\"']\\s+value=[\"']([^\"']+)[\"']").matcher(scHtml)
                val encAltMatcher = Pattern.compile("value=[\"']([^\"']+)[\"']\\s+id=[\"']workEnc[\"']").matcher(scHtml)

                var workEnc: String? = null
                if (encMatcher.find()) {
                    workEnc = encMatcher.group(1)
                } else if (encAltMatcher.find()) {
                    workEnc = encAltMatcher.group(1)
                }

                if (workEnc.isNullOrBlank()) continue

                // 3. 携带 workEnc 获取正式作业列表
                val workUrl = "https://mooc1.chaoxing.com/mooc2/work/list?courseId=${c.courseId}&classId=${c.classId}&cpi=${c.cpi}&enc=$workEnc"
                val workReq = Request.Builder()
                    .url(workUrl)
                    .header("User-Agent", userAgent)
                    .header("Referer", studentCourseUrl)
                    .build()

                val workRes = client.newCall(workReq).execute()
                val workHtml = workRes.body?.string() ?: ""

                if (workHtml.contains("暂无作业") || workHtml.length < 500) continue

                val doc = Jsoup.parse(workHtml)
                val items = doc.select(".bottomList li, .task-list ul li, .ulDiv li, li[onclick*='goTask']")

                for (it in items) {
                    var title = it.select(".overHidden2, .wh-title, .title, h3, h4").text().trim()
                    var status = it.select(".status, .state").text().trim()

                    val ariaLabel = it.attr("aria-label").trim()
                    if (ariaLabel.isNotEmpty() && (title.isEmpty() || status.isEmpty())) {
                        val parts = ariaLabel.split(";")
                        if (parts.size >= 2) {
                            if (title.isEmpty()) title = parts[0].trim()
                            if (status.isEmpty()) status = parts[1].trim()
                        } else if (title.isEmpty()) {
                            title = ariaLabel
                        }
                    }

                    if (title.isEmpty()) continue
                    if (status.isEmpty()) {
                        status = if (it.html().contains("notOver")) "未交" else "已截止"
                    }

                    val timeText = it.select(".time, .end-time, .status-item").text().trim()
                    var deadlineStr = ""
                    var sortKey = Long.MAX_VALUE
                    var isExpired = false
                    var isUrgent = false
                    var remainingText = "无截止时间"

                    // 解析相对时间与标准日期
                    val remMatch = Pattern.compile("剩余\\s*(\\d+)\\s*小时\\s*(\\d+)?").matcher(timeText)
                    val remDaysMatch = Pattern.compile("剩余\\s*(\\d+)\\s*天\\s*(\\d+)?").matcher(timeText)
                    val dateMatch = Pattern.compile("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}(\\s+\\d{1,2}:\\d{1,2})?").matcher(timeText)

                    var dlMillis = 0L
                    if (remMatch.find()) {
                        val hrs = remMatch.group(1)!!.toLong()
                        val mins = if (remMatch.group(2) != null) remMatch.group(2)!!.toLong() else 0L
                        dlMillis = now + (hrs * 3600 + mins * 60) * 1000
                        deadlineStr = sdf.format(Date(dlMillis))
                    } else if (remDaysMatch.find()) {
                        val days = remDaysMatch.group(1)!!.toLong()
                        val hrs = if (remDaysMatch.group(2) != null) remDaysMatch.group(2)!!.toLong() else 0L
                        dlMillis = now + (days * 86400 + hrs * 3600) * 1000
                        deadlineStr = sdf.format(Date(dlMillis))
                    } else if (dateMatch.find()) {
                        deadlineStr = dateMatch.group(0)!!
                        try {
                            dlMillis = sdf.parse(deadlineStr)?.time ?: 0L
                        } catch (e: Exception) {
                            // ignore
                        }
                    } else if (timeText.isNotBlank()) {
                        deadlineStr = timeText
                    }

                    if (dlMillis > 0) {
                        val diff = dlMillis - now
                        sortKey = diff
                        if (diff < 0) {
                            isExpired = true
                            remainingText = "已截止"
                        } else {
                            val days = diff / (86400 * 1000)
                            val hours = (diff % (86400 * 1000)) / (3600 * 1000)
                            val mins = (diff % (3600 * 1000)) / (60 * 1000)
                            remainingText = if (days > 0) "剩 ${days} 天 ${hours} 小时"
                            else if (hours > 0) "剩 ${hours} 小时 ${mins} 分"
                            else "仅剩 ${mins} 分钟！"

                            if (diff <= 86400 * 1000) {
                                isUrgent = true
                            }
                        }
                    }

                    // 准确判断已完成状态（待批阅属于已完成）
                    val isCompleted = if (uncompletedKeywords.any { status.contains(it) }) {
                        false
                    } else if (completedKeywords.any { status.contains(it) }) {
                        true
                    } else {
                        false
                    }

                    val taskUrl = it.attr("data").ifBlank { workUrl }

                    result.add(
                        HomeworkItem(
                            id = "cx_${Math.abs((c.courseId + title).hashCode())}",
                            platform = "学习通",
                            courseName = c.courseName,
                            title = title,
                            status = status,
                            deadline = deadlineStr,
                            url = taskUrl,
                            isCompleted = isCompleted,
                            isExpired = isExpired,
                            isUrgent = isUrgent,
                            remainingText = remainingText,
                            sortKey = sortKey
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }
}
