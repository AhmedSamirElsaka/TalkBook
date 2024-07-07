package com.example.graduationproject.ui

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleObserver
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.example.graduationproject.R
import com.example.graduationproject.databinding.PdfRendererViewBinding
import com.example.graduationproject.utilities.PdfEngine
import com.example.graduationproject.utilities.SHARED_PREFERNCES_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale


class PdfRendererView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), LifecycleObserver, TextToSpeech.OnInitListener {
    private lateinit var pdfRendererCore: PdfRendererCore
    private lateinit var pdfViewAdapter: PdfViewAdapter
    private var engine = PdfEngine.INTERNAL
    private var showDivider = true
    private var divider: Drawable? = null
    private var enableLoadingForPages: Boolean = false
    private var pdfRendererCoreInitialised = false
    private var pageMargin: Rect = Rect(0, 0, 0, 0)
    var statusListener: StatusCallBack? = null
    private var positionToUseForState: Int = 0
    private var restoredScrollPosition: Int = NO_POSITION
    private var disableScreenshots: Boolean = false
    private var postInitializationAction: (() -> Unit)? = null
    private lateinit var binding: PdfRendererViewBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var tts: TextToSpeech
    private var currentPosition: Int = 0
    private var isPaused: Boolean = false
    var textToConvert: String? = "default text"
    var previousText = "default text"



    val totalPageCount: Int
        get() {
            return pdfRendererCore.getPageCount()
        }

    init {
        getAttrs(attrs, defStyleAttr)
        tts = TextToSpeech(context, this)
    }


    interface StatusCallBack {
        fun onPdfLoadStart() {}
        fun onPdfLoadProgress(progress: Int, downloadedBytes: Long, totalBytes: Long?) {}
        fun onPdfLoadSuccess(absolutePath: String) {}
        fun onError(error: Throwable) {}
        fun onPageChanged(currentPage: Int, totalPage: Int) {}
    }

    fun initWithUrl(
        url: String,
        headers: HeaderData = HeaderData(),
        lifecycleCoroutineScope: LifecycleCoroutineScope,
        lifecycle: Lifecycle
    ) {
        lifecycle.addObserver(this) // Register as LifecycleObserver
        PdfDownloader(lifecycleCoroutineScope, headers, url, object : PdfDownloader.StatusListener {
            override fun getContext(): Context = context
            override fun onDownloadStart() {
                statusListener?.onPdfLoadStart()
            }

            override fun onDownloadProgress(currentBytes: Long, totalBytes: Long) {
                var progress = (currentBytes.toFloat() / totalBytes.toFloat() * 100F).toInt()
                if (progress >= 100) progress = 100
                statusListener?.onPdfLoadProgress(progress, currentBytes, totalBytes)
            }

            override fun onDownloadSuccess(absolutePath: String) {
                initWithFile(File(absolutePath))
                statusListener?.onPdfLoadSuccess(absolutePath)
            }

            override fun onError(error: Throwable) {
                error.printStackTrace()
                statusListener?.onError(error)
            }
        })
    }

