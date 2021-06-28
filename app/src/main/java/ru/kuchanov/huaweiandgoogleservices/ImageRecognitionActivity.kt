package ru.kuchanov.huaweiandgoogleservices

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hmf.tasks.Task
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.MLException
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.document.MLDocument
import com.huawei.hms.mlsdk.document.MLDocumentAnalyzer
import com.huawei.hms.mlsdk.document.MLDocumentSetting
import com.huawei.hms.mlsdk.text.MLLocalTextSetting
import com.huawei.hms.mlsdk.text.MLRemoteTextSetting
import com.huawei.hms.mlsdk.text.MLText
import com.huawei.hms.mlsdk.text.MLTextAnalyzer
import timber.log.Timber


class ImageRecognitionActivity : AppCompatActivity() {

    private lateinit var startTextRecognisingButton: View
    private lateinit var startDocumentRecognisingButton: View
    private lateinit var resultsTextView: TextView

    private lateinit var textAnalyzer: MLTextAnalyzer
    private lateinit var documentAnalyzer: MLDocumentAnalyzer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_recognition)

        startTextRecognisingButton = findViewById(R.id.textRecognising)
        startDocumentRecognisingButton = findViewById(R.id.documentRecognising)
        resultsTextView = findViewById(R.id.resultsTextView)

        initTextRecognizing()
        initDocumentRecognizing()

        startTextRecognisingButton.setOnClickListener {
            Timber.d("startRecognisingButton clicked!")
            resultsTextView.text = null

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.russian_text_image)
            val frame = MLFrame.fromBitmap(bitmap)

            val task: Task<MLText> = textAnalyzer.asyncAnalyseFrame(frame)
            task.addOnSuccessListener {
                // Processing for successful recognition.
                resultsTextView.text = it.stringValue
            }.addOnFailureListener {
                // Processing logic for recognition failure.
                Timber.e(it, "Error while text recognizing")
                resultsTextView.text = it.message
            }
        }

        startDocumentRecognisingButton.setOnClickListener {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.russian_text_image)
            val frame = MLFrame.fromBitmap(bitmap)

            val task: Task<MLDocument> = documentAnalyzer.asyncAnalyseFrame(frame)
            task.addOnSuccessListener {
                // Recognition success.
                resultsTextView.text = it.stringValue
            }.addOnFailureListener { e ->
                // Recognition failure.
                try {
                    val mlException = e as MLException
                    // Obtain the result code. You can process the result code and customize respective messages displayed to users.
                    val errorCode = mlException.errCode
                    // Obtain the error information. You can quickly locate the fault based on the result code.
                    val errorMessage = mlException.message
                    Timber.e("Error while document recognizing: $errorCode, $errorMessage")
                } catch (error: Exception) {
                    // Handle the conversion error.
                    Timber.e(error, "Error while document recognizing")
                    resultsTextView.text = error.message
                }
            }
        }
    }

    private fun initTextRecognizing() {
        val setting: MLLocalTextSetting = MLLocalTextSetting.Factory()
            .setOCRMode(MLLocalTextSetting.OCR_DETECT_MODE)
            // Specify languages that can be recognized.
            .setLanguage("ru")
            .create()
        textAnalyzer = MLAnalyzerFactory.getInstance().getLocalTextAnalyzer(setting)
    }

    private fun initDocumentRecognizing() {
        val languageList: MutableList<String> = ArrayList()
        languageList.add("ru")
        languageList.add("en")
        // Set parameters.
        val setting = MLDocumentSetting.Factory()
            // Specify the languages that can be recognized, which should comply with ISO 639-1.
            .setLanguageList(languageList)
            // Set the format of the returned text border box.
            // MLRemoteTextSetting.NGON: Return the coordinates of the four corner points of the quadrilateral.
            // MLRemoteTextSetting.ARC: Return the corner points of a polygon border in an arc. The coordinates of up to 72 corner points can be returned.
            .setBorderType(MLRemoteTextSetting.ARC)
            .create()
        documentAnalyzer = MLAnalyzerFactory.getInstance().getRemoteDocumentAnalyzer(setting)
    }
}