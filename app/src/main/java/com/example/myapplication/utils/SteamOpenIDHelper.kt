package com.example.myapplication.utils

import android.net.Uri
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

object SteamOpenIDHelper {

    fun extractSteamId(claimedId: String): String? {
        // URL выглядит так: https://steamcommunity.com/openid/id/76561197960435530
        val pattern = Regex("https://steamcommunity.com/openid/id/(\\d+)")
        val matchResult = pattern.find(claimedId)
        return matchResult?.groupValues?.get(1)
    }

    fun getSteamLoginUrl(returnUrl: String): String {
        // Генерируем уникальный ID для сессии
        val nonce = generateNonce()

        return Uri.parse("https://steamcommunity.com/openid/login").buildUpon()
            .appendQueryParameter("openid.ns", "http://specs.openid.net/auth/2.0")
            .appendQueryParameter("openid.mode", "checkid_setup")
            .appendQueryParameter("openid.return_to", returnUrl)
            .appendQueryParameter("openid.realm", returnUrl)
            .appendQueryParameter("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select")
            .appendQueryParameter("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select")
            .build()
            .toString()
    }

    private fun generateNonce(): String {
        val random = Random()
        val nonceBytes = ByteArray(20)
        random.nextBytes(nonceBytes)
        val md = MessageDigest.getInstance("SHA-1")
        md.update(nonceBytes)
        return BigInteger(1, md.digest()).toString(16)
    }
}