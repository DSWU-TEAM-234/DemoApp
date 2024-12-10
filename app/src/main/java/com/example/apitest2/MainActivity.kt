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


//자이로센서 코드
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Button
import androidx.activity.ComponentActivity







class MainActivity : AppCompatActivity(), SensorEventListener  {

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
    private lateinit var syncManager: SyncManager



    //아래는 자이로센서코드

    private lateinit var sensorManager: SensorManager
    private var gyroSensor: Sensor? = null

    // 계산용 변수
    private var lastTimestamp: Long = 0
    private var stepCount: Int = 0

    private lateinit var stepCountText: TextView
    private lateinit var logText: TextView




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
                metronomeManager.stopMetronome()
                playPauseButton.setImageResource(R.drawable.ic_play) // 재생 아이콘으로 변경
            } else {
                // 재생 상태로 변경
                resumePlayback()

                playPauseButton.setImageResource(R.drawable.ic_pause) // 일시정지 아이콘으로 변경
            }

            // 상태 토글
            isPlaying = !isPlaying
        }


        //메트로놈 init
        initializeSound()
        val countTextView: TextView = findViewById(R.id.metronomeCountTextView)
        metronomeManager = MetronomeManager(soundPool, soundId, countTextView)

        syncManager = SyncManager(this)

        metronomeManager.setOnCountUpdateListener {
            syncManager.updateMetronomeCount()
            Log.d("MainActivity", "Metronome count updated")
        }



        //자이로센서
        // UI 요소 초기화
        stepCountText = findViewById(R.id.stepCountText)
        logText = findViewById(R.id.logText)

        // 자이로스코프는 기기의 x, y, z축을 중심으로 회전 속도를 rad/s 단위로 측정합니다.
        // 기본 자이로스코프의 인스턴스를 가져오는 방법을 나타냅니다.
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        //stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (gyroSensor == null) {
            Log.e("Gyroseneor", "자이로센서 사용 불가")
            logText.text = "자이로센서 사용 불가"
        } else {
            logText.text = "자이로센서 초기화 성공!"
        }

        stepCountText.text = "아직 측정되지 않았습니다. "

        findViewById<Button>(R.id.button).setOnClickListener {
            stepCount = 0
            stepCountText.text = "Steps: $stepCount" // 화면에 초기화된 값 반영
            logText.text = "걸음 수 초기화됨."
            Log.d("Reset", "Step count reset to 0")
        }



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

    override fun onResume() {
        super.onResume()

        // 센서 등록
        gyroSensor.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    // 얘네가 왜 있는가? 꼭 필요한 것인가? 이유를 찾자
    override fun onPause() {
        super.onPause()

        // 센서 해제
        sensorManager.unregisterListener(this)
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
                //startPlaybackWithMetronome(tempo)
                metronomeManager.startMetronome(tempo)

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



    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            val angularSpeedX = p0.values[0] // X축 회전 속도
            val angularSpeedY = p0.values[1] // Y축 회전 속도
            val angularSpeedZ = p0.values[2]; // Z축 회전 속도

            val currentTime = System.currentTimeMillis()

//            // 회전 속도를 기준으로 걸음 판단
//            if (Math.abs(angularSpeedZ) > 2.0) {
//                if (lastTimestamp == 0L || currentTime - lastTimestamp > 500) { // 500ms이상 간격
//                    stepCount++
//                    lastTimestamp = currentTime
//
//                    stepCountText.text = "Steps: $stepCount"
//                    logText.text = "걸음 감지 -> ${currentTime}ms"
//                }
//            }

            if (Math.abs(angularSpeedZ) > 4.0 &&
                Math.abs(angularSpeedX) < 2.0 &&
                Math.abs(angularSpeedY) < 2.0 ) {

                // 걸음 간격이 적절한지 확인 (0.5초 이상)
                if (lastTimestamp == 0L || currentTime - lastTimestamp > 350) {
                    stepCount++
                    lastTimestamp = currentTime

                    //케이던스 피드백
                    syncManager.updateGyroSensorCount()

                    // UI 업데이트
                    stepCountText.text = "Steps: $stepCount"
                    logText.text = "걸음 감지 -> $currentTime ms"
                }
            }
        }


//        if (p0?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
//            val totalSteps = p0.values[0]
//
//            // 초기 값을 설정하여 보정
//            if (initialStepCount == -1f) {
//                initialStepCount = totalSteps
//            }
//
//            // 현재 걸음 수 계산
//            currentSteps = (totalSteps - initialStepCount).toInt()
//
//            // UI 업데이트
//            stepCountText.text = "Steps: 테스트입니다"
//            logText.text = "걸음 수 업데이트: $currentSteps"
//        }

    }
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d("Sensor", "Accuracy changed")
    }
}
