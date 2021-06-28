package ru.kuchanov.huaweiandgoogleservices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.huawei.hms.mlsdk.aft.cloud.MLRemoteAftEngine
import com.huawei.hms.mlsdk.asr.MLAsrConstants
import com.huawei.hms.mlsdk.asr.MLAsrListener
import com.huawei.hms.mlsdk.asr.MLAsrRecognizer
import com.huawei.hms.mlsdk.common.MLApplication
import com.huawei.hms.mlsdk.speechrtt.MLSpeechRealTimeTranscription
import com.huawei.hms.mlsdk.speechrtt.MLSpeechRealTimeTranscriptionConfig
import com.huawei.hms.mlsdk.speechrtt.MLSpeechRealTimeTranscriptionConstants
import com.huawei.hms.mlsdk.speechrtt.MLSpeechRealTimeTranscriptionListener
import com.huawei.hms.mlsdk.tts.*
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    companion object {
        const val RECORD_AUDIO_PERMISSION_CODE = 4242
    }

    private lateinit var speechRecognizer: MLAsrRecognizer
    private lateinit var mlTtsEngine: MLTtsEngine
    private lateinit var config: MLSpeechRealTimeTranscriptionConfig
    private lateinit var mlSpeechRealTimeTranscription: MLSpeechRealTimeTranscription

    private lateinit var startRecognisingButton: View
    private lateinit var startRecognisingRealTime: View
    private lateinit var asrShowSupportedLanguagesButton: View
    private lateinit var speechRealTimeTranscriptionShowSupportedLanguagesAsr: View
    private lateinit var audioFileShowSupportedLanguages: View
    private lateinit var resultsTextView: TextView
    private lateinit var startTtsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.plant(Timber.DebugTree())

        startRecognisingButton = findViewById(R.id.textRecognising)
        startRecognisingRealTime = findViewById(R.id.textRecognisingRealTime)
        asrShowSupportedLanguagesButton = findViewById(R.id.showSupportedLanguagesAsr)
        speechRealTimeTranscriptionShowSupportedLanguagesAsr =
            findViewById(R.id.speechRealTimeTranscriptionShowSupportedLanguagesAsr)
        audioFileShowSupportedLanguages =
            findViewById(R.id.audioFileShowSupportedLanguages)
        resultsTextView = findViewById(R.id.resultsTextView)
        startTtsTextView = findViewById(R.id.startTtsTextView)

        checkPermissions()

        initSpeechRecognitionService()
        initTtsService()
        initRealTimeTranscription()

        startRecognisingButton.setOnClickListener {
            Timber.d("Clicked!")
            resultsTextView.text = null

            // Create an Intent to set parameters.
            val mSpeechRecognizerIntent = Intent(MLAsrConstants.ACTION_HMS_ASR_SPEECH)
            // Use Intent for recognition parameter settings.
            mSpeechRecognizerIntent // Set the language that can be recognized to English. If this parameter is not set, English is recognized by default. Example: "zh-CN": Chinese; "en-US": English; "fr-FR": French; "es-ES": Spanish; "de-DE": German; "it-IT": Italian; "ar": Arabic.
//                .putExtra(MLAsrConstants.LANGUAGE, "en-US")
                .putExtra(MLAsrConstants.LANGUAGE, "ru-RU")
                // Set to return the recognition result along with the speech. If you ignore the setting, this mode is used by default. Options are as follows:
                // MLAsrConstants.FEATURE_WORDFLUX: Recognizes and returns texts through onRecognizingResults.
                // MLAsrConstants.FEATURE_ALLINONE: After the recognition is complete, texts are returned through onResults.
                .putExtra(MLAsrConstants.FEATURE, MLAsrConstants.FEATURE_WORDFLUX)
                // Set the application scenario. MLAsrConstants.SCENES_SHOPPING indicates shopping, which is supported only for Chinese. Under this scenario, recognition for the name of Huawei products has been optimized.
                .putExtra(MLAsrConstants.SCENES, MLAsrConstants.SCENES_SHOPPING)
            // Start speech recognition.
            speechRecognizer.startRecognizing(mSpeechRecognizerIntent)
        }

        startRecognisingRealTime.setOnClickListener {
            mlSpeechRealTimeTranscription.startRecognizing(config);
        }

        asrShowSupportedLanguagesButton.setOnClickListener {
            MLAsrRecognizer.createAsrRecognizer(this)
                .getLanguages(object : MLAsrRecognizer.LanguageCallback {
                    override fun onResult(result: List<String>) {
                        Timber.i("support languages==$result")
                        resultsTextView.text = result.toString()
                    }

                    override fun onError(errorCode: Int, errorMsg: String) {
                        Timber.e("errorCode: ${errorCode}, errorMsg: $errorMsg")
                    }
                })
        }

        speechRealTimeTranscriptionShowSupportedLanguagesAsr.setOnClickListener {
            MLSpeechRealTimeTranscription.getInstance()
                .getLanguages(object : MLSpeechRealTimeTranscription.LanguageCallback {
                    override fun onResult(result: List<String>) {
                        Timber.i("support languages==$result")
                        resultsTextView.text = result.toString()
                    }

                    override fun onError(errorCode: Int, errorMsg: String) {
                        Timber.e("errorCode: ${errorCode}, errorMsg: $errorMsg")
                    }
                })
        }

        audioFileShowSupportedLanguages.setOnClickListener {
            MLRemoteAftEngine.getInstance()
                .getShortAftLanguages(object : MLRemoteAftEngine.LanguageCallback {
                    override fun onResult(result: List<String>) {
                        Timber.i("support languages==$result")
                        resultsTextView.text = result.toString()
                    }

                    override fun onError(errorCode: Int, errorMsg: String) {
                        Timber.e("errorCode: ${errorCode}, errorMsg: $errorMsg")
                    }
                })
        }

        startTtsTextView.setOnClickListener {
            Timber.d("startTtsTextView clicked")
            // Use the built-in player of the SDK to play speech in queuing mode.
            val id: String =
                mlTtsEngine.speak(resultsTextView.text.toString(), MLTtsEngine.QUEUE_APPEND)
            // In queuing mode, the synthesized audio stream is output through onAudioAvailable, and the built-in player of the SDK is used to play the speech.
            // String id = mlTtsEngine.speak(sourceText, MLTtsEngine.QUEUE_APPEND | MLTtsEngine.OPEN_STREAM);
            // In queuing mode, the synthesized audio stream is output through onAudioAvailable, and the audio stream is not played, but controlled by you.
            // String id = mlTtsEngine.speak(sourceText, MLTtsEngine.QUEUE_APPEND | MLTtsEngine.OPEN_STREAM | MLTtsEngine.EXTERNAL_PLAYBACK);
        }

        findViewById<View>(R.id.imageRecognition).setOnClickListener {
            startActivity(Intent(this@MainActivity, ImageRecognitionActivity::class.java))
        }
        findViewById<View>(R.id.formRecognition).setOnClickListener {
            startActivity(Intent(this@MainActivity, FormRecognitionActivity::class.java))
        }
        findViewById<View>(R.id.translation).setOnClickListener {
            startActivity(Intent(this@MainActivity, TranslationActivity::class.java))
        }
    }

    //todo test
    private fun initRealTimeTranscription() {
        config = MLSpeechRealTimeTranscriptionConfig.Factory()
            // Set languages. Currently, Mandarin Chinese, English, and French are supported.
            .setLanguage(MLSpeechRealTimeTranscriptionConstants.LAN_EN_US)
            // Set punctuation.
            .enablePunctuation(true)
            // Set the sentence offset.
            .enableSentenceTimeOffset(true)
            // Set the word offset.
            .enableWordTimeOffset(true)
            // Set the application scenario. MLSpeechRealTimeTranscriptionConstants.SCENES_SHOPPING indicates shopping, which is supported only for Chinese. Under this scenario, recognition for the name of Huawei products has been optimized.
            .setScenes(MLSpeechRealTimeTranscriptionConstants.SCENES_SHOPPING)
            .create()
        mlSpeechRealTimeTranscription = MLSpeechRealTimeTranscription.getInstance()

        mlSpeechRealTimeTranscription.setRealTimeTranscriptionListener(object :
            MLSpeechRealTimeTranscriptionListener {
            override fun onStartListening() {
                Timber.d("onStartListening")
                // The recorder starts to receive speech.
            }

            override fun onStartingOfSpeech() {
                Timber.d("onStartingOfSpeech")
                // The user starts to speak, that is, the speech recognizer detects that the user starts to speak.
            }

            override fun onVoiceDataReceived(data: ByteArray?, energy: Float, bundle: Bundle?) {
                Timber.d("onVoiceDataReceived")
                // Return the original PCM stream and audio power to the user. This API is not running in the main thread, and the return result is processed in the sub-thread.
            }

            override fun onRecognizingResults(partialResults: Bundle?) {
                // Receive the recognized text from MLSpeechRealTimeTranscription.
                Timber.d("onRecognizingResults: $partialResults")
            }

            override fun onError(error: Int, errorMessage: String?) {
                // Called when an error occurs in recognition.
                Timber.e("onError: $error, $errorMessage")
            }

            override fun onState(state: Int, params: Bundle?) {
                Timber.d("onState")
                // Notify the app status change.
            }
        })
    }

    private fun initTtsService() {
        val TTS_RU = "ru-RU"
        // Use customized parameter settings to create a TTS engine.
        val mlTtsConfig: MLTtsConfig =
            MLTtsConfig()
                .setLanguage(TTS_RU)
                // Set the Chinese timbre.
//                .setPerson(MLTtsConstants.TTS_SPEAKER_MALE_EN)
                // Set the speech speed. The range is (0,5.0]. 1.0 indicates a normal speed.
                .setSpeed(1.0f)
                // Set the volume. The range is (0,2). 1.0 indicates a normal volume.
                .setVolume(1.0f)

        mlTtsEngine = MLTtsEngine(mlTtsConfig)
        // Set the volume of the built-in player, in dBs. The value is in the range of [0, 100].
        mlTtsEngine.setPlayerVolume(20)
        // Update the configuration when the engine is running.
        mlTtsEngine.updateConfig(mlTtsConfig)

        val callback: MLTtsCallback = object : MLTtsCallback {
            override fun onError(taskId: String, err: MLTtsError) {
                val message = "onError: $taskId, ${err.errorId}, ${err.errorMsg}"
                Timber.e(message)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
                // Processing logic for TTS failure.
            }

            override fun onWarn(taskId: String, warn: MLTtsWarn) {
                // Alarm handling without affecting service logic.
            }

            // Return the mapping between the currently played segment and text. start: start position of the audio segment in the input text; end (excluded): end position of the audio segment in the input text.
            override fun onRangeStart(taskId: String, start: Int, end: Int) {
                // Process the mapping between the currently played segment and text.
            }

            // taskId: ID of a TTS task corresponding to the audio.
            // audioFragment: audio data.
            // offset: offset of the audio segment to be transmitted in the queue. One TTS task corresponds to a TTS queue.
            // range: text area where the audio segment to be transmitted is located; range.first (included): start position; range.second (excluded): end position.
            fun onAudioAvailable(
                taskId: String?,
                audioFragment: MLTtsAudioFragment?,
                offset: Int,
                range: Pair<Int?, Int?>?,
                bundle: Bundle?
            ) {
                // Audio stream callback API, which is used to return the synthesized audio data to the app.
            }

            override fun onEvent(taskId: String, eventId: Int, bundle: Bundle?) {
                Timber.d("onEvent: $taskId, $eventId, $bundle")
                // Callback method of a TTS event. eventId indicates the event name.
                when (eventId) {
                    MLTtsConstants.EVENT_PLAY_START -> {
                    }
                    MLTtsConstants.EVENT_PLAY_STOP -> {
                        var isInterrupted =
                            bundle?.getBoolean(MLTtsConstants.EVENT_PLAY_STOP_INTERRUPTED)
                    }
                    MLTtsConstants.EVENT_PLAY_RESUME -> {
                    }
                    MLTtsConstants.EVENT_PLAY_PAUSE -> {
                    }
                    MLTtsConstants.EVENT_SYNTHESIS_START -> {
                    }
                    MLTtsConstants.EVENT_SYNTHESIS_END -> {
                    }
                    // TTS is complete. All synthesized audio streams are passed to the app.
                    MLTtsConstants.EVENT_SYNTHESIS_COMPLETE -> {
                        var isInterrupted =
                            bundle?.getBoolean(MLTtsConstants.EVENT_SYNTHESIS_INTERRUPTED)
                    }
                    else -> {
                    }
                }
            }

            override fun onAudioAvailable(
                p0: String?,
                p1: MLTtsAudioFragment?,
                p2: Int,
                p3: android.util.Pair<Int, Int>?,
                p4: Bundle?
            ) {

            }
        }

        mlTtsEngine.setTtsCallback(callback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != RECORD_AUDIO_PERMISSION_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (grantResults.isEmpty().not() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // The camera permission is granted.
            startRecognisingButton.isEnabled = true
            asrShowSupportedLanguagesButton.isEnabled = true
        }
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // The app has the camera permission.
//            ...
            startRecognisingButton.isEnabled = true
            asrShowSupportedLanguagesButton.isEnabled = true
        } else {
            startRecognisingButton.isEnabled = false
            asrShowSupportedLanguagesButton.isEnabled = false

            val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                ActivityCompat.requestPermissions(this, permissions, RECORD_AUDIO_PERMISSION_CODE)
                return
            }
        }
    }

    private fun initSpeechRecognitionService() {
        MLApplication.getInstance().apiKey =
            "CgB6e3x9yroP8ecetjfm4f5Pu6QmVhVR6Q9eugtnkHnonZOdsicaNh1evPoA42MPmt3Zf7jDDLbNeM3vjVW3NSlL";
        speechRecognizer = MLAsrRecognizer.createAsrRecognizer(this)
        speechRecognizer.setAsrListener(object : MLAsrListener {

            override fun onResults(results: Bundle?) {
                Timber.d("onResults: $results")
                resultsTextView.text = results?.getString("results_recognized", null)
            }

            override fun onRecognizingResults(partialResults: Bundle?) {
                Timber.d("onRecognizingResults: $partialResults")
            }

            override fun onError(error: Int, message: String?) {
                Timber.d("onError: $error, $message")
            }

            override fun onStartListening() {
                Timber.d("onStartListening")
            }

            override fun onStartingOfSpeech() {
                Timber.d("onStartingOfSpeech")
            }

            override fun onVoiceDataReceived(data: ByteArray?, energy: Float, bundle: Bundle?) {
                Timber.d("onVoiceDataReceived: $data, $energy, $bundle")
            }

            override fun onState(state: Int, params: Bundle?) {
                Timber.d("onState: $state, $params")
            }
        })
    }
}