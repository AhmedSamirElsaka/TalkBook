package com.example.graduationproject.ui

import android.Manifest.permission
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.graduationproject.R
import com.example.graduationproject.databinding.ActivityPdfViewerBinding
import com.example.graduationproject.utilities.FileUtils.createPdfDocumentUri
import com.example.graduationproject.utilities.FileUtils.fileFromAsset
import com.example.graduationproject.utilities.FileUtils.uriToFile
import com.example.graduationproject.utilities.NetworkUtil.checkInternetConnection
import com.example.graduationproject.utilities.PdfEngine
import com.example.graduationproject.utilities.saveTo
import java.io.File


class PdfViewerActivity : AppCompatActivity() {

    private lateinit var file_not_downloaded_yet: String
    private lateinit var file_saved_to_downloads: String
    private lateinit var file_saved_successfully: String
    private lateinit var error_no_internet_connection: String
    private lateinit var permission_required: String
    private lateinit var permission_required_title: String
    private lateinit var error_pdf_corrupted: String
    private lateinit var pdf_viewer_retry: String
    private lateinit var pdf_viewer_grant: String
    private lateinit var pdf_viewer_cancel: String
    private lateinit var pdf_viewer_error: String
    private var fileUrl: String? = null
    private lateinit var headers: HeaderData
    private lateinit var binding: ActivityPdfViewerBinding
    private var downloadedFilePath: String? = null


