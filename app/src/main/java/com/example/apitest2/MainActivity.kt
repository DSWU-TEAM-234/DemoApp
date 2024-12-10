package com.example.apitest2

import MetronomeManager
import SpotifyApiClient
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import android.content.pm.PackageManager // PackageManager import 추가
import android.net.Uri // Uri import 추가
import android.widget.ImageButton
import android.widget.Toast
import android.widget.TextView


import android.media.SoundPool
import kotlinx.coroutines.*
import java.util.Timer




class MainActivity : AppCompatActivity() {

    private val CLIENT_ID = "c94d8d8ac11940508342148aaacf3636"
    private val REDIRECT_URI = "temrunapitest://callback"
    private val REQUEST_CODE = 1337
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var accessToken: String? = null

    /*
    private lateinit var melody1Manager: AudioManager
    private lateinit var melody2Manager: AudioManager
    */

    private lateinit var soundPool: SoundPool
    private var soundId: Int = 0

    private var Tempo : Double = 0.0

    private var isPlaying = true

    private var spotifyAPI = "spotify:album:5EtwY9I5SXbAFJRMr3rkab"
    private var SongName = "APT."

    private lateinit var metronomeManager: MetronomeManager
    private lateinit var spotifyApiClient:SpotifyApiClient




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*

        melody1Manager = AudioManager(this, R.raw.melody1)
        melody2Manager = AudioManager(this, R.raw.melody2)

        val melodyButton: Button = findViewById(R.id.melodyButton)
        val drumButton: Button = findViewById(R.id.drumButton)

        melodyButton.setOnClickListener {
            melody1Manager.togglePlayback();
        }

        // Drum button click listener
        drumButton.setOnClickListener {
            melody2Manager.togglePlayback();
        }
        */

        // Spotify 설치 여부 확인 및 안내
        if (!isSpotifyInstalled()) {
            // Spotify 설치 안내
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music"))
            startActivity(intent)
            finish() // 현재 액티비티 종료
            return
        }

        authenticateSpotify() // Spotify 인증 요청

        val playPauseButton: ImageButton = findViewById(R.id.playPauseButton)

        playPauseButton.setOnClickListener {
            if (isPlaying) {
                // 일시정지 상태로 변경
                pausePlayback()
                playPauseButton.setImageResource(R.drawable.ic_play) // 재생 아이콘으로 변경
            } else {
                // 재생 상태로 변경
                resumePlayback()
                playPauseButton.setImageResource(R.drawable.ic_pause) // 일시정지 아이콘으로 변경
            }

            // 상태 토글
            isPlaying = !isPlaying
        }



