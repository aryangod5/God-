package com.example.god

import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/** Public-web research with no API key. */
data class WebSource(val title: String, val url: String, val snippet: String)
data class WebResearchResult(
    val query: String,
    val sources: List<WebSource>,
    val extractedText: String
)

class WebResearchManager {

    fun research(query: String, maxSources: Int = 6): WebResearchResult {
        val sources = search(query, maxSources)
        val text = buildString {
            sources.forEachIndexed { index, source ->
                append("SOURCE ${index + 1}\n")
                append("TITLE: ${source.title}\n")
                append("URL: ${source.url}\n")
                if (source.snippet.isNotBlank()) append("SNIPPET: ${source.snippet}\n")

                // Public pages are fetched only to obtain more context for the local summarizer.
                val page = fetch(source.url, 7000)
                if (page.isNotBlank()) {
                    append("PAGE TEXT: ")
                    append(page.take(6500))
                    append('\n')
                }
                append('\n')
            }
        }
        return WebResearchResult(query, sources, text.take(42000))
    }

    private fun search(query: String, max: Int): List<WebSource> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val endpoints = listOf(
            "https://html.duckduckgo.com/html/?q=$encoded",
            "https://www.google.com/search?q=$encoded"
        )

        for (url in endpoints) {
            try {
                val html = get(url, 12000)
                val parsed = parseDuckLikeResults(html, max)
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Exception) {
                // Try the next public endpoint.
            }
        }
        return emptyList()
    }

    private fun parseDuckLikeResults(html: String, max: Int): List<WebSource> {
        val result = ArrayList<WebSource>()

        // DuckDuckGo's HTML results contain result__a/result__snippet classes.
        val anchorRegex = Regex(
            "(?is)<a[^>]*class=[\\\"'][^\\\"']*result__a[^\\\"']*[\\\"'][^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>"
        )
        val anchors = anchorRegex.findAll(html).toList()

        for (match in anchors) {
            if (result.size >= max) break
            val rawUrl = match.groupValues[1]
            val url = decode(rawUrl)
            if (!url.startsWith("http", true)) continue

            val title = clean(match.groupValues[2])
            val start = match.range.last
            val tail = html.substring(start, minOf(html.length, start + 6000))
            val snippetMatch = Regex(
                "(?is)class=[\\\"'][^\\\"']*result__snippet[^\\\"']*[\\\"'][^>]*>(.*?)</(?:a|div)>"
            ).find(tail)
            val snippet = if (snippetMatch != null) clean(snippetMatch.groupValues[1]) else ""

            if (title.isNotBlank()) result += WebSource(title, url, snippet)
        }

        // Fallback for simple search-result links if the first parser finds nothing.
        if (result.isEmpty()) {
            val simple = Regex("(?is)<a[^>]+href=[\\\"'](https?://[^\\\"']+)[\\\"'][^>]*>(.*?)</a>")
            for (m in simple.findAll(html)) {
                if (result.size >= max) break
                val url = m.groupValues[1]
                val title = clean(m.groupValues[2])
                if (title.length >= 4 && title.length <= 180 && !url.contains("google.com/search")) {
                    result += WebSource(title, url, "")
                }
            }
        }

        return result.distinctBy { it.url }
    }

    private fun decode(value: String): String {
        return try {
            val uri = Uri.parse(value)
            uri.getQueryParameter("uddg") ?: value
        } catch (_: Exception) {
            value
        }
    }

    private fun fetch(url: String, limit: Int): String {
        if (!(url.startsWith("https://") || url.startsWith("http://"))) return ""
        return try {
            clean(get(url, 10000).take(limit))
        } catch (_: Exception) {
            ""
        }
    }

    private fun get(url: String, timeout: Int): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = timeout
        connection.readTimeout = timeout
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; GOD/1.0)")
        connection.setRequestProperty("Accept-Language", "en-IN,en;q=0.9")
        connection.setRequestProperty("Accept", "text/html, text/plain;q=0.9, */*;q=0.7")

        return try {
            val stream = if (connection.responseCode in 200..399) connection.inputStream else connection.errorStream
            if (stream == null) "" else BufferedReader(
                InputStreamReader(stream, StandardCharsets.UTF_8)
            ).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun clean(value: String): String {
        return value
            .replace(Regex("(?is)<script.*?</script>"), " ")
            .replace(Regex("(?is)<style.*?</style>"), " ")
            .replace(Regex("(?is)<noscript.*?</noscript>"), " ")
            .replace(Regex("(?is)<svg.*?</svg>"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
