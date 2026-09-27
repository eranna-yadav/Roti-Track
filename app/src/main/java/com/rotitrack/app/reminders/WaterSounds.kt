package com.rotitrack.app.reminders

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.rotitrack.app.R
import com.rotitrack.app.data.WaterSound

/** Plays the water sounds (res/raw) and the reminder buzz, honouring the phone's silent mode. */
object WaterSounds {
    private var player: MediaPlayer? = null

    fun res(sound: WaterSound): Int = when (sound) {
        WaterSound.DROP_1 -> R.raw.water_drop_1
        WaterSound.DROP_2 -> R.raw.water_drop_2
        WaterSound.FLOWING_1 -> R.raw.water_flowing_1
        WaterSound.FLOWING_2 -> R.raw.water_flowing_2
        WaterSound.FLOWING_3 -> R.raw.water_flowing_3
    }

    /** Starts [sound] and returns straight away; any sound already playing stops. [onDone] runs when it ends. */
    fun play(context: Context, sound: WaterSound, volume: Float, onDone: () -> Unit = {}) {
        stop()
        if (ringerMode(context) != AudioManager.RINGER_MODE_NORMAL) {
            onDone()
            return
        }
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val mp = MediaPlayer.create(context.applicationContext, res(sound), attrs, audioSession(context)) ?: run {
            onDone()
            return
        }
        val v = volume.coerceIn(0f, 1f)
        mp.setVolume(v, v)
        mp.setOnCompletionListener {
            it.release()
            if (player === it) player = null
            onDone()
        }
        player = mp
        mp.start()
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }

    fun vibrate(context: Context) {
        if (ringerMode(context) == AudioManager.RINGER_MODE_SILENT) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        } ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 180), -1))
    }

    private fun ringerMode(context: Context): Int =
        context.getSystemService(AudioManager::class.java)?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

    private fun audioSession(context: Context): Int =
        context.getSystemService(AudioManager::class.java)?.generateAudioSessionId() ?: 0
}
