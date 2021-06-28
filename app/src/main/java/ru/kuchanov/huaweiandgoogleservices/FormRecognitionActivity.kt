package ru.kuchanov.huaweiandgoogleservices

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonObject
import com.huawei.hmf.tasks.Task
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.fr.MLFormRecognitionAnalyzer
import com.huawei.hms.mlsdk.fr.MLFormRecognitionAnalyzerFactory
import com.huawei.hms.mlsdk.fr.MLFormRecognitionAnalyzerSetting
import timber.log.Timber


class FormRecognitionActivity : AppCompatActivity() {

    private lateinit var startTextRecognisingButton: View
    private lateinit var resultsTextView: TextView

    private lateinit var analyzer: MLFormRecognitionAnalyzer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_recognition)

        startTextRecognisingButton = findViewById(R.id.textRecognising)
        resultsTextView = findViewById(R.id.resultsTextView)

        initFormRecognizing()

        startTextRecognisingButton.setOnClickListener {
            Timber.d("startRecognisingButton clicked!")
            resultsTextView.text = null

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.russian_table_image)
            val frame = MLFrame.fromBitmap(bitmap)

            // Call the asynchronous method asyncAnalyseFrame.
            val recognizeTask: Task<JsonObject> = analyzer.asyncAnalyseFrame(frame)
            recognizeTask.addOnSuccessListener {
                // Recognition success.
                resultsTextView.text = it.toString()
            }.addOnFailureListener {
                // Recognition failure.
                Timber.e(it, "Error while document recognizing")
                resultsTextView.text = it.message
            }
        }
    }

    private fun initFormRecognizing() {
        val setting: MLFormRecognitionAnalyzerSetting = MLFormRecognitionAnalyzerSetting.Factory()
            .create()
        analyzer =
            MLFormRecognitionAnalyzerFactory.getInstance().getFormRecognitionAnalyzer(setting)
    }
}