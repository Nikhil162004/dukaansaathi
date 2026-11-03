package com.dukaansaathi.app.util

import android.app.Activity
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.dukaansaathi.app.R

/** Base fragment that can fire a speech-recognition intent and deliver the transcript to a callback. */
abstract class VoiceCapableFragment : Fragment() {

    private var pendingCallback: ((String) -> Unit)? = null

    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) {
                pendingCallback?.invoke(text.trim())
            }
        }
        pendingCallback = null
    }

    fun askVoice(prompt: String, onResult: (String) -> Unit) {
        if (!VoiceHelper.isAvailable(requireContext())) {
            Toast.makeText(requireContext(), R.string.mic_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        pendingCallback = onResult
        voiceLauncher.launch(VoiceHelper.intent(prompt))
    }
}