    fun initWithFile(file: File) {
        init(file)
    }


    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = Bundle()
        savedState.putParcelable("superState", superState)
        if (this::binding.isInitialized) {
            savedState.putInt("scrollPosition", positionToUseForState)
        }
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var savedState = state
        if (savedState is Bundle) {
            val superState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedState.getParcelable("superState", Parcelable::class.java)
            } else {
                savedState.getParcelable("superState")
            }
            super.onRestoreInstanceState(superState)
            restoredScrollPosition = savedState.getInt("scrollPosition", positionToUseForState)
        } else {
            super.onRestoreInstanceState(savedState)
        }
    }

    private fun init(file: File) {
        val fileDescriptor = PdfRendererCore.getFileDescriptor(file)
        init(fileDescriptor)
    }

    private fun init(fileDescriptor: ParcelFileDescriptor) {
        // Proceed with safeFile
        pdfRendererCore = PdfRendererCore(context, fileDescriptor)
        pdfRendererCoreInitialised = true
        pdfViewAdapter = PdfViewAdapter(context, pdfRendererCore, pageMargin, enableLoadingForPages)
        val v = LayoutInflater.from(context).inflate(R.layout.pdf_renderer_view, this, false)
        addView(v)

        binding = PdfRendererViewBinding.inflate(LayoutInflater.from(context), this, true)

        sharedPreferences = context.getSharedPreferences(
            SHARED_PREFERNCES_KEY, Context.MODE_PRIVATE
        )


        binding.recyclerView.apply {
            adapter = pdfViewAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()


            if (showDivider) {
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                    divider?.let { setDrawable(it) }
                }.let { addItemDecoration(it) }
            }
            addOnScrollListener(scrollListener)
        }

        binding.playOrPause.setOnClickListener {
            if (tts.isSpeaking) {
                pauseSpeaking()
                binding.playOrPause.setImageResource(R.drawable.pause)
            } else if (isPaused) {
                resumeSpeaking()
                binding.playOrPause.setImageResource(R.drawable.play)
            }
        }

        binding.forward.setOnClickListener {
            seekForward(10)
        }

        binding.rewind.setOnClickListener {
            seekBackward(10)
        }
        // Set seekbar listener
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    pauseSpeaking()
                    currentPosition = progress
                    resumeSpeakingFrom(currentPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        Handler(Looper.getMainLooper()).postDelayed({
            if (restoredScrollPosition != NO_POSITION) {
                binding.recyclerView.scrollToPosition(restoredScrollPosition)
                restoredScrollPosition = NO_POSITION  // Reset after applying
            }
        }, 500) // Adjust delay as needed

//        runnable = Runnable {
//            binding.pageNumber.pageNo.visibility = View.GONE
//        }

        binding.recyclerView.post {
            postInitializationAction?.invoke()
            postInitializationAction = null
        }

    }

    private fun resumeSpeakingFrom(position: Int) {
        if (isPaused || !tts.isSpeaking) {
            val remainingText = textToConvert!!.substring(position)
            speakOut(remainingText)
        }
    }

    private fun pauseSpeaking() {
        if (tts.isSpeaking) {
            tts.stop()
            isPaused = true
        }
    }

    private fun seekForward(seconds: Int) {
        if (tts.isSpeaking || isPaused) {
            // Approximate average speaking rate (characters per second)
            val avgCharsPerSecond = 15
            currentPosition += avgCharsPerSecond * seconds

            // Ensure we don't exceed the text length
            if (currentPosition > textToConvert!!.length) {
                currentPosition = textToConvert!!.length
            }

            val remainingText = textToConvert!!.substring(currentPosition)
            speakOut(remainingText)
        }
    }

    private fun seekBackward(seconds: Int) {
        if (tts.isSpeaking || isPaused) {
            // Approximate average speaking rate (characters per second)
            val avgCharsPerSecond = 15
            currentPosition -= avgCharsPerSecond * seconds

            // Ensure we don't go below the start of the text
            if (currentPosition < 0) {
                currentPosition = 0
            }

            val remainingText = textToConvert!!.substring(currentPosition)
            speakOut(remainingText)
        }
    }

    private fun resumeSpeaking() {
        if (isPaused) {
            val remainingText = textToConvert!!.substring(currentPosition)
            speakOut(remainingText)
        }
    }

    private fun stopSpeaking() {
        if (tts.isSpeaking) {
            tts.stop()
        }
        isPaused = false
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        private var lastFirstVisiblePosition = NO_POSITION
        private var lastCompletelyVisiblePosition = NO_POSITION

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager


            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            val firstCompletelyVisiblePosition =
                layoutManager.findFirstCompletelyVisibleItemPosition()
            val isPositionChanged =
                firstVisiblePosition != lastFirstVisiblePosition || firstCompletelyVisiblePosition != lastCompletelyVisiblePosition
            if (isPositionChanged) {
                val positionToUse = if (firstCompletelyVisiblePosition != NO_POSITION) {
                    firstCompletelyVisiblePosition
                } else {
                    firstVisiblePosition
                }
                positionToUseForState = positionToUse
                updatePageNumberDisplay(positionToUse)



                if (positionToUse == 0 || positionToUse % 2 == 0) {
                    Toast.makeText(
                        context, "please wait while audio is prepared", Toast.LENGTH_LONG
                    ).show()
                    CoroutineScope(Dispatchers.IO).launch {
                        while (textToConvert.equals("default text") || textToConvert.equals(
                                previousText
                            )
                        ) {
                            textToConvert =
                                sharedPreferences.getString("page${positionToUse}", "default text")
                        }
                    }.invokeOnCompletion {
                        Log.i("hello2", textToConvert.toString())
                        speakOut(textToConvert!!)
                        previousText = textToConvert.toString()
                    }
                } else {
                    textToConvert = context.getSharedPreferences(
                        SHARED_PREFERNCES_KEY, Context.MODE_PRIVATE
                    ).getString("page${positionToUse}", "default text")
                    previousText = textToConvert.toString()
                }
                speek()

                lastFirstVisiblePosition = firstVisiblePosition
                lastCompletelyVisiblePosition = firstCompletelyVisiblePosition
            } else {
                positionToUseForState = firstVisiblePosition
            }
        }

        private fun updatePageNumberDisplay(position: Int) {
            if (position != NO_POSITION) {
                binding.pageNumber.pageNo.text =
                    context.getString(R.string.pdfView_page_no, position + 1, totalPageCount)
                binding.pageNumber.pageNo.visibility = View.VISIBLE
                if (position == 0) {
//                    binding.pageNumber.pageNo.postDelayed({
//                        binding.pageNumber.pageNo.visibility = View.GONE
//                    }, 3000)
                }
                statusListener?.onPageChanged(position, totalPageCount)
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                binding.pageNumber.pageNo.postDelayed(runnable, 3000)
            } else {
//                binding.pageNumber.pageNo.removeCallbacks(runnable)
            }
        }
    }

    private fun speek() {
        currentPosition = 0
        binding.seekBar.max = textToConvert!!.length
        binding.seekBar.progress = 0
        speakOut(textToConvert!!)
    }

    private fun getAttrs(attrs: AttributeSet?, defStyle: Int) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.PdfRendererView, defStyle, 0)
        setTypeArray(typedArray)
    }

    private fun setTypeArray(typedArray: TypedArray) {
        val engineValue =
            typedArray.getInt(R.styleable.PdfRendererView_pdfView_engine, PdfEngine.INTERNAL.value)
        engine = PdfEngine.values().first { it.value == engineValue }


        showDivider = typedArray.getBoolean(R.styleable.PdfRendererView_pdfView_showDivider, true)
        divider = typedArray.getDrawable(R.styleable.PdfRendererView_pdfView_divider)
        enableLoadingForPages = typedArray.getBoolean(
            R.styleable.PdfRendererView_pdfView_enableLoadingForPages, enableLoadingForPages
        )
        val marginDim =
            typedArray.getDimensionPixelSize(R.styleable.PdfRendererView_pdfView_page_margin, 0)
        pageMargin = Rect(marginDim, marginDim, marginDim, marginDim).apply {
            top = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginTop, top
            )
            left = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginLeft, left
            )
            right = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginRight, right
            )
            bottom = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginBottom, bottom
            )
        }
        disableScreenshots =
            typedArray.getBoolean(R.styleable.PdfRendererView_pdfView_disableScreenshots, false)
        applyScreenshotSecurity()
        typedArray.recycle()
    }

    private fun applyScreenshotSecurity() {
        if (disableScreenshots) {
            // Disables taking screenshots and screen recording
            (context as? Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun closePdfRender() {
        val editor = sharedPreferences.edit()
        if (pdfRendererCoreInitialised) {
            pdfRendererCore.closePdfRender()
            pdfRendererCoreInitialised = false
            editor.clear();
            editor.apply();
        }

        tts.stop()
        tts.shutdown()
    }

    private fun speakOut(text: String) {
        isPaused = false
        val params = hashMapOf(
            TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to "utteranceId"
        )
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the language
            val result = tts.setLanguage(Locale.ENGLISH)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language data is missing or the language is not supported.
                println("The language is not supported!")
            }

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {}

                override fun onError(utteranceId: String?) {}

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (!isPaused) {
                        currentPosition = start
                        binding.seekBar.progress = start
                    }
                }
            })
        } else {
            // Initialization failed
            println("Initialization failed!")
        }
    }

}
