import android.media.SoundPool
import kotlinx.coroutines.*

class MetronomeManager(private val soundPool: SoundPool, private val soundId: Int) {

    private var metronomeJob: Job? = null
    private var currentBpm = 0.0

    fun startMetronome(bpm: Double) {
        stopMetronome() // 기존 메트로놈 정지
        currentBpm = bpm
        val interval = (60_000 / bpm).toLong() // BPM -> ms 단위

        metronomeJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
                delay(interval)

            }
        }
    }

    fun stopMetronome() {
        metronomeJob?.cancel()
        metronomeJob = null
    }

    fun changeBpm(newBpm: Double) {
        startMetronome(newBpm)
    }
}
