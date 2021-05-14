package com.lwl.mediacodectest

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.random.Random

class LockActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.tv_msg).setOnClickListener { testDownlaod() }
        findViewById<View>(R.id.tv_text).setOnClickListener {
            Log.e("Locktest", "手动停止")
            stop = true
        }


    }


    private val lock = ReentrantLock()
//    private val thread = Executors.newSingleThreadExecutor()

    @Volatile
    private var stop: Boolean = false

    fun testDownlaod() {
        for (i in 0..5) {
            Thread({
                Log.d("Locktest", "外部线程 启动 ${getThreadName()}")
                try {
                    Log.d("Locktest", "请求锁 ${getThreadName()}")
                    if (lock.tryLock(30, TimeUnit.SECONDS)) {
                        getThumbInfo(i)
                    } else {
                        Log.d("Locktest", "请求锁失败 ${getThreadName()}")
                    }

                } catch (e: InterruptedException) {
                    Log.d("Locktest", "err ${e.message}")
                }

            }, "Glide-$i").start()


        }

    }

    fun getThumbInfo(index: Int) {
//        lock.lock()
        stop = false
        Log.d("Locktest", "获得锁===== :${getThreadName()}")
        try {

            val info = Observable.fromCallable {
                val waite = Random.nextLong(5000)
                Log.d("Locktest", "获取缩略图 信息 等待$waite:${getThreadName()}")
                Thread.sleep(waite)
                return@fromCallable index.toString()
            }
                .timeout(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .doOnNext {
                }.blockingLast()
            Log.d("Locktest", "得到缩略图信息$info ${getThreadName()}")
            readData()
            Log.d("Locktest", "结束 ${getThreadName()}")


        } catch (e: Exception) {
            Log.e("Locktest", "失败 ${getThreadName()}")
            return
        } finally {
            Log.d("Locktest", "释放锁 ${getThreadName()}")
            lock.unlock()

        }


//                    .timeout(10, TimeUnit.SECONDS)


    }

    fun readData() {
        Log.d("Locktest", "开始读数据 ${getThreadName()}")
        for (i in 0..5) {
            if (!stop) {
                Log.d("Locktest", "进度 $i ${getThreadName()}")

                Thread.sleep(1000)
            }
        }
        Log.d("Locktest", "数据读取完成 ${getThreadName()}")
    }

    fun getThreadName(): String {
        return Thread.currentThread().name
    }


}