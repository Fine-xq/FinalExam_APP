package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder

/**
 * 购买成功提示音服务（对应作业七：Service + MediaPlayer + ContentResolver）。
 * 通过 RingtoneManager 取得系统通知铃声 Uri，交给 MediaPlayer 播放，播放完自动停止。
 */
class SoundService : Service() {

    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(this@SoundService, uri)
                setOnCompletionListener { stopSelf() }
                prepare()
                start()
            }
        } catch (e: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
