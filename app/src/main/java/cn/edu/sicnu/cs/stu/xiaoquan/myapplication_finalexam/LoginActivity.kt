package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import cn.edu.sicnu.cs.xiaoquan.myapplication_finalexam.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : BaseActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = getSharedPreferences("account", MODE_PRIVATE)
        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        etUsername.setText(prefs.getString("last_username", ""))

        findViewById<MaterialButton>(R.id.btn_login).setOnClickListener { doLogin() }
        findViewById<MaterialButton>(R.id.btn_register).setOnClickListener { doRegister() }
    }

    private fun doLogin() {
        val name = etUsername.text?.toString()?.trim().orEmpty()
        val pwd = etPassword.text?.toString()?.trim().orEmpty()
        if (name.isEmpty() || pwd.isEmpty()) {
            toast(R.string.login_empty)
            return
        }
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { Repository.login(name, pwd) }
            if (user == null) {
                toast(R.string.login_failed)
            } else {
                enter(user)
            }
        }
    }

    private fun doRegister() {
        val name = etUsername.text?.toString()?.trim().orEmpty()
        val pwd = etPassword.text?.toString()?.trim().orEmpty()
        if (name.isEmpty() || pwd.isEmpty()) {
            toast(R.string.login_empty)
            return
        }
        if (pwd.length < 3) {
            toast(R.string.register_too_short)
            return
        }
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                val registered = Repository.register(User(username = name, password = pwd))
                if (registered) Repository.login(name, pwd) else null
            }
            if (user == null) {
                toast(R.string.register_failed)
            } else {
                toast(R.string.register_success)
                enter(user)
            }
        }
    }

    private fun enter(user: User) {
        Session.login(user)
        prefs.edit().putString("last_username", user.username).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }
}
