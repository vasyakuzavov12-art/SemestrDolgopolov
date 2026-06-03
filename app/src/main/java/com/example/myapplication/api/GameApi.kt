package com.example.myapplication.api

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Credentials
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

interface GameApi {
    @GET("api/storesearch/")
    suspend fun searchGames(
        @Query("term") term: String,
        @Query("l") language: String = "english",
        @Query("cc") currency: String = "us",
        @Query("count") count: Int = 50
    ): SteamSearchResponse

    @GET("api/appdetails/")
    suspend fun getGameDetails(
        @Query("appids") appId: Int,
        @Query("l") language: String = "english"
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

data class Screenshot(
    val id: Int,
    val path_thumbnail: String,
    val path_full: String
)

data class MovieData(
    @SerializedName("480")
    val video480: String?,
    val max: String?
)

data class Movie(
    val id: Int,
    val name: String,
    val thumbnail: String,
    val webm: MovieData?,
    val mp4: MovieData?
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
    val website: String?,
    val screenshots: List<Screenshot>?,
    val movies: List<Movie>?
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
        get() = developers?.joinToString(", ") ?: "Не указаны"

    val displayPublishers: String
        get() = publishers?.joinToString(", ") ?: "Не указаны"

    val displayReleaseDate: String
        get() = release_date?.date ?: "Не указана"

    val displayGenres: String
        get() = genres?.joinToString(", ") { it.name ?: "" } ?: "Не указан"

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


val PROXY_HOST = ""
val PROXY_PORT = 3128
val PROXY_USER = ""
val PROXY_PASS = ""
val USE_PROXY = false


val okHttpClient: OkHttpClient by lazy {
    val builder = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)


    if (USE_PROXY && PROXY_HOST.isNotEmpty()) {
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(PROXY_HOST, PROXY_PORT))
        builder.proxy(proxy)


        if (PROXY_USER.isNotEmpty() && PROXY_PASS.isNotEmpty()) {
            val credentials = Credentials.basic(PROXY_USER, PROXY_PASS)
            builder.proxyAuthenticator { _, response ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", credentials)
                    .build()
            }
        }
    }

    builder.build()
}


val gameApi: GameApi by lazy {
    Retrofit.Builder()
        .baseUrl("https://store.steampowered.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GameApi::class.java)
}