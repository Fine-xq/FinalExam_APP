package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

interface UserDao {
    suspend fun register(user: User): Boolean
    suspend fun login(username: String, password: String): User?
    suspend fun findUserByName(username: String): User?
    suspend fun getUserById(userId: Int): User?
    suspend fun updateProfile(
        userId: Int,
        nickname: String,
        phone: String,
        address: String
    ): Boolean
}
