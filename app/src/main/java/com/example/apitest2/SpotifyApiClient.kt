import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import android.util.Log

class SpotifyApiClient(private val accessToken: String) {

    private val client = OkHttpClient()
    private val token ="BQBmoaMMQoFBy4lQVH1Aaij6Ev0dHw5Mp8qRfS2aGfLiDZtEY2ARIoSeFweNWuYNR_kqlxxDz8KPg2Xk71RsSLwE2jyGTFGZWJ1ee8g_WxvdVE9hDVR33VLBS_dE1uE3P-ekkh5D6kG4A5FS5qkM2U57dQLhu-vKmuwZZ74vN5W19FxwqCgEg0-g1xKd5H_V42UzxFJ4KYVN5-1aUUGj4V6l0K3W-qAMdPuZYWNW"

    fun fetchTrackAudioFeatures(query: String, callback: (JSONObject?) -> Unit) {
        searchTrack(query) { trackId ->
            if (trackId != null) {
                Log.d("MainActivity", "Found trackId: $trackId")
                val url = "https://api.spotify.com/v1/audio-features/$trackId"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("SpotifyApiClient", "API call failed: ${e.message}")
                        callback(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!it.isSuccessful) {
                                Log.e("SpotifyApiClient", "Request failed with code: ${response.code}")
                                Log.e("SpotifyApiClient", "Response body: ${it.body?.string()}")
                                Log.d("SpotifyApiClient", "Request URL: $url")
                                Log.d("SpotifyApiClient", "Authorization Header: Bearer $accessToken")

                                callback(null)
                                return
                            }

                            val body = it.body?.string()
                            if (body != null) {
                                try {
                                    val jsonResponse = JSONObject(body)
                                    callback(jsonResponse)
                                } catch (e: Exception) {
                                    Log.e("SpotifyApiClient", "JSON parsing error: ${e.message}")
                                    callback(null)
                                }
                            } else {
                                Log.e("SpotifyApiClient", "Empty response body")
                                callback(null)
                            }
                        }
                    }
                })
            } else {
                Log.e("MainActivity", "Track not found")
                callback(null)
            }
        }
    }

    fun fetchTrackAudioAnalysis(query: String, callback: (List<Pair<Double, Double>>?) -> Unit) {
        searchTrack(query) { trackId ->
            if (trackId != null) {
                Log.d("SpotifyApiClient", "Found trackId: $trackId")
                val url = "https://api.spotify.com/v1/audio-analysis/$trackId"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("SpotifyApiClient", "API call failed: ${e.message}")
                        callback(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!it.isSuccessful) {
                                Log.e("SpotifyApiClient", "Request failed with code: ${response.code}")
                                callback(null)
                                return
                            }

                            val body = it.body?.string()
                            if (body != null) {
                                try {
                                    val jsonResponse = JSONObject(body)
                                    val sections = jsonResponse.getJSONArray("sections")
                                    val tempoList = mutableListOf<Pair<Double, Double>>()

                                    for (i in 0 until sections.length()) {
                                        val section = sections.getJSONObject(i)
                                        val start = section.getDouble("start")
                                        val tempo = section.getDouble("tempo")
                                        tempoList.add(Pair(start, tempo))
                                        Log.d("SpotifyApiClient", "Section $i - Start: $start, Tempo: $tempo")
                                    }

                                    callback(tempoList)
                                } catch (e: Exception) {
                                    Log.e("SpotifyApiClient", "JSON parsing error: ${e.message}")
                                    callback(null)
                                }
                            } else {
                                Log.e("SpotifyApiClient", "Empty response body")
                                callback(null)
                            }
                        }
                    }
                })
            } else {
                Log.e("SpotifyApiClient", "Track not found")
                callback(null)
            }
        }
    }


    fun searchTrack(query: String, callback: (String?) -> Unit) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.spotify.com/v1/search?q=$encodedQuery&type=track"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("SpotifySearch", "Search failed: ${it.code}")
                        callback(null)
                        return
                    }

                    val body = it.body?.string()
                    if (body != null) {
                        try {
                            val jsonResponse = JSONObject(body)
                            val items = jsonResponse.getJSONObject("tracks").getJSONArray("items")
                            if (items.length() > 0) {
                                val trackId = items.getJSONObject(0).getString("id")
                                callback(trackId)
                            } else {
                                Log.e("SpotifySearch", "No tracks found")
                                callback(null)
                            }
                        } catch (e: Exception) {
                            Log.e("SpotifySearch", "JSON parsing error: ${e.message}")
                            callback(null)
                        }
                    } else {
                        Log.e("SpotifySearch", "Empty response body")
                        callback(null)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("SpotifySearch", "Search API call failed: ${e.message}")
                callback(null)
            }
        })
    }


}