    companion object {
        const val FILE_URL = "pdf_file_url"
        const val FILE_TITLE = "pdf_file_title"
        const val ENABLE_FILE_DOWNLOAD = "enable_download"
        const val FROM_ASSETS = "from_assets"
        var engine = PdfEngine.INTERNAL
        var enableDownload = false
        var isPDFFromPath = false
        var isFromAssets = false
        var SAVE_TO_DOWNLOADS = true

        fun launchPdfFromUrl(
            context: Context?,
            pdfUrl: String?,
            pdfTitle: String?,
            saveTo: saveTo,
            enableDownload: Boolean = true,
            headers: Map<String, String> = emptyMap()
        ): Intent {
            val intent = Intent(context, PdfViewerActivity::class.java)
            intent.putExtra(FILE_URL, pdfUrl)
            intent.putExtra(FILE_TITLE, pdfTitle)
            intent.putExtra(ENABLE_FILE_DOWNLOAD, enableDownload)
            intent.putExtra("headers", HeaderData(headers))
            isPDFFromPath = false
            SAVE_TO_DOWNLOADS = saveTo == com.example.graduationproject.utilities.saveTo.DOWNLOADS
            return intent
        }

        fun launchPdfFromPath(
            context: Context?,
            path: String?,
            pdfTitle: String?,
            saveTo: saveTo,
            fromAssets: Boolean = false
        ): Intent {
            val intent = Intent(context, PdfViewerActivity::class.java)
            intent.putExtra(FILE_URL, path)
            intent.putExtra(FILE_TITLE, pdfTitle)
            intent.putExtra(ENABLE_FILE_DOWNLOAD, false)
            intent.putExtra(FROM_ASSETS, fromAssets)
            isPDFFromPath = true
            SAVE_TO_DOWNLOADS = saveTo == com.example.graduationproject.utilities.saveTo.DOWNLOADS

            return intent
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pdfTitleTv.text = intent.extras!!.getString(
            FILE_TITLE,
            "Graduation Project",
        )


        // Configure progress bar and background
        val typedArray1 = theme.obtainStyledAttributes(R.styleable.PdfRendererView)
        try {
            val backgroundColor = typedArray1.getColor(
                R.styleable.PdfRendererView_pdfView_backgroundColor,
                ContextCompat.getColor(applicationContext, android.R.color.white)
            )
            binding.parentLayout.setBackgroundColor(backgroundColor)

            val progressBarStyleResId = typedArray1.getResourceId(
                R.styleable.PdfRendererView_pdfView_progressBar, -1
            )
            if (progressBarStyleResId != -1) {
                val progressBarStyle = ContextCompat.getDrawable(this, progressBarStyleResId)
                binding.progressBar.indeterminateDrawable = progressBarStyle
            }
        } finally {
            typedArray1.recycle()
        }

        enableDownload = intent.extras!!.getBoolean(
            ENABLE_FILE_DOWNLOAD,
            false
        )

        val headerData: HeaderData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("headers", HeaderData::class.java)
        } else {
            intent.getParcelableExtra("headers")
        }
        headerData?.let {
            headers = it
        }

        isFromAssets = intent.extras!!.getBoolean(
            FROM_ASSETS,
            false
        )

        engine = PdfEngine.INTERNAL

        val typedArray = obtainStyledAttributes(R.styleable.PdfRendererView_Strings)
        error_pdf_corrupted =
            typedArray.getString(R.styleable.PdfRendererView_Strings_error_pdf_corrupted)
                ?: getString(R.string.error_pdf_corrupted)
        error_no_internet_connection =
            typedArray.getString(R.styleable.PdfRendererView_Strings_error_no_internet_connection)
                ?: getString(R.string.error_no_internet_connection)
        file_saved_successfully =
            typedArray.getString(R.styleable.PdfRendererView_Strings_file_saved_successfully)
                ?: getString(R.string.file_saved_successfully)
        file_saved_to_downloads =
            typedArray.getString(R.styleable.PdfRendererView_Strings_file_saved_to_downloads)
                ?: getString(R.string.file_saved_to_downloads)
        file_not_downloaded_yet =
            typedArray.getString(R.styleable.PdfRendererView_Strings_file_not_downloaded_yet)
                ?: getString(R.string.file_not_downloaded_yet)
        permission_required =
            typedArray.getString(R.styleable.PdfRendererView_Strings_permission_required)
                ?: getString(R.string.permission_required)
        permission_required_title =
            typedArray.getString(R.styleable.PdfRendererView_Strings_permission_required_title)
                ?: getString(R.string.permission_required_title)
        pdf_viewer_error =
            typedArray.getString(R.styleable.PdfRendererView_Strings_pdf_viewer_error)
                ?: getString(R.string.pdf_viewer_error)
        pdf_viewer_retry =
            typedArray.getString(R.styleable.PdfRendererView_Strings_pdf_viewer_retry)
                ?: getString(R.string.pdf_viewer_retry)
        pdf_viewer_cancel =
            typedArray.getString(R.styleable.PdfRendererView_Strings_pdf_viewer_cancel)
                ?: getString(R.string.pdf_viewer_cancel)
        pdf_viewer_grant =
            typedArray.getString(R.styleable.PdfRendererView_Strings_pdf_viewer_grant)
                ?: getString(R.string.pdf_viewer_grant)

        init()

    }

