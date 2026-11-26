package cn.edu.sicnu.cs.stu.xiaoquan.myapplication_finalexam

/** 修改账号密码的结果。 */
enum class CredentialResult { SUCCESS, WRONG_PASSWORD, USERNAME_TAKEN, INVALID }

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

    /** 校验原密码后修改账号名；newPassword 为空表示只改账号。 */
    suspend fun updateCredentials(
        userId: Int,
        newUsername: String,
        oldPassword: String,
        newPassword: String
    ): CredentialResult
}
