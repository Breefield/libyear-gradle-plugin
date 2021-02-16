package com.libyear.sourcing

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Instant

class SolrSearchAdapter(
  private val repoUrl: String,
) : VersionInfoAdapter {

  private val client = OkHttpClient()
  private val mapper = ObjectMapper()

  override fun getArtifactCreated(m: ModuleVersionIdentifier, repository: ArtifactRepository): Instant? {
    val url = buildUrl(m)
    val response = getResponseText(url)
    return response?.let(::parse)
  }

  private fun buildUrl(m: ModuleVersionIdentifier): HttpUrl {
    return repoUrl.toHttpUrl().newBuilder()
      .setQueryParameter("q", encodeToQueryParameter(m))
      .build()
  }

  private fun encodeToQueryParameter(m: ModuleVersionIdentifier): String {
    return """g:"${m.group}" AND a:"${m.name}" AND v:"${m.version}""""
  }

  private fun getResponseText(url: HttpUrl): String? {
    val request = Request.Builder().url(url).build()
    try {
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          return response.body?.string()
        }
      }
    } catch (e: IOException) {
      LOG.debug("SOLR search request failed for URL {}", url, e)
    }
    return null
  }

  private fun parse(response: String): Instant? {
    val json = mapper.readTree(response)
    val docs = json["response"].withArray("docs")
    if (docs.size() >= 1) {
      val created = docs[0]["timestamp"].asLong()
      return Instant.ofEpochMilli(created)
    }
    return null
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(SolrSearchAdapter::class.java)

    fun forMavenCentral() = SolrSearchAdapter(
      repoUrl = "https://search.maven.org/solrsearch/select"
    )
  }
}
