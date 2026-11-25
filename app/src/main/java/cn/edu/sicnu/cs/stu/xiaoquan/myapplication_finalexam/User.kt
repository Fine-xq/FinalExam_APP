package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

import java.io.Serializable

data class User(
    val id: Int = 0,
    val username: String,
    val password: String,
    val nickname: String = "",
    val phone: String = "",
    val address: String = ""
) : Serializable {
    /** 展示用名称：优先昵称，其次用户名。 */
    val displayName: String get() = nickname.takeIf { it.isNotBlank() } ?: username

    companion object {
        private const val serialVersionUID = 1L
    }
}
