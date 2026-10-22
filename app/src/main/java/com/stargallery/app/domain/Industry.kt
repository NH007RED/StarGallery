package com.stargallery.app.domain

/**
 * A "cinema industry" used to explore performers.
 *
 * IMPORTANT: TMDB has no authoritative country-of-origin field for people, and no
 * "actresses by country" endpoint. An industry here is a *film filter*
 * (`with_original_language` + `with_origin_country` on /discover/movie). Performers
 * shown for an industry are people credited in the cast of popular films matching
 * that filter. This is an association, NOT a statement of nationality.
 */
data class Industry(
    val id: String,
    val label: String,
    val group: String,
    val originalLanguage: String?,
    val originCountry: String?
)

object Industries {
    const val GROUP_HOLLYWOOD = "Hollywood"
    const val GROUP_BOLLYWOOD = "Bollywood"
    const val GROUP_SOUTH_INDIAN = "South Indian"
    const val GROUP_EAST_ASIAN = "East Asian"
    const val GROUP_EUROPEAN = "European"
    const val GROUP_AFRICAN = "African"
    const val GROUP_LATIN = "Latin American"
    const val GROUP_OTHER = "Other"

    val all: List<Industry> = listOf(
        Industry("hollywood", "Hollywood", GROUP_HOLLYWOOD, "en", "US"),
        Industry("bollywood", "Bollywood (Hindi)", GROUP_BOLLYWOOD, "hi", "IN"),
        Industry("tamil", "Tamil", GROUP_SOUTH_INDIAN, "ta", "IN"),
        Industry("telugu", "Telugu", GROUP_SOUTH_INDIAN, "te", "IN"),
        Industry("malayalam", "Malayalam", GROUP_SOUTH_INDIAN, "ml", "IN"),
        Industry("kannada", "Kannada", GROUP_SOUTH_INDIAN, "kn", "IN"),
        Industry("korean", "Korean", GROUP_EAST_ASIAN, "ko", "KR"),
        Industry("japanese", "Japanese", GROUP_EAST_ASIAN, "ja", "JP"),
        Industry("chinese", "Chinese (Mandarin)", GROUP_EAST_ASIAN, "zh", "CN"),
        Industry("hongkong", "Hong Kong (Cantonese)", GROUP_EAST_ASIAN, "cn", "HK"),
        Industry("british", "British", GROUP_EUROPEAN, "en", "GB"),
        Industry("french", "French", GROUP_EUROPEAN, "fr", "FR"),
        Industry("german", "German", GROUP_EUROPEAN, "de", "DE"),
        Industry("italian", "Italian", GROUP_EUROPEAN, "it", "IT"),
        Industry("spanish", "Spanish (Spain)", GROUP_EUROPEAN, "es", "ES"),
        Industry("nigerian", "Nigerian (Nollywood)", GROUP_AFRICAN, "en", "NG"),
        Industry("egyptian", "Egyptian", GROUP_AFRICAN, "ar", "EG"),
        Industry("mexican", "Mexican", GROUP_LATIN, "es", "MX"),
        Industry("brazilian", "Brazilian", GROUP_LATIN, "pt", "BR"),
        Industry("argentine", "Argentine", GROUP_LATIN, "es", "AR"),
        Industry("turkish", "Turkish", GROUP_OTHER, "tr", "TR"),
        Industry("thai", "Thai", GROUP_OTHER, "th", "TH")
    )

    fun byId(id: String?): Industry? = all.firstOrNull { it.id == id }

    /** Short list of chips for the Home screen (one per major industry/group). */
    val homeChips: List<Industry> = listOf(
        "hollywood", "bollywood", "tamil", "telugu", "korean", "japanese",
        "chinese", "french", "nigerian", "mexican", "brazilian", "turkish"
    ).mapNotNull { byId(it) }

    /** Industries shown as rows in the Home "International cinema" section. */
    val homeSections: List<Industry> = listOf("korean", "japanese", "bollywood", "french", "nigerian")
        .mapNotNull { byId(it) }
}

/** Languages TMDB content can be requested in. */
data class ContentLanguage(val code: String, val label: String)

object ContentLanguages {
    val all = listOf(
        ContentLanguage("en-US", "English"),
        ContentLanguage("hi-IN", "हिन्दी (Hindi)"),
        ContentLanguage("bn-BD", "বাংলা (Bengali)"),
        ContentLanguage("ta-IN", "தமிழ் (Tamil)"),
        ContentLanguage("te-IN", "తెలుగు (Telugu)"),
        ContentLanguage("ko-KR", "한국어 (Korean)"),
        ContentLanguage("ja-JP", "日本語 (Japanese)"),
        ContentLanguage("zh-CN", "中文 (Chinese)"),
        ContentLanguage("fr-FR", "Français"),
        ContentLanguage("es-ES", "Español"),
        ContentLanguage("pt-BR", "Português (Brasil)")
    )
}
