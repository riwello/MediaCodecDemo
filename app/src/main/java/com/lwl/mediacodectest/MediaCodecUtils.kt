package com.lwl.mediacodectest

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaFormat
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object MediaCodecUtils {

    private val COLOR_FormatI420 = 1
    private val COLOR_FormatNV21 = 2
    private val TAG= "MediaCodec"


    @SuppressLint("WrongConstant")
    fun decoderData(sourceData: ByteArray): Bitmap? {
        val decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val capabilitiesForType: CodecCapabilities =
            decoder.getCodecInfo().getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val videoCapabilities = capabilitiesForType.videoCapabilities
        val supportedWidths = videoCapabilities.supportedWidths
        val supportedHeights = videoCapabilities.supportedHeights
        //初始化MediaFormat
        var mediaFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            supportedWidths.lower,
            supportedHeights.lower
        )
        //配置MediaFormat以及需要显示的surface
        decoder.configure(mediaFormat, null, null, 0)
        decoder.start()

        val inputBufferIndex = decoder . dequeueInputBuffer (-1)

        Log.d(TAG, "inputBufferIndex  $inputBufferIndex")
        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
        //喂数据
        val put = inputBuffer?.put(sourceData)
        decoder.queueInputBuffer(
            inputBufferIndex,
            0,
            sourceData.size,
            5,
            MediaCodec.BUFFER_FLAG_END_OF_STREAM
        )
        // 获取输出buffer index

        val bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex = -1
        while (outputBufferIndex < 0) {

            outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 200)
            Log.d(TAG, "outputBufferIndex $outputBufferIndex")
        }
        val image = decoder.getOutputImage(outputBufferIndex)


        val mediaImageToBitmap = mediaImageToBitmap(image!!)
        decoder.stop()
        decoder.release()
        return mediaImageToBitmap
    }


    fun mediaImageToBitmap(image: Image): Bitmap? {
        val yuvBytes = ByteBuffer.wrap(getDataFromImage(image, COLOR_FormatNV21))

        // Convert YUV to RGB
        val rs = RenderScript.create(App.INSTANCE)
        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        val allocationRgb = Allocation.createFromBitmap(rs, bitmap)
        val allocationYuv = Allocation.createSized(rs, Element.U8(rs), yuvBytes.array().size)
        allocationYuv.copyFrom(yuvBytes.array())
        val scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        scriptYuvToRgb.setInput(allocationYuv)
        scriptYuvToRgb.forEach(allocationRgb)
        allocationRgb.copyTo(bitmap)
        val rote = 200f / image.width
        val matrix = Matrix()
        matrix.postScale(rote, rote) //长和宽放大缩小的比例
        val resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val baos = ByteArrayOutputStream()
        resizeBmp.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val isBm = ByteArrayInputStream(baos.toByteArray())
        val newBitmap = BitmapFactory.decodeStream(isBm, null, null)

        bitmap.recycle()
        resizeBmp.recycle()

        return newBitmap
    }

    private fun getDataFromImage(image: Image, colorFormat: Int): ByteArray? {
        require(!(colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21)) { "only support COLOR_FormatI420 " + "and COLOR_FormatNV21" }
//        if (!isImageFormatSupported(image)) {
//            throw RuntimeException("can't convert Image to byte array, format " + image.format)
//        }
        val crop = image.cropRect
        val format = image.format
        val width = crop.width()
        val height = crop.height()
        val planes = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
//        Log.v(TAG, "get data from " + planes.size + " planes")
        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = width * height
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> if (colorFormat == COLOR_FormatI420) {
                    channelOffset = (width * height * 1.25).toInt()
                    outputStride = 1
                } else if (colorFormat == COLOR_FormatNV21) {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride
//            Log.v(TAG, "pixelStride $pixelStride")
//            Log.v(TAG, "rowStride $rowStride")
//            Log.v(TAG, "width $width")
//            Log.v(TAG, "height $height")
//            Log.v(TAG, "buffer size " + buffer.remaining())
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer[data, channelOffset, length]
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
//            Log.v(TAG, "Finished reading data from plane $i")
        }
        return data
    }

}