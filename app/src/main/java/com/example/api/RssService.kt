package com.example.api

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class RssService {
    private val client = OkHttpClient()

    suspend fun fetchVnExpressRss(url: String = "https://vnexpress.net/rss/tin-moi-nhat.rss"): List<RssItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        try {
            val response = client.newCall(request).execute()
            val xmlResponse = response.body?.string() ?: return@withContext emptyList()
            parseRss(xmlResponse)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseRss(xml: String): List<RssItem> {
        val items = mutableListOf<RssItem>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var currentItem: RssItem? = null
        var text = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        currentItem = RssItem()
                    }
                }
                XmlPullParser.TEXT -> {
                    text = parser.text
                }
                XmlPullParser.END_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        currentItem?.let { items.add(it) }
                    } else if (currentItem != null) {
                        when (tagName.lowercase()) {
                            "title" -> currentItem.title = text
                            "link" -> currentItem.link = text
                            "description" -> {
                                // VNExpress puts image in description like <a href=...><img src="..."></a>Text...
                                currentItem.description = text
                                val imgRegex = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex()
                                val match = imgRegex.find(text)
                                if (match != null) {
                                    currentItem.imageUrl = match.groupValues[1]
                                }
                            }
                            "pubdate" -> currentItem.pubDate = text
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return items
    }
}

data class RssItem(
    var title: String = "",
    var link: String = "",
    var description: String = "",
    var pubDate: String = "",
    var imageUrl: String? = null
)
