package com.example.gpnflix.data.model

import java.util.Locale

data class RegionLocaleConfig(
    val languageCode: String,
    val regionCode: String,
    val displayLabel: String,
    val frenchOnly: Boolean = true
) {
    val tmdbLanguage: String
        get() = "$languageCode-$regionCode"

    val includeVideoLanguage: String
        get() = if (frenchOnly) "fr,null" else if (languageCode == "en") "en,null" else "$languageCode,en,null"

    val includeImageLanguage: String
        get() = if (frenchOnly) "fr,null" else "$languageCode,en,null"

    val archiveLanguageQuery: String
        get() = when {
            frenchOnly || languageCode == "fr" -> "(language:fre OR language:French OR language:fra)"
            languageCode == "es" -> "(language:spa OR language:Spanish)"
            languageCode == "de" -> "(language:ger OR language:German OR language:deu)"
            languageCode == "it" -> "(language:ita OR language:Italian)"
            languageCode == "pt" -> "(language:por OR language:Portuguese)"
            else -> "(language:eng OR language:English OR language:fre OR language:French)"
        }

    companion object {
        fun fromPreference(languagePreference: String): RegionLocaleConfig {
            val deviceLocale = Locale.getDefault()
            val country = deviceLocale.country
                .uppercase(Locale.ROOT)
                .takeIf { it.length == 2 && it.all(Char::isLetter) }
                ?: "FR"
            return when (languagePreference) {
                "English" -> RegionLocaleConfig(
                    languageCode = "en",
                    regionCode = if (country in setOf("US", "GB", "CA", "AU")) country else "US",
                    displayLabel = "EN",
                    frenchOnly = false
                )
                "Español" -> RegionLocaleConfig(
                    languageCode = "es",
                    regionCode = if (country in setOf("ES", "MX", "AR", "CO")) country else "ES",
                    displayLabel = "ES",
                    frenchOnly = false
                )
                "Auto (Région)" -> {
                    val lang = deviceLocale.language.lowercase(Locale.ROOT).ifBlank { "fr" }
                    val supportedLang = if (lang in setOf("fr", "en", "es", "de", "it", "pt")) lang else "fr"
                    RegionLocaleConfig(
                        languageCode = supportedLang,
                        regionCode = country,
                        displayLabel = "${supportedLang.uppercase(Locale.ROOT)}-$country",
                        frenchOnly = supportedLang == "fr"
                    )
                }
                else -> RegionLocaleConfig(
                    languageCode = "fr",
                    regionCode = if (country in setOf("FR", "BE", "CH", "CA", "LU", "MC", "CD", "SN", "CI")) country else "FR",
                    displayLabel = "FR uniquement",
                    frenchOnly = true
                )
            }
        }
    }
}
