package com.example.god

import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

data class WebSource(val title: String, val url: String, val snippet: String)
data class WebResearchResult(val query: String, val sources: List<WebSource>, val extractedText: String)

/** Public-web research with no API key. */
class WebResearchManager {
    fun research(query: String, maxSources: Int = 5): WebResearchResult {
        val sources = search(query, maxSources)
        val text = buildString {
            sources.forEachIndexed { i, s ->
                append("SOURCE ${i + 1}\nTITLE: ${s.title}\nURL: ${s.url}\nSNIPPET: ${s.snippet}\n")
                val page = fetch(s.url, 5000)
                if (page.isNotBlank()) append("PAGE TEXT: ${page.take(5000)}\n")
                append('\n')
            }
        }
        return WebResearchResult(query, sources, text.take(30000))
    }

    private fun search(query: String, max: Int): List<WebSource> {
        val endpoints = listOf(
            "https://html.duckduckgo.com/html/?q=",
            "https://www.google.com/search?q="
        )
        for (endpoint in endpoints) {
            try {
                val html = get(endpoint + URLEncoder.encode(query, "UTF-8"), 12000)
                val parsed = parse(html, max)
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Exception) { }
        }
        return emptyList()
    }

    private fun parse(html: String, max: Int): List<WebSource> {
        val out = ArrayList<WebSource>()
        val block = Pattern.compile("(?s)<div[^>]+class=[\\\"'][^\\\"']*result__body[^\\\"']*[\\\"'][^>]*>(.*?)</div>\\s*</div>", Pattern.CASE_INSENSITIVE).matcher(html)
        while (block.find() && out.size < max) {
            val b = block.group(1).orEmpty()
            val link = Pattern.compile("class=[\\\"'][^\\\"']*result__a[^\\\"']*[\\\"'][^>]*href=[\\\"']([^\\\"']+)[\\\"'][^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(b)
            if (!link.find()) continue
            val url = decode(link.group(1).orEmpty())
            if (!url.startsWith("http")) continue
            val title = clean(link.group(2).orEmpty())
            val sm = Pattern.compile("class=[\\\"'][^\\\"']*result__snippet[^\\\"']*[\\\"'][^>]*>(.*?)</(?:a|div)>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(b)
            val snippet = if (sm.find()) clean(sm.group(1).orEmpty()) else ""
            out += WebSource(title, url, snippet)
        }
        return out.distinctBy { it.url }
    }

    private fun decode(value: String): String = try { Uri.parse(value).getQueryParameter("uddg") ?: value } catch (_: Exception) { value }

    private fun fetch(url: String, limit: Int): String = try { clean(get(url, 10000).take(limit)) } catch (_: Exception) { "" }

    private fun get(url: String, timeout: Int): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = timeout; c.readTimeout = timeout; c.requestMethod = "GET"
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; GOD/1.0)")
        c.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        return try {
            val stream = if (c.responseCode in 200..399) c.inputStream else c.errorStream
            if (stream == null) "" else BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        } finally { c.disconnect() }
    }

    private fun clean(v: String): String = v
        .replace(Regex("(?is)<script.*?</script>"), " ")
        .replace(Regex("(?is)<style.*?</style>"), " ")
        .replace(Regex("(?is)<noscript.*?</noscript>"), " ")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#x27;", "'")
        .replace("&lt;", "<").replace("&gt;", ">")
        .replace(Regex("\\s+"), " ").trim()
}
