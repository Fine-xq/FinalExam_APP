package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 「我的」页：个人信息（昵称 / 手机号 / 收货地址）与常用功能入口。
 */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var tvAvatar: TextView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvAccount: TextView
    private lateinit var etNickname: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etAddress: TextInputEditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvAvatar = view.findViewById(R.id.tv_avatar)
        tvDisplayName = view.findViewById(R.id.tv_display_name)
        tvAccount = view.findViewById(R.id.tv_account)
        etNickname = view.findViewById(R.id.et_nickname)
        etPhone = view.findViewById(R.id.et_phone)
        etAddress = view.findViewById(R.id.et_address)

        view.findViewById<MaterialButton>(R.id.btn_save).setOnClickListener { saveProfile() }
        view.findViewById<View>(R.id.row_my_goods).setOnClickListener {
            startActivity(Intent(requireContext(), MyGoodsActivity::class.java))
        }
        view.findViewById<View>(R.id.row_my_orders).setOnClickListener {
            startActivity(Intent(requireContext(), OrdersActivity::class.java))
        }
        view.findViewById<View>(R.id.row_account).setOnClickListener { showChangeAccountDialog() }
        view.findViewById<View>(R.id.row_lang).setOnClickListener { switchLanguage() }
        view.findViewById<View>(R.id.row_about).setOnClickListener { showAbout() }
        view.findViewById<View>(R.id.row_logout).setOnClickListener { confirmLogout() }

        loadProfile()
    }

    private fun loadProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { Repository.getUserById(Session.uid) }
            if (user == null) {
                Toast.makeText(requireContext(), R.string.login_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val name = user.displayName
            tvDisplayName.text = name
            tvAccount.text = getString(R.string.profile_account_label) + "：" + user.username
            tvAvatar.text = name.take(1).uppercase()
            etNickname.setText(user.nickname)
            etPhone.setText(user.phone)
            etAddress.setText(user.address)
        }
    }

    private fun saveProfile() {
        val nickname = etNickname.text?.toString().orEmpty()
        val phone = etPhone.text?.toString().orEmpty()
        val address = etAddress.text?.toString().orEmpty()
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                Repository.updateProfile(Session.uid, nickname, phone, address)
            }
            Toast.makeText(
                requireContext(),
                if (ok) R.string.profile_saved else R.string.login_failed,
                Toast.LENGTH_SHORT
            ).show()
            if (ok) {
                val display = nickname.takeIf { it.isNotBlank() } ?: Session.username
                tvDisplayName.text = display
                tvAvatar.text = display.take(1).uppercase()
            }
        }
    }

    /** 修改账号密码：弹窗内校验原密码，成功后同步会话与登录记忆。 */
    private fun showChangeAccountDialog() {
        val container = layoutInflater.inflate(R.layout.dialog_change_account, null)
        val etUsername = container.findViewById<TextInputEditText>(R.id.et_new_username)
        val etOldPwd = container.findViewById<TextInputEditText>(R.id.et_old_password)
        val etNewPwd = container.findViewById<TextInputEditText>(R.id.et_new_password)
        etUsername.setText(Session.username)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.change_account_title)
            .setView(container)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        // 用自定义点击事件拦截，校验不通过时不关闭弹窗
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                submitAccountChange(dialog, etUsername, etOldPwd, etNewPwd)
            }
        }
        dialog.show()
    }

    private fun submitAccountChange(
        dialog: AlertDialog,
        etUsername: TextInputEditText,
        etOldPwd: TextInputEditText,
        etNewPwd: TextInputEditText
    ) {
        val name = etUsername.text?.toString()?.trim().orEmpty()
        val oldPwd = etOldPwd.text?.toString().orEmpty()
        val newPwd = etNewPwd.text?.toString()?.trim().orEmpty()
        if (name.isEmpty() || oldPwd.isEmpty()) {
            toast(R.string.account_invalid)
            return
        }
        if (newPwd.isNotEmpty() && newPwd.length < 3) {
            toast(R.string.register_too_short)
            return
        }
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                Repository.updateCredentials(Session.uid, name, oldPwd, newPwd)
            }
            when (result) {
                CredentialResult.SUCCESS -> {
                    Session.updateUsername(name)
                    // 登录页记住的用户名同步更新
                    ctx.getSharedPreferences("account", Context.MODE_PRIVATE)
                        .edit().putString("last_username", name).apply()
                    tvAccount.text = getString(R.string.profile_account_label) + "：" + name
                    toast(R.string.account_updated)
                    dialog.dismiss()
                }
                CredentialResult.WRONG_PASSWORD -> toast(R.string.password_wrong)
                CredentialResult.USERNAME_TAKEN -> toast(R.string.username_taken)
                CredentialResult.INVALID -> toast(R.string.account_invalid)
            }
        }
    }

    private fun toast(resId: Int) {
        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
    }

    private fun showAbout() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun switchLanguage() {
        val ctx = context ?: return
        val next = if (LocaleManager.getLang(ctx) == "zh-CN") "en-US" else "zh-CN"
        LocaleManager.setLang(ctx, next)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(next))
        activity?.recreate()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.action_logout) { _, _ ->
                Session.logout()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
