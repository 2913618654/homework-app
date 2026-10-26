package com.homework.assistant.service

import com.homework.assistant.model.HomeworkItem
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MosoteachService(private val cookieStr: String) {

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
                            cookieStore.add(
                                Cookie.Builder()
                                    .name(kv[0].trim())
                                    .value(kv[1].trim())
                                    .domain("mosoteach.cn")
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

    fun getAllHomeworks(): List<HomeworkItem> {
        val result = mutableListOf<HomeworkItem>()
        if (cookieStr.isBlank()) return result

        try {
            val classUrl = "https://www.mosoteach.cn/web/index.php?c=clazz&m=my_joined"
            val classReq = Request.Builder()
                .url(classUrl)
                .header("User-Agent", userAgent)
                .build()

            val classRes = client.newCall(classReq).execute()
            val classHtml = classRes.body?.string() ?: return result
            val classDoc = Jsoup.parse(classHtml)

            val classCards = classDoc.select(".clazz-item, .my-joined-box, [data-clazz-id], .class-box")
            val classList = mutableListOf<Pair<String, String>>()

            for (card in classCards) {
                var cId = card.attr("data-clazz-id")
                if (cId.isBlank()) {
                    val link = card.select("a[href*='clazz_course_id']").attr("href")
                    val m = Pattern.compile("clazz_course_id=([a-zA-Z0-9]+)").matcher(link)
                    if (m.find()) {
                        cId = m.group(1) ?: ""
                    }
                }
                val cName = card.select(".clazz-name, .course-name, h3, h4, .class-name").text().trim().ifBlank { "未命名班课" }
                if (cId.isNotBlank()) {
                    classList.add(Pair(cId, cName))
                }
            }

            val uncompletedKeywords = listOf("未交", "未做", "待做", "未提交", "未完成", "未参与", "退回", "需重做", "打回")
            val completedKeywords = listOf(
                "已完成", "已提交", "已交", "已参与",
                "待批阅", "待批改", "待评", "评阅中", "批改中", "批阅中",
                "已批改", "已批阅", "互评", "待互评", "已互评", "分"
            )

            for ((cId, cName) in classList) {
                val actUrl = "https://www.mosoteach.cn/web/index.php?c=interaction&m=index&clazz_course_id=$cId"
                val actReq = Request.Builder().url(actUrl).header("User-Agent", userAgent).build()
                val actRes = client.newCall(actReq).execute()
                val actHtml = actRes.body?.string() ?: continue
                val actDoc = Jsoup.parse(actHtml)

                val items = actDoc.select(".interaction-row, .activity-item, .item-row, .interaction-item")
                for (it in items) {
                    val textContent = it.text()
                    if (!textContent.contains("作业") && !textContent.contains("测试") && !textContent.contains("任务")) {
                        continue
                    }

                    val title = it.select(".interaction-name, .activity-title, .item-title, h4, a").text().trim()
                    if (title.isBlank() || title.length > 80) continue

                    val status = it.select(".status, .state, .interaction-status").text().trim().ifBlank { "进行中" }
                    val timeRaw = it.select(".interaction-time, .end-time, .deadline, .time").text().trim()

                    val isCompleted = if (uncompletedKeywords.any { status.contains(it) }) {
                        false
                    } else if (completedKeywords.any { status.contains(it) }) {
                        true
                    } else {
                        false
                    }

                    result.add(
                        HomeworkItem(
                            id = "ybk_${Math.abs((cId + title).hashCode())}",
                            platform = "云班课",
                            courseName = cName,
                            title = title,
                            status = status,
                            deadline = timeRaw,
                            url = actUrl,
                            isCompleted = isCompleted,
                            remainingText = if (isCompleted) "已提交" else "待完成"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
