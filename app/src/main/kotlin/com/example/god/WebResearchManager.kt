package com.example.god

import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.regex.Pattern

data class WebSource(val title: String, val url: String, val snippet: String)

data class WebResearchResult(
    val query: String,
    val sources: List<WebSource>,
    val extractedText: String
)

/**
 * No API key is required.
 *
 * GOD searches the public web, collects a small set of result pages/snippets,
 * strips boilerplate HTML, and returns only text relevant to the question.
 *
 * This is deliberately a lightweight Android implementation. Sites that block
 * automated requests, require JavaScript, login, CAPTCHA, or a private API
 * cannot be read by this class.
 */
class WebResearchManager {
    fun research(query: String, maxSources: Int = 5): WebResearchResult {
        val sources = searchDuckDuckGo(query, maxSources)
        val extracted = buildString {
            sources.forEachIndexed { index, source ->
                append("SOURCE ").append(index + 1).append('\n')
                append("TITLE: ").append(source.title).append('\n')
                append("URL: ").append(source.url).append('\n')
                append("SNIPPET: ").append(source.snippet).append('\n')
                val page = fetchText(source.url, 7000)
                if (page.isNotBlank()) {
                    append("PAGE TEXT: ").append(page.take(7000)).append('\n')
                }
                append('\n')
            }
        }
        return WebResearchResult(query, sources, extracted.take(35000))
    }

    private fun searchDuckDuckGo(query: String, maxSources: Int): List<WebSource> {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val html = get("https://html.duckduckgo.com/html/?q=$encoded", 12000)
            val blockPattern = Pattern.compile(
                """(?s)<div[^>]+class="result__body"[^>]*>(.*?)</div>\s*</div>"""
            )
            val linkPattern = Pattern.compile(
                """class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>"""
            )
            val snippetPattern = Pattern.compile(
                """class="result__snippet"[^>]*>(.*?)</a>|class="result__snippet"[^>]*>(.*?)</div>"""
            )
            val out = ArrayList<WebSource>()
            val blocks = blockPattern.matcher(html)
            while (blocks.find() && out.size < maxSources) {
                val block = blocks.group(1) ?: continue
                val lm = linkPattern.matcher(block)
                if (!lm.find()) continue
                val rawUrl = decodeRedirect(lm.group(1) ?: "")
                if (!rawUrl.startsWith("http")) continue
                val title = cleanHtml(lm.group(2) ?: "")
                val sm = snippetPattern.matcher(block)
                val snippet = if (sm.find()) cleanHtml(sm.group(1) ?: sm.group(2) ?: "") else ""
                out.add(WebSource(title, rawUrl, snippet))
            }
            out.distinctBy { it.url }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun decodeRedirect(value: String): String {
        return try {
            val uri = Uri.parse(value)
            val uddg = uri.getQueryParameter("uddg")
            uddg ?: value
        } catch (_: Exception) {
            value
        }
    }

    private fun fetchText(url: String, limit: Int): String {
        return try {
            cleanHtml(get(url, 10000).take(limit))
        } catch (_: Exception) {
            ""
        }
    }

    private fun get(urlString: String, timeout: Int): String {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeout
            readTimeout = timeout
            requestMethod = "GET"
            setRequestProperty("User-Agent", "GOD-Android/1.0")
            instanceFollowRedirects = true
        }
        return try {
            val stream = if (conn.responseCode in 200..399) conn.inputStream else conn.errorStream
            BufferedReader(InputStreamReader(stream)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun cleanHtml(value: String): String {
        return value
            .replace(Regex("(?is)<script.*?</script>"), " ")
            .replace(Regex("(?is)<style.*?</style>"), " ")
            .replace(Regex("(?is)<noscript.*?</noscript>"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
