package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

/**
 * 当前登录会话（保存在内存中，配合 SharedPreferences 记住上次登录的用户名）。
 */
object Session {
    var uid: Int = 0
        private set
    var username: String = ""
        private set

    val isLoggedIn: Boolean get() = uid != 0

    fun login(user: User) {
        uid = user.id
        username = user.username
    }

    /** 修改账号后同步内存中的用户名。 */
    fun updateUsername(name: String) {
        username = name
    }

    fun logout() {
        uid = 0
        username = ""
    }
}
