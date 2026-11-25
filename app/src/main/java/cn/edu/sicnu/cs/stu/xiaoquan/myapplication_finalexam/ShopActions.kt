package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 购买流程共用逻辑：对话框确认 -> 仓库落单 -> 播放提示音 -> 回调刷新。
 */
object ShopActions {

    fun confirmBuy(
        context: Context,
        goods: Goods,
        scope: CoroutineScope,
        onSuccess: () -> Unit
    ) {
        if (goods.sellerId == Session.uid) {
            Toast.makeText(context, R.string.buy_own_goods, Toast.LENGTH_SHORT).show()
            return
        }
        if (goods.isSold) {
            Toast.makeText(context, R.string.buy_failed, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(context)
            .setTitle(R.string.buy_confirm_title)
            .setMessage(
                context.getString(R.string.buy_confirm_message, formatPrice(goods.price), goods.name)
            )
            .setPositiveButton(R.string.action_buy) { _, _ ->
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        Repository.buyGoods(goods.id, Session.uid, Session.username)
                    }
                    if (ok) {
                        Toast.makeText(context, R.string.buy_success, Toast.LENGTH_SHORT).show()
                        runCatching {
                            context.startService(Intent(context, SoundService::class.java))
                        }
                    } else {
                        Toast.makeText(context, R.string.buy_failed, Toast.LENGTH_SHORT).show()
                    }
                    onSuccess()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
