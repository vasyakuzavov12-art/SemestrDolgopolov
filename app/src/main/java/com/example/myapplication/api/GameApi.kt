package com.example.myapplication.api

import retrofit2.http.GET
import retrofit2.http.Query

interface GameApi {
    @GET("api/storesearch/")
    suspend fun searchGames(
        @Query("term") term: String,
        @Query("l") language: String = "russian",
        @Query("cc") currency: String = "ru",
        @Query("count") count: Int = 50
    ): SteamSearchResponse

    @GET("api/appdetails/")
    suspend fun getGameDetails(
        @Query("appids") appId: Int,
        @Query("l") language: String = "russian"
    ): Map<String, AppDetailsResponse>
}

data class SteamSearchResponse(
    val total: Int,
    val items: List<SteamGameItem>
)

data class SteamGameItem(
    val id: Int,
    val name: String?,
    val price: PriceData?,
    val tiny_image: String?,
    val header_image: String?
) {
    val formattedPrice: String
        get() = if (price != null && price.final > 0) {
            "${price.currency ?: "$"}${price.final / 100}"
        } else {
            "Бесплатно"
        }

    val displayName: String
        get() = name ?: "Unknown Game"

    val displayImage: String
        get() = if (!header_image.isNullOrEmpty()) header_image else tiny_image ?: ""

    val steamUrl: String
        get() = "https://store.steampowered.com/app/${id}"
}

data class PriceData(
    val final: Int,
    val initial: Int,
    val currency: String?
)

data class AppDetailsResponse(
    val success: Boolean,
    val data: GameData?
)

data class ReleaseDate(
    val date: String?
)

data class GameData(
    val id: Int,
    val name: String?,
    val short_description: String?,
    val detailed_description: String?,
    val header_image: String?,
    val release_date: ReleaseDate?,
    val developers: List<String>?,
    val publishers: List<String>?,
    val genres: List<Genre>?,
    val website: String?
) {
    val displayName: String
        get() = name ?: "Unknown Game"

    val displayDescription: String
        get() = cleanHtml(detailed_description?.takeIf { it.isNotEmpty() }
            ?: short_description
            ?: "Нет описания")

    val displayImage: String
        get() = header_image ?: ""

    val displayDevelopers: String
        get() = developers?.joinToString(", ") ?: "Unknown"

    val displayPublishers: String
        get() = publishers?.joinToString(", ") ?: "Unknown"

    val displayReleaseDate: String
        get() = release_date?.date ?: "Unknown"

    val displayGenres: String
        get() = genres?.joinToString(", ") { it.description ?: it.name ?: "" } ?: "Не указан"

    val steamUrl: String
        get() = "https://store.steampowered.com/app/${id}"

    private fun cleanHtml(html: String?): String {
        if (html.isNullOrEmpty()) return ""
        return html
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

data class Genre(
    val id: String?,
    val name: String?,
    val description: String?
)