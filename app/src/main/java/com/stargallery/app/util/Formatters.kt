package com.stargallery.app.util

import com.stargallery.app.data.local.ImageQuality
import com.stargallery.app.data.remote.TmdbConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ImageKind { PROFILE, POSTER, HERO }

/** Builds TMDB image URLs for the chosen quality. Returns null when there is no path. */
fun imageUrl(path: String?, kind: ImageKind, quality: ImageQuality): String? {
    if (path.isNullOrBlank()) return null
    val size = when (kind) {
        ImageKind.PROFILE -> quality.profileSize
        ImageKind.POSTER -> quality.posterSize
        ImageKind.HERO -> quality.heroSize
    }
    return "${TmdbConfig.IMAGE_BASE_URL}$size$path"
}

/** "1990-04-12" -> "12 Apr 1990"; falls back to the raw value if unparsable. */
fun formatIsoDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(parsed as Date)
    }.getOrDefault(iso)
}

/** Age in years from ISO birthday (and optional ISO death date). */
fun ageFrom(birthday: String?, deathday: String? = null): Int? {
    val b = birthday?.take(10)?.split("-")?.mapNotNull { it.toIntOrNull() }
    if (b == null || b.size != 3) return null
    val end = deathday?.take(10)?.split("-")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 3 }
        ?: java.util.Calendar.getInstance().let { listOf(it.get(1), it.get(2) + 1, it.get(5)) }
    var age = end[0] - b[0]
    if (end[1] < b[1] || (end[1] == b[1] && end[2] < b[2])) age--
    return age.takeIf { it in 0..130 }
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
