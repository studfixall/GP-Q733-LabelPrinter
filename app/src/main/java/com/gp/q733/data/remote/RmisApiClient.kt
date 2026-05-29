package com.gp.q733.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * RMIS API 客户端 — Issue #13
 * 封装昂捷RMIS对外接口的认证和请求逻辑
 *
 * 认证流程：
 * 1. DES-ECB-Zeros 加密 (8位随机数+应用程序编码) → Token
 * 2. 获取令牌接口 → ObjectData (DES加密的令牌)
 * 3. DES解密 ObjectData → 令牌明文
 * 4. Session-Key = MD5(令牌明文 + JSON请求体)
 * 5. Header: Session-Key=<value>
 */
class RmisApiClient(
    private var baseUrl: String,
    private var userNo: String,
    private var masterKey: String
) {
    companion object {
        private const val TAG = "RmisApiClient"
        private const val CONNECT_TIMEOUT = 15000
        private const val READ_TIMEOUT = 30000
    }

    /** 缓存的令牌明文 */
    private var sessionKey: String? = null
    private var tokenExpiry: Long = 0

    /** 更新连接配置（门店切换/设置变更时调用） */
    fun updateConfig(baseUrl: String, userNo: String, masterKey: String) {
        this.baseUrl = baseUrl
        this.userNo = userNo
        this.masterKey = masterKey
        // 配置变更，令牌失效
        this.sessionKey = null
        this.tokenExpiry = 0
    }

    // ==================== DES 加密/解密 ====================

    /**
     * DES-ECB-Zeros 加密
     * 算法: DES/ECB/ZerosPadding, 8字节key, ASCII输入, Base64输出
     */
    fun desEncrypt(plaintext: String, key: String): String {
        val keyBytes = ByteArray(8)
        val keyInput = key.toByteArray(Charsets.US_ASCII)
        System.arraycopy(keyInput, 0, keyBytes, 0, minOf(keyInput.size, 8))

        val cipher = Cipher.getInstance("DES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "DES"))

        // Zeros padding: 补齐到8的倍数
        val input = plaintext.toByteArray(Charsets.US_ASCII)
        val paddedLen = if (input.size % 8 == 0) input.size else (input.size / 8 + 1) * 8
        val padded = ByteArray(paddedLen)
        System.arraycopy(input, 0, padded, 0, input.size)

        val encrypted = cipher.doFinal(padded)
        return android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
    }

    /**
     * DES-ECB-Zeros 解密
     * Base64输入 → DES解密 → 去除尾部\0 → ASCII字符串
     */
    fun desDecrypt(ciphertext: String, key: String): String {
        val keyBytes = ByteArray(8)
        val keyInput = key.toByteArray(Charsets.US_ASCII)
        System.arraycopy(keyInput, 0, keyBytes, 0, minOf(keyInput.size, 8))

        val cipher = Cipher.getInstance("DES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "DES"))

        val decoded = android.util.Base64.decode(ciphertext, android.util.Base64.NO_WRAP)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted, Charsets.US_ASCII).trimEnd('\u0000')
    }

    /**
     * MD5 哈希 — UTF-8编码输入, 小写hex输出
     */
    fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    // ==================== 令牌管理 ====================

    /**
     * 获取令牌
     * 1. 生成8位随机数
     * 2. Token = DesE(随机数+UserNo, MasterKey)
     * 3. POST 获取令牌接口
     * 4. DesD(ObjectData, MasterKey) → 令牌明文
     */
    private suspend fun ensureToken(): String = withContext(Dispatchers.IO) {
        if (sessionKey != null && System.currentTimeMillis() < tokenExpiry) {
            return@withContext sessionKey!!
        }

        val random8 = (10000000..99999999).random().toString()
        val tokenPayload = random8 + userNo
        val encryptedToken = desEncrypt(tokenPayload, masterKey)

        val requestJson = JSONObject().apply {
            put("UniqueKey", "获取令牌")
            put("ClientTime", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'")
                .format(java.util.Date()))
            put("UserNo", userNo)
            put("Token", encryptedToken)
        }

        val response = postRequest(requestJson.toString())
        val respJson = JSONObject(response)
        val objectData = respJson.optString("ObjectData", "")

        if (objectData.isEmpty()) {
            val exception = respJson.optJSONObject("Exception")
            val msg = exception?.optString("Message") ?: "获取令牌失败"
            throw RmisException("令牌获取失败: $msg")
        }

        val token = desDecrypt(objectData, masterKey)
        Log.d(TAG, "令牌获取成功: ${token.take(4)}****")
        // 缓存7天
        this@RmisApiClient.sessionKey = token
        this@RmisApiClient.tokenExpiry = System.currentTimeMillis() + 7 * 24 * 3600 * 1000L
        token
    }

    // ==================== 通用请求 ====================

    /**
     * 执行RMIS接口调用
     * @param uniqueKey 接口方法名
     * @param objectData 请求数据(JSONObject/JSONArray/String)
     * @return 返回的ObjectData(JSONObject)
     */
    suspend fun call(uniqueKey: String, objectData: Any): JSONObject = withContext(Dispatchers.IO) {
        val token = ensureToken()

        val requestJson = JSONObject().apply {
            put("UniqueKey", uniqueKey)
            put("ObjectData", objectData)
            put("ClientTime", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(java.util.Date()))
            put("UserNo", userNo)
        }

        val requestStr = requestJson.toString()
        val sessionKeyHash = md5(token + requestStr)

        val response = postRequest(requestStr, sessionKeyHash)
        val respJson = JSONObject(response)

        // 检查异常
        val hasException = respJson.optBoolean("HasException", false)
        if (hasException) {
            val exception = respJson.optJSONObject("Exception")
            val code = exception?.optString("Code", "-1") ?: "-1"
            val message = exception?.optString("Message", "未知错误") ?: "未知错误"

            // 令牌失效，清除缓存重试一次
            if (code == "004") {
                Log.w(TAG, "令牌失效，重新获取")
                this@RmisApiClient.sessionKey = null
                this@RmisApiClient.tokenExpiry = 0
                val retryToken = ensureToken()
                val retryHash = md5(retryToken + requestStr)
                val retryResponse = postRequest(requestStr, retryHash)
                val retryRespJson = JSONObject(retryResponse)
                val retryHasException = retryRespJson.optBoolean("HasException", false)
                if (retryHasException) {
                    val retryEx = retryRespJson.optJSONObject("Exception")
                    throw RmisException("接口调用失败[${uniqueKey}]: ${retryEx?.optString("Message") ?: "未知错误"}")
                }
                return@withContext retryRespJson.optJSONObject("ObjectData")
                    ?: retryRespJson
            }

            throw RmisException("接口调用失败[${uniqueKey}]: $message (code=$code)")
        }

        respJson.optJSONObject("ObjectData") ?: respJson
    }

    /**
     * 执行分页查询
     */
    suspend fun callPaged(
        uniqueKey: String,
        tag: JSONObject,
        page: Int = 1,
        pageSize: Int = 100
    ): JSONObject {
        val objectData = JSONObject().apply {
            put("IsNotPage", false)
            put("CurrentPage", page)
            put("PageSize", pageSize)
            put("Tag", tag)
        }
        return call(uniqueKey, objectData)
    }

    // ==================== HTTP ====================

    private fun postRequest(jsonBody: String, sessionKey: String? = null): String {
        val url = URL("${baseUrl.trimEnd('/')}/Service")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        if (sessionKey != null) {
            conn.setRequestProperty("Session-Key", sessionKey)
        }
        conn.doOutput = true

        Log.d(TAG, "POST ${url} | UniqueKey=${JSONObject(jsonBody).optString("UniqueKey")}")

        conn.outputStream.use { os ->
            os.write(jsonBody.toByteArray(Charsets.UTF_8))
        }

        val responseCode = conn.responseCode
        if (responseCode != 200) {
            val errorStream = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw RmisException("HTTP $responseCode: $errorStream")
        }

        val response = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        conn.disconnect()
        return response
    }
}

class RmisException(message: String) : Exception(message)
