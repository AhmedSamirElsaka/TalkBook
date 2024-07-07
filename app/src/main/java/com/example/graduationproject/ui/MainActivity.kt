package com.example.graduationproject.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import com.example.graduationproject.R
import com.example.graduationproject.databinding.ActivityMainBinding
import com.example.graduationproject.utilities.saveTo
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity @Inject constructor() : AppCompatActivity() {
     lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)




        binding.onlinePdf.setOnClickListener {

            startActivity(
                Intent(baseContext, PdfFromInternetActivity::class.java)
            )
        }

        binding.pickPdfButton.setOnClickListener {
            launchFilePicker()
        }

        binding.fromAssets.setOnClickListener {
            launchPdfFromAssets("quote.pdf")
        }
    }
    private val filePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedFileUri = result.data?.data
                selectedFileUri?.let { uri ->
                    launchPdfFromUri(uri)
                }
            }
        }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if ((cursor != null) && cursor.moveToFirst()
                ) {
                    result =
                        cursor.getString(cursor.run { getColumnIndex(OpenableColumns.DISPLAY_NAME) })
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result.substring(0, result.length - 4);
    }


    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        filePicker.launch(intent)
    }

    private fun launchPdfFromUri(uri: Uri) {
        var pdfTitle = getFileName(uri!!)

        startActivity(
            PdfViewerActivity.launchPdfFromPath(
                context = this,
                path = uri.toString(),
                pdfTitle = pdfTitle,
                saveTo = saveTo.ASK_EVERYTIME,
                fromAssets = false
            )
        )
    }

    private fun launchPdfFromAssets(uri: String) {
        startActivity(
            PdfViewerActivity.launchPdfFromPath(
                context = this,
                path = uri,
                pdfTitle = "Test Pdf From Assets",
                saveTo = saveTo.ASK_EVERYTIME,
                fromAssets = true
            )
        )
    }
}