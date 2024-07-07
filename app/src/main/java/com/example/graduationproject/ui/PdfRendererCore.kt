package com.example.graduationproject.ui


import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Size
import com.example.graduationproject.utilities.CacheManager
import com.example.graduationproject.utilities.CacheManager.Companion.CACHE_PATH
import com.example.graduationproject.utilities.SHARED_PREFERNCES_KEY
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap


internal class PdfRendererCore(
    private val context: Context, fileDescriptor: ParcelFileDescriptor
) : TextToSpeech.OnInitListener {

    private var isRendererOpen = false
    private lateinit var sharedPreferences: SharedPreferences

    constructor(context: Context, file: File) : this(
        context = context, fileDescriptor = getFileDescriptor(file)
    )

    private val openPages = ConcurrentHashMap<Int, PdfRenderer.Page>()
    private var pdfRenderer: PdfRenderer? = null
    private val cacheManager = CacheManager(context)
    private lateinit var textToSpeech: TextToSpeech

    companion object {

        private fun sanitizeFilePath(filePath: String): String {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val path = Paths.get(filePath)
                    if (Files.exists(path)) {
                        filePath
                    } else {
                        "" // Return a default safe path or handle the error
                    }
                } else {
                    filePath
                }
            } catch (e: Exception) {
                "" // Handle the exception and return a safe default path
            }
        }

        internal fun getFileDescriptor(file: File): ParcelFileDescriptor {
            val safeFile = File(sanitizeFilePath(file.path))
            return ParcelFileDescriptor.open(safeFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }


    init {
        pdfRenderer = PdfRenderer(fileDescriptor).also { isRendererOpen = true }
        cacheManager.initCache()

        textToSpeech = TextToSpeech(context, this)
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERNCES_KEY, MODE_PRIVATE)

    }

    internal fun getBitmapFromCache(pageNo: Int): Bitmap? = cacheManager.getBitmapFromCache(pageNo)

    private fun addBitmapToMemoryCache(pageNo: Int, bitmap: Bitmap) =
        cacheManager.addBitmapToCache(pageNo, bitmap)

    private fun writeBitmapToCache(pageNo: Int, bitmap: Bitmap, shouldCache: Boolean = true) {
        if (!shouldCache) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val savePath = File(File(context.cacheDir, CACHE_PATH), pageNo.toString())
                FileOutputStream(savePath).use { fos ->
                    bitmap.compress(CompressFormat.JPEG, 75, fos) // Compress as JPEG
                }
            } catch (e: Exception) {
                Log.e("PdfRendererCore", "Error writing bitmap to cache: ${e.message}")
            }
        }
    }

    fun pageExistInCache(pageNo: Int): Boolean = cacheManager.pageExistsInCache(pageNo)

    fun getPageCount(): Int {
        synchronized(this) {
            if (!isRendererOpen) return 0
            return pdfRenderer?.pageCount ?: 0
        }
    }

    fun renderPage(
        pageNo: Int,
        bitmap: Bitmap,
        onBitmapReady: ((success: Boolean, pageNo: Int, bitmap: Bitmap?) -> Unit)? = null
    ) {
        if (pageNo >= getPageCount()) {
            onBitmapReady?.invoke(false, pageNo, null)
            return
        }
        val cachedBitmap = getBitmapFromCache(pageNo)
        if (cachedBitmap != null) {
            CoroutineScope(Dispatchers.Main).launch {
                onBitmapReady?.invoke(
                    true, pageNo, cachedBitmap
                )
            }
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            synchronized(this@PdfRendererCore) {
                if (!isRendererOpen) return@launch
                openPageSafely(pageNo)?.use { pdfPage ->
                    try {
                        bitmap.eraseColor(Color.WHITE) // Clear the bitmap with white color
                        pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        addBitmapToMemoryCache(pageNo, bitmap)
                        CoroutineScope(Dispatchers.IO).launch {
                            writeBitmapToCache(pageNo, bitmap)
                            convertBitmapToText(bitmap, pageNo)
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            onBitmapReady?.invoke(
                                true, pageNo, bitmap
                            )
                        }
                    } catch (e: Exception) {
                        CoroutineScope(Dispatchers.Main).launch {
                            onBitmapReady?.invoke(
                                false, pageNo, null
                            )
                        }
                    }
                }
            }
        }
    }


    private fun convertBitmapToText(bitmap: Bitmap, pageNo: Int) {
        val textRecognizer = TextRecognizer.Builder(context).build()
        val imageFrame =
            Frame.Builder().setBitmap(bitmap) // Replace 'bitmap' with your image bitmap
                .build()

        var imageText = "Extracted text"
        val textBlocks = textRecognizer.detect(imageFrame)

        for (i in 0 until textBlocks.size()) {
            val textBlock = textBlocks.get(textBlocks.keyAt(i))
            imageText = textBlock.value // Extracted text
        }
//        if (pageNo % 2 == 1 && pageNo != 1) {
//            var count = pageNo - 1
//            while (count != pageNo - 4) {
//                deleteTextFromSharedPreference(count)
//                Log.i("hello2", "delete $count")
//                count--
//            }
//        }
        saveTextToSharedPreference(imageText, pageNo)
    }



    private fun saveTextToSharedPreference(text: String, pageNo: Int) {
        val editor = sharedPreferences.edit()
        editor.putString("page${pageNo}", text)
        Log.i("hello2", "save $pageNo")
        editor.apply()
    }

    private fun deleteTextFromSharedPreference(pageNo: Int) {
        val editor = sharedPreferences.edit()
        editor.remove("page${pageNo}")
        editor.apply()
    }


    private suspend fun <T> withPdfPage(pageNo: Int, block: (PdfRenderer.Page) -> T): T? =
        withContext(Dispatchers.IO) {
            synchronized(this@PdfRendererCore) {
                pdfRenderer?.openPage(pageNo)?.use { page ->
                    return@withContext block(page)
                }
            }
            null
        }

    private val pageDimensionCache = mutableMapOf<Int, Size>()

    fun getPageDimensionsAsync(pageNo: Int, callback: (Size) -> Unit) {
        pageDimensionCache[pageNo]?.let {
            callback(it)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val size = withPdfPage(pageNo) { page ->
                Size(page.width, page.height).also { pageSize ->
                    pageDimensionCache[pageNo] = pageSize
                }
            } ?: Size(1, 1) // Fallback to a default minimal size

            withContext(Dispatchers.Main) {
                callback(size)
            }
        }
    }

    private fun openPageSafely(pageNo: Int): PdfRenderer.Page? {
        synchronized(this) {
            if (!isRendererOpen) return null
            closeAllOpenPages()
            return pdfRenderer?.openPage(pageNo)?.also { page ->
                openPages[pageNo] = page
            }
        }
    }

    private fun closeAllOpenPages() {
        synchronized(this) {
            openPages.values.forEach { page ->
                try {
                    page.close()
                } catch (e: IllegalStateException) {
                    Log.e("PDFRendererCore", "Page was already closed")
                }
            }
            openPages.clear() // Clear the map after closing all pages.
        }
    }

    fun closePdfRender() {
        synchronized(this) {
            closeAllOpenPages()
            if (isRendererOpen) {
                pdfRenderer?.close()
                isRendererOpen = false
            }
            cacheManager.clearCache()
        }
    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Handle the error
            } else {
            }
        } else {
            // Handle the initialization failure
        }
    }

}