    private fun init() {
        if (intent.extras!!.containsKey(FILE_URL)) {
            fileUrl = intent.extras!!.getString(FILE_URL)
            if (isPDFFromPath) {
                initPdfViewerWithPath(this.fileUrl)
            } else {
                if (checkInternetConnection(this)) {
                    loadFileFromNetwork(this.fileUrl)
                } else {
                    Toast.makeText(
                        this,
                        error_no_internet_connection,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.pdfView.statusListener = object : PdfRendererView.StatusCallBack {
            override fun onPdfLoadStart() {
                runOnUiThread {
                    true.showProgressBar()
                }
            }

            override fun onPdfLoadProgress(
                progress: Int,
                downloadedBytes: Long,
                totalBytes: Long?
            ) {
                //Download is in progress
            }

            override fun onPdfLoadSuccess(absolutePath: String) {
                runOnUiThread {
                    false.showProgressBar()
                    downloadedFilePath = absolutePath
                }
            }

            override fun onError(error: Throwable) {
                runOnUiThread {
                    false.showProgressBar()
                    onPdfError(error.toString())
                }
            }

            override fun onPageChanged(currentPage: Int, totalPage: Int) {
                //Page change. Not require
            }
        }
    }


    private fun loadFileFromNetwork(fileUrl: String?) {
        initPdfViewer(
            fileUrl
        )
    }

    private fun initPdfViewer(fileUrl: String?) {
        if (TextUtils.isEmpty(fileUrl)) onPdfError("")
        //Initiating PDf Viewer with URL
        try {
            binding.pdfView.initWithUrl(
                fileUrl!!,
                headers,
                lifecycleScope,
                lifecycle = lifecycle
            )
        } catch (e: Exception) {
            onPdfError(e.toString())
        }
    }

    private fun initPdfViewerWithPath(filePath: String?) {
        if (TextUtils.isEmpty(filePath)) {
            onPdfError("")
            return
        }
        try {
            val file = if (filePath!!.startsWith("content://")) {
                uriToFile(applicationContext, Uri.parse(filePath))
            } else if (isFromAssets) {
                fileFromAsset(this, filePath)
            } else {
                File(filePath)
            }
            binding.pdfView.initWithFile(file)
        } catch (e: Exception) {
            onPdfError(e.toString())
        }
    }

    private fun onPdfError(e: String) {
        Log.e("Pdf render error", e)
        AlertDialog.Builder(this)
            .setTitle(pdf_viewer_error)
            .setMessage(error_pdf_corrupted)
            .setPositiveButton(pdf_viewer_retry) { dialog, which ->
                runOnUiThread {
                    init()
                }
            }
            .setNegativeButton(pdf_viewer_cancel, null)
            .show()
    }

    private fun Boolean.showProgressBar() {
        binding.progressBar.visibility = if (this) VISIBLE else GONE
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startDownload()
        } else {
            // Show an AlertDialog here
            AlertDialog.Builder(this)
                .setTitle(permission_required_title)
                .setMessage(permission_required)
                .setPositiveButton(pdf_viewer_grant) { dialog: DialogInterface, which: Int ->
                    // Request the permission again
                    requestStoragePermission()
                }
                .setNegativeButton(pdf_viewer_cancel, null)
                .show()
        }
    }

    private fun requestStoragePermission() {
        requestPermissionLauncher.launch(permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun startDownload() {
        val fileName = intent.getStringExtra(FILE_TITLE) ?: "downloaded_file.pdf"
        downloadedFilePath?.let { filePath ->
            if (SAVE_TO_DOWNLOADS) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveFileToPublicDirectoryScopedStorage(filePath, fileName)
                } else {
                    saveFileToPublicDirectoryLegacy(filePath, fileName)
                }
            } else {
                promptUserForLocation(fileName)
            }
        } ?: Toast.makeText(this, file_not_downloaded_yet, Toast.LENGTH_SHORT).show()
    }

    private fun promptUserForLocation(fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        createFileLauncher.launch(intent)
    }

    private val createFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        downloadedFilePath?.let { filePath ->
                            File(filePath).inputStream().copyTo(outputStream)
                        }
                    }
                    Toast.makeText(this, file_saved_successfully, Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun saveFileToPublicDirectoryScopedStorage(filePath: String, fileName: String) {
        val contentResolver = applicationContext.contentResolver
        val uri = createPdfDocumentUri(contentResolver, fileName)
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            File(filePath).inputStream().copyTo(outputStream)
        }
        Toast.makeText(this, file_saved_to_downloads, Toast.LENGTH_SHORT).show()
    }

    private fun saveFileToPublicDirectoryLegacy(filePath: String, fileName: String) {
        val destinationFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        File(filePath).copyTo(destinationFile, overwrite = true)
        Toast.makeText(this, file_saved_to_downloads, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.pdfView.closePdfRender()
    }

}