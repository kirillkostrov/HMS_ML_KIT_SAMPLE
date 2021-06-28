package ru.kuchanov.huaweiandgoogleservices

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hmf.tasks.Task
import com.huawei.hms.mlsdk.asr.MLAsrRecognizer
import com.huawei.hms.mlsdk.common.MLException
import com.huawei.hms.mlsdk.langdetect.MLDetectedLang
import com.huawei.hms.mlsdk.langdetect.MLLangDetectorFactory
import com.huawei.hms.mlsdk.langdetect.cloud.MLRemoteLangDetector
import com.huawei.hms.mlsdk.translate.MLTranslateLanguage
import com.huawei.hms.mlsdk.translate.MLTranslatorFactory
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslateSetting
import com.huawei.hms.mlsdk.translate.cloud.MLRemoteTranslator
import timber.log.Timber


class TranslationActivity : AppCompatActivity() {

    private lateinit var mlRemoteTranslator: MLRemoteTranslator
    private lateinit var mlRemoteLangDetector: MLRemoteLangDetector

    private lateinit var startRecognisingButton: View
    private lateinit var showSupportedLanguagesButton: View
    private lateinit var resultsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translation)

        startRecognisingButton = findViewById(R.id.textRecognising)
        showSupportedLanguagesButton = findViewById(R.id.showSupportedLanguagesAsr)
        resultsTextView = findViewById(R.id.resultsTextView)

        // sourceText: text to be translated, with up to 5000 characters.
        val sourceText = """
                The Battle of Cherbourg was part of the Battle of Normandy during World War II.
                 It was fought immediately after the successful Allied landings on 6 June 1944. 
                 Allied troops, mainly American, isolated and captured the fortified port, 
                 which was considered vital to the campaign in Western Europe, 
                 in a hard-fought, month-long campaign.
            """.trimIndent()

        findViewById<TextView>(R.id.originalText).text = sourceText

        findViewById<TextView>(R.id.showLanguage).setOnClickListener {
            val text =resultsTextView.text.toString()
            resultsTextView.text = null
            // Method 1: Return multiple language detection results, including the language codes and confidences. sourceText indicates the text to be detected, with up to 5000 characters.
            val probabilityDetectTask: Task<List<MLDetectedLang>> =
                mlRemoteLangDetector.probabilityDetect(text)
            probabilityDetectTask
                .addOnSuccessListener {
                    // Processing logic for detection success.
                    resultsTextView.text = it.joinToString(separator = ",")
                }.addOnFailureListener { e ->
                    // Processing logic for detection failure.
                    // Recognition failure.
                    try {
                        val mlException = e as MLException
                        // Obtain the result code. You can process the result code and customize respective messages displayed to users.
                        val errorCode = mlException.errCode
                        // Obtain the error information. You can quickly locate the fault based on the result code.
                        val errorMessage = mlException.message
                        Timber.e("Error while lang detect: $errorCode, $errorMessage")
                    } catch (error: java.lang.Exception) {
                        // Handle the conversion error.
                        Timber.e(error, "Error while lang detect")
                    }
                }
        }

        initTranslationService()
        initLangDetect()

        startRecognisingButton.setOnClickListener {
            Timber.d("Clicked!")
            resultsTextView.text = null

            val task: Task<String> = mlRemoteTranslator.asyncTranslate(sourceText)
            task.addOnSuccessListener {
                // Processing logic for recognition success.
                resultsTextView.text = it
            }.addOnFailureListener { e ->
                // Processing logic for recognition failure.
                try {
                    val mlException = e as MLException
                    // Obtain the result code. You can process the result code and customize respective messages displayed to users.
                    val errorCode = mlException.errCode
                    // Obtain the error information. You can quickly locate the fault based on the result code.
                    val errorMessage = mlException.message
                    Timber.e("Error while translate: $errorCode, $errorMessage")
                } catch (error: Exception) {
                    // Handle the conversion error.
                    Timber.e(error, "Error while translate")
                }
            }
        }

        showSupportedLanguagesButton.setOnClickListener {
            MLTranslateLanguage.getCloudAllLanguages()
                .addOnSuccessListener {
                    Timber.i("support languages==$it")
                    resultsTextView.text = it.toString()
                }
                .addOnFailureListener {
                    Timber.e(it, "Error while get supported languages")
                }


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
    }

    private fun initLangDetect() {
        mlRemoteLangDetector = MLLangDetectorFactory.getInstance()
            .getRemoteLangDetector()
    }

    private fun initTranslationService() {
        // Create a text translator using custom parameter settings.
        val setting: MLRemoteTranslateSetting =
            MLRemoteTranslateSetting.Factory()
                // Set the source language code. The BCP-47 standard is used for Traditional Chinese, and the ISO 639-1 standard is used for other languages. This parameter is optional. If this parameter is not set, the system automatically detects the language.
                .setSourceLangCode("en")
                // Set the target language code. The BCP-47 standard is used for Traditional Chinese, and the ISO 639-1 standard is used for other languages.
                .setTargetLangCode("ru")
                .create()
        mlRemoteTranslator =
            MLTranslatorFactory.getInstance().getRemoteTranslator(setting)
    }
}