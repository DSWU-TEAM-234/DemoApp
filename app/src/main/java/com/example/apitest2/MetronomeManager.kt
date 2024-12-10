import android.media.SoundPool
import android.widget.TextView
import com.example.apitest2.R
import kotlinx.coroutines.*


class MetronomeManager(private val soundPool: SoundPool, private val soundId: Int,  private val countTextView: TextView) {

    private var metronomeJob: Job? = null
    private var currentBpm = 0.0
    private var count = 0
    private var onCountUpdateListener: (() -> Unit)? = null

    fun setOnCountUpdateListener(listener: () -> Unit) {
        onCountUpdateListener = listener
    }

    fun startMetronome(bpm: Double) {
        stopMetronome() // 기존 메트로놈 정지
        currentBpm = bpm
        val interval = (60_000 / bpm).toLong() // BPM -> ms 단위

        metronomeJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
                count++
                onCountUpdateListener?.invoke()
                countTextView.text = "Count: $count"
                delay(interval)

            }
        }
    }

    fun stopMetronome() {
        metronomeJob?.cancel()
        metronomeJob = null
    }

    fun resumeMetronome() {
        startMetronome(currentBpm)
    }

    fun changeBpm(newBpm: Double) {
        startMetronome(newBpm)
    }
}
