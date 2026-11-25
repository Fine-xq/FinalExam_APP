package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * 极简图片加载器：
 * - 本地文件优先（用户上传的封面）；
 * - 否则走 HttpURLConnection 下载（对应作业八的网络通信）；
 * - LruCache 缓存，失败时放弃，界面回退到分区表情图标。
 */
object ImageLoader {

    private val cache = LruCache<String, Bitmap>(24)
    private val failed = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val executor = Executors.newFixedThreadPool(3)

    /** 统一入口：有本地封面就读本地，没有就用网络封面。 */
    fun loadCover(goods: Goods, filesDir: File, imageView: ImageView, onLoaded: () -> Unit = {}) {
        val local = goods.coverLocalPath?.takeIf { it.isNotEmpty() }
        if (local != null) {
            val file = File(filesDir, local)
            if (file.exists()) {
                loadFile(file, imageView, onLoaded)
                return
            }
        }
        load(goods.coverUrl, imageView, onLoaded)
    }

    fun loadFile(file: File, imageView: ImageView, onLoaded: () -> Unit = {}) {
        val key = "file:${file.absolutePath}#${file.lastModified()}"
        imageView.tag = key
        cache.get(key)?.let { bitmap ->
            imageView.setImageBitmap(bitmap)
            imageView.visibility = View.VISIBLE
            onLoaded()
            return
        }

        executor.execute {
            val bitmap = decodeFile(file)
            if (bitmap == null) return@execute
            cache.put(key, bitmap)
            imageView.post {
                if (imageView.tag == key) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                    onLoaded()
                }
            }
        }
    }

    fun load(url: String, imageView: ImageView, onLoaded: () -> Unit = {}) {
        imageView.tag = url
        cache.get(url)?.let { bitmap ->
            imageView.setImageBitmap(bitmap)
            imageView.visibility = View.VISIBLE
            onLoaded()
            return
        }
        if (failed.contains(url)) return

        executor.execute {
            val bitmap = download(url)
            if (bitmap == null) {
                failed.add(url)
                return@execute
            }
            cache.put(url, bitmap)
            imageView.post {
                if (imageView.tag == url) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                    onLoaded()
                }
            }
        }
    }

    private fun decodeFile(file: File): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 720 || bounds.outHeight / sample > 720) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(file.absolutePath, opts)
    } catch (e: Exception) {
        null
    }

    private fun download(url: String): Bitmap? = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 2500
        connection.readTimeout = 2500
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
        val bytes = connection.inputStream.use { it.readBytes() }
        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } catch (e: Exception) {
        null
    }
}
