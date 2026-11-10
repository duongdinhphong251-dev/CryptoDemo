package com.example.cryptodemo

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val prefs = EncryptedSharedPreferences.create(
        context, "encrypted_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val secretKey: SecretKey = run {
        val alias = "cryptodemo_export_key"
        MasterKey.Builder(context, alias).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey(alias, null) as SecretKey
    }
    private val associatedData = "CryptoDemo:CD1".toByteArray(Charsets.UTF_8)

    fun encrypt(token: String): String {
        require(token.isNotEmpty()) { "Hãy nhập token cần mã hóa." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        cipher.updateAAD(associatedData)
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        // CD1 encodes a 12-byte IV followed by ciphertext and its 16-byte tag.
        check(cipher.iv.size == 12)
        val output = "CD1." + Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        check(prefs.edit().putString("token", token).putString("exported_ciphertext", output).commit()) {
            "Chưa lưu được kết quả. Hãy thử mã hóa lại."
        }
        return output
    }

    fun decrypt(input: String): String {
        val compact = input.filterNot { it.isWhitespace() }
        require(compact.startsWith("CD1.")) { "Bản mã phải bắt đầu bằng CD1. Hãy copy toàn bộ kết quả." }
        val encoded = compact.removePrefix("CD1.")
        require(encoded.isNotEmpty() && encoded.length % 4 == 0 &&
            encoded.matches(Regex("[A-Za-z0-9+/]*={0,2}"))) { "Bản mã có định dạng Base64 không hợp lệ." }
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size >= 28) { "Bản mã bị thiếu nội dung." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        cipher.updateAAD(associatedData)
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }

    fun savedCiphertext(): String? = prefs.getString("exported_ciphertext", null)

    // Existing stored tokens remain readable.
    fun read(): String? = prefs.getString("token", null)
}
