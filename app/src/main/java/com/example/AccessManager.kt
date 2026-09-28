package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * AccessManager handles remote email-based subscription verification.
 * Fetches the remote JSON array of subscriber emails from the Gist URL.
 */
object AccessManager {

    const val SUBSCRIBERS_URL = "https://gist.githubusercontent.com/hdggdts-creator/raw/subscribers.json"
    const val TELEGRAM_CONTACT_URL = "https://t.me/MMF5C3"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    sealed class AccessResult {
        data object Granted : AccessResult()
        data class Blocked(val email: String, val message: String? = null) : AccessResult()
        data class NetworkError(val message: String) : AccessResult()
    }

    /**
     * Verifies if the given user email is authorized in the remote subscriber JSON array.
     * Executes HTTP GET on Dispatchers.IO background thread and handles network failure gracefully.
     */
    suspend fun verifyAccess(email: String): AccessResult = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return@withContext AccessResult.Blocked("", "No valid email address found for the signed-in account.")
        }

        try {
            val request = Request.Builder()
                .url(SUBSCRIBERS_URL)
                .header("Cache-Control", "no-cache, no-store")
                .header("Pragma", "no-cache")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        Log.w("AccessManager", "Subscriber gist returned 404. Restricting access for: $trimmedEmail")
                        return@withContext AccessResult.Blocked(
                            email = trimmedEmail,
                            message = "The subscription list is currently unavailable or empty (HTTP 404). Access is restricted until your subscription is activated."
                        )
                    }
                    return@withContext AccessResult.NetworkError("Server returned HTTP ${response.code} (${response.message})")
                }

                val body = response.body?.string().orEmpty()
                val subscribers = parseSubscribersJson(body)
                val normalizedEmail = trimmedEmail.lowercase()
                val isSubscribed = subscribers.any { it.trim().lowercase() == normalizedEmail }

                Log.d("AccessManager", "Checked subscriber access for '$normalizedEmail': isSubscribed=$isSubscribed, totalSubscribers=${subscribers.size}")

                if (isSubscribed) {
                    AccessResult.Granted
                } else {
                    AccessResult.Blocked(
                        email = trimmedEmail,
                        message = "Your email is not currently listed in the active subscribers directory."
                    )
                }
            }
        } catch (e: IOException) {
            Log.e("AccessManager", "Network failure while verifying access for $trimmedEmail", e)
            AccessResult.NetworkError("Network connection failure. Please check your internet and tap 'Refresh Access'.")
        } catch (e: Exception) {
            Log.e("AccessManager", "Unexpected error during access check", e)
            AccessResult.NetworkError("Error verifying subscription: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Parses the remote JSON array containing subscriber email strings or objects.
     */
    fun parseSubscribersJson(jsonString: String): List<String> {
        val list = mutableListOf<String>()
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) return list

        try {
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    when (val item = array.opt(i)) {
                        is String -> if (item.isNotBlank()) list.add(item.trim())
                        is JSONObject -> {
                            val emailField = item.optString("email", "")
                                .ifBlank { item.optString("mail", "") }
                            if (emailField.isNotBlank()) list.add(emailField.trim())
                        }
                        else -> {
                            val str = item?.toString().orEmpty().trim()
                            if (str.isNotBlank()) list.add(str)
                        }
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                val array = obj.optJSONArray("subscribers")
                    ?: obj.optJSONArray("emails")
                    ?: obj.optJSONArray("users")
                if (array != null) {
                    for (i in 0 until array.length()) {
                        when (val item = array.opt(i)) {
                            is String -> if (item.isNotBlank()) list.add(item.trim())
                            is JSONObject -> {
                                val emailField = item.optString("email", "")
                                if (emailField.isNotBlank()) list.add(emailField.trim())
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AccessManager", "Failed to parse subscribers JSON payload: $jsonString", e)
        }
        return list
    }
}