        initializeSound()
        metronomeManager = MetronomeManager(soundPool, soundId)





    }

    private fun authenticateSpotify() {
        val builder = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            REDIRECT_URI
        )
        builder.setScopes(arrayOf("streaming", "user-read-private", "user-modify-playback-state", "user-read-playback-state", "user-library-read")) // 필요한 Scope 추가
        val request = builder.build()

        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, intent)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    accessToken = response.accessToken
                    Log.d("MainActivity", "Access Token: $accessToken")
                    connectToSpotifyAppRemote() // Spotify Remote 연결



                }
                AuthorizationResponse.Type.ERROR -> {
                    Log.e("MainActivity", "Auth error: ${response.error}")
                }
                else -> {
                    Log.d("MainActivity", "Auth result: ${response.type}")
                }
            }
        }
    }

    private fun connectToSpotifyAppRemote() {
        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("MainActivity", "Spotify App Remote 연결 성공!")
                //startPlayback()
                fetchTrackFeatures(SongName)
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("MainActivity", "Spotify App Remote 연결 실패: ${throwable.message}")
            }
        })
    }

    private fun startPlaybackWithMetronome(tempo: Double) {
        spotifyAppRemote?.playerApi?.play(spotifyAPI)
            ?.setResultCallback {
                Log.d("MainActivity", "노래 재생 시작 요청됨")

                spotifyApiClient = SpotifyApiClient(accessToken!!)

                /*spotifyApiClient.fetchTrackAudioAnalysis(SongName) { sections ->
                    sections?.let {
                        startDynamicMetronome(it)
                    }
                }*/
                metronomeManager.startMetronome(tempo)

                // Spotify 트랙 상태를 구독
                spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
                    if (!playerState.isPaused) {
                        Log.d("MainActivity", "노래 재생 시작 확인됨!")

                    }
                }
            }
            ?.setErrorCallback { error ->
                Log.e("MainActivity", "노래 재생 실패: ${error.message}")
            }
    }

    private fun pausePlayback() {
        spotifyAppRemote?.playerApi?.pause()
            ?.setResultCallback {
                Toast.makeText(this, "음악 재생 멈춤", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "음악 재생 멈춤")
            }
            ?.setErrorCallback { error ->
                Toast.makeText(this, "멈춤 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "멈춤 실패: ${error.message}")
            }
    }

    private fun resumePlayback() {
        spotifyAppRemote?.playerApi?.resume()
            ?.setResultCallback {
                Toast.makeText(this, "음악 재생 재개", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "음악 재생 재개")
            }
            ?.setErrorCallback { error ->
                Toast.makeText(this, "재생 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "재생 실패: ${error.message}")
            }
    }

    override fun onStop() {
        super.onStop()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources
        /*melody1Manager.release()
        melody2Manager.release()*/
        soundPool.release() // SoundPool 해제
    }

    // Spotify 설치 여부 확인
    private fun isSpotifyInstalled(): Boolean {
        val packageManager = packageManager
        return try {
            packageManager.getPackageInfo("com.spotify.music", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun fetchTrackFeatures(query: String) {
        if (accessToken.isNullOrEmpty()) {
            Log.e("MainActivity", "Access token is null or empty")
            return
        }

        spotifyApiClient = SpotifyApiClient(accessToken!!)

        //파트 나눠서 BPM가져오는 코드
        /*spotifyApiClient.fetchTrackAudioAnalysis(query) { sections ->
            sections?.let {
                startDynamicMetronome(it)
            }
        }*/


        spotifyApiClient.fetchTrackAudioFeatures(query) { jsonObject ->
            if (jsonObject != null) {
                val tempo = jsonObject.getDouble("tempo")
                Tempo = tempo
                val danceability = jsonObject.getDouble("danceability")
                val energy = jsonObject.getDouble("energy")

                Log.d("TrackFeatures", "Tempo: $tempo BPM")
                Log.d("TrackFeatures", "Danceability: $danceability")
                Log.d("TrackFeatures", "Energy: $energy")


                runOnUiThread {
                    // UI 업데이트 (TextView에 tempo 표시)
                    val tempoTextView: TextView = findViewById(R.id.tempoTextView)
                    tempoTextView.text = "Tempo: $tempo BPM"
                }

                // 템포 결과 나온 후에 플레이 & 템포 플레이
                startPlaybackWithMetronome(tempo)

            } else {
                Log.e("TrackFeatures", "Failed to fetch track audio features")
            }
        }
    }


   private fun initializeSound() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(1) // 동시에 한 개의 소리만 재생
            .build()

        soundId = soundPool.load(this, R.raw.click_sound, 1)
    }

    private fun startDynamicMetronome(sections: List<Pair<Double, Double>>) {
        CoroutineScope(Dispatchers.Main).launch {
            var lastStartTime = 0.0
            for ((startTime, bpm) in sections) {
                try {
                    val delayTime = ((startTime - lastStartTime) * 1000).toLong()
                    if (delayTime > 0) {
                        delay(delayTime)
                    }
                    lastStartTime = startTime
                    Log.d("DynamicMetronome", "Waiting for start time: $startTime, BPM: $bpm")

                    // 다음 섹션의 시작 시간까지 대기
                    delay((startTime * 1000).toLong())

                    // BPM 변경 호출
                    metronomeManager.changeBpm(bpm)
                    Log.d("DynamicMetronome", "Start time: $startTime, BPM: $bpm, Delay: ${(startTime * 1000).toLong()}")

                } catch (e: Exception) {
                    Log.e("DynamicMetronome", "Error in processing section: ${e.message}")
                }
            }

            // 모든 섹션이 끝난 후
            metronomeManager.stopMetronome()
            Log.d("DynamicMetronome", "Metronome stopped after processing all sections")
        }
    }



}
