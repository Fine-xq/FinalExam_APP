package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 个人中心：查看/编辑昵称、手机号、收货地址，并提供常用功能入口。
 */
class ProfileActivity : BaseActivity() {

    private lateinit var tvAvatar: TextView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvAccount: TextView
    private lateinit var etNickname: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var btnSave: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tvAvatar = findViewById(R.id.tv_avatar)
        tvDisplayName = findViewById(R.id.tv_display_name)
        tvAccount = findViewById(R.id.tv_account)
        etNickname = findViewById(R.id.et_nickname)
        etPhone = findViewById(R.id.et_phone)
        etAddress = findViewById(R.id.et_address)
        btnSave = findViewById(R.id.btn_save)

        btnSave.setOnClickListener { saveProfile() }

        findViewById<android.view.View>(R.id.row_my_goods).setOnClickListener {
            startActivity(Intent(this, MyGoodsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.row_my_orders).setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }
        findViewById<android.view.View>(R.id.row_lang).setOnClickListener { switchLanguage() }
        findViewById<android.view.View>(R.id.row_about).setOnClickListener { showAbout() }
        findViewById<android.view.View>(R.id.row_logout).setOnClickListener { confirmLogout() }

        loadProfile()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { Repository.getUserById(Session.uid) }
            if (user == null) {
                Toast.makeText(this@ProfileActivity, R.string.login_failed, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            bindUser(user)
        }
    }

    private fun bindUser(user: User) {
        val name = user.displayName
        tvDisplayName.text = name
        tvAccount.text = getString(R.string.profile_account_label) + "：" + user.username
        tvAvatar.text = name.take(1).uppercase()
        etNickname.setText(user.nickname)
        etPhone.setText(user.phone)
        etAddress.setText(user.address)
    }

    private fun saveProfile() {
        val nickname = etNickname.text?.toString().orEmpty()
        val phone = etPhone.text?.toString().orEmpty()
        val address = etAddress.text?.toString().orEmpty()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                Repository.updateProfile(Session.uid, nickname, phone, address)
            }
            Toast.makeText(
                this@ProfileActivity,
                if (ok) R.string.profile_saved else R.string.login_failed,
                Toast.LENGTH_SHORT
            ).show()
            if (ok) {
                // 刷新顶部展示名与头像首字母
                val display = nickname.takeIf { it.isNotBlank() } ?: Session.username
                tvDisplayName.text = display
                tvAvatar.text = display.take(1).uppercase()
            }
        }
    }

    private fun showAbout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun switchLanguage() {
        val next = if (LocaleManager.getLang(this) == "zh-CN") "en-US" else "zh-CN"
        LocaleManager.setLang(this, next)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(next))
        recreate()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.action_logout) { _, _ ->
                Session.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
