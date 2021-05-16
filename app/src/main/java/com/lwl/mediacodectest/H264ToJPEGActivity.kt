package com.lwl.mediacodectest

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import com.lwl.mediacodectest.databinding.ActivityDecoderBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class H264ToJPEGActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecoderBinding

    private val holder: SurfaceHolder? = null
    private val TAG = "DecoderActivity"
    private val executor: Executor = Executors.newSingleThreadExecutor()

    companion object {
        init {
            System.loadLibrary("ijkffmpegCmd")
        }
    }

    private var bitmap: Bitmap? = null
    external fun decode(buff: ByteArray?): ByteArray?
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecoderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnStartFfmpeg.setOnClickListener {
            startDecoder()
            //                startMediaCodecDecoder();
        }
        binding.ivFfmpeg.setOnClickListener {
            bitmap!!.recycle()
            binding.ivFfmpeg.setImageBitmap(null)
        }

        binding.btnStartMediaCodec.setOnClickListener {
            startMediaCodecDecoder()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 123
            )
        }
    }

    fun readFile(): ByteArray {
        val inputStream = assets.open("idr_2.7k.h264")
//        val inputStream = assets.open("idr_8k.h264")
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        return buffer

    }

    fun readFromSdcard(): ByteArray {
        val file = File("/sdcard/AKASO/amba/thumb_1620899691638.h264")
        val size = file.length()
        val inputStream = FileInputStream(file)
        val buffer = ByteArray(size.toInt())
        inputStream.read(buffer)
        inputStream.close()
        return buffer;

    }

    private fun startDecoder() {
        executor.execute {
            try {
//                val buff = readFromSdcard()
                val buff = readFile()
                Log.d(TAG, "source size ${buff.size}")
                val start = System.currentTimeMillis()
                val decode = decode(buff)
                if (decode != null && decode.size > 0) {
                    Log.d(
                        TAG,
                        "decode complete" + " time " + (System.currentTimeMillis() - start) + " length " + decode.size + "   " + decode.size / 1024f + " kb  Thread " + Thread.currentThread().name
                    )
                    bitmap = BitmapFactory.decodeByteArray(decode, 0, decode.size)
                    runOnUiThread { binding.ivFfmpeg.setImageBitmap(bitmap) }
                } else {
                    Log.d(TAG, "解析失败")
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    //初始化播放相关
    private fun startMediaCodecDecoder() {
        executor.execute {
            val buff = readFile()
            val start = System.currentTimeMillis()
            val bitmap = MediaCodecUtils.decoderData(buff)
            Log.d(TAG, "MediaCodec 完成${System.currentTimeMillis() - start}")

            if (bitmap != null) {
                runOnUiThread { binding.ivFfmpeg.setImageBitmap(bitmap) }
            } else {
                Log.d(TAG, "解析失败")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.e("Main", "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("Main", "onDestroy")
    }

}