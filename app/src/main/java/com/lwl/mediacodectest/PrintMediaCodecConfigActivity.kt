package com.lwl.mediacodectest

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaCodecList.REGULAR_CODECS
import android.media.MediaFormat
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class PrintMediaCodecConfigActivity : AppCompatActivity() {
    lateinit var tvMsg: TextView
    lateinit var etWidth: EditText
    lateinit var etHeight: EditText




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tv_text = findViewById<TextView>(R.id.tv_text)
        tvMsg = findViewById<TextView>(R.id.tv_msg)
        etWidth = findViewById<EditText>(R.id.et_width)
        etHeight = findViewById<EditText>(R.id.et_height)

        tv_text.setOnClickListener {
            test()
        }
        val mediaCodecList = MediaCodecList(REGULAR_CODECS)
        for (codecInfo in mediaCodecList.codecInfos) {
//            val capabilitiesForType = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
            if (codecInfo.isEncoder) {
                for (supportedType in codecInfo.supportedTypes) {
                    if (supportedType.contains(Regex("avc"))) {
                        Log.d(
                            "Main",
                            "name ${codecInfo.name}  ${Arrays.toString(codecInfo.supportedTypes)} "
                        )
                        val capabilitiesForType =
                            codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                        Log.d(
                            "Main",
                            "name ${codecInfo.name}  ${Arrays.toString(codecInfo.supportedTypes)} capabilitiesForType ${capabilitiesForType.videoCapabilities} "
                        )
                        val videoCapabilities = capabilitiesForType.videoCapabilities
                        tvMsg.apply {
                            append("======== ${codecInfo.name}\n")
                            append("bitrateRange ${videoCapabilities.bitrateRange}\n")
                            append("supportedWidths ${videoCapabilities.supportedWidths}\n")
                            append("supportedHeights ${videoCapabilities.supportedHeights}\n")
                            append("supportedFrameRates ${videoCapabilities.supportedFrameRates}\n")
                        }
                        Log.d(
                            "main",
                            tvMsg.text.toString()
                        )


                    }
                }

            }
        }


    }

    private fun test() {
        val mime = "video/avc"
        val widht = etWidth.text.toString().toInt();
        val height = etHeight.text.toString().toInt();


        val format = MediaFormat.createVideoFormat(mime, widht, height)

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
//        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
//                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )

        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 26 * 1024 * 1024)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 60)
//       if (VERBOSE) Log.d(TAG, "format: " + format);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        var mVideoEncoder: MediaCodec? = null
        try {
            Log.e("Main", format.toString())
            mVideoEncoder = MediaCodec.createEncoderByType(mime)
            mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val mInputSurface = mVideoEncoder?.createInputSurface()
            tvMsg.text = "成功"
        } catch (e: MediaCodec.CodecException) {
            e.printStackTrace()
            Log.e("MainActivity", "${e.message} width $widht height $height", e)
            tvMsg.text = "${e} width $widht height $height"
        } catch (e: IllegalStateException) {
            Log.e("MainActivity", "${e.message} width $widht height $height", e)
            tvMsg.text = "${e} width $widht height $height"

        }


    }
}