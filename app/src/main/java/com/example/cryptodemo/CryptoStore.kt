package com.example.cryptodemo

import android.content.Context
import android.util.Xml
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.xmlpull.v1.XmlPullParser
import java.io.File

// The encryption configuration is kept here for the classroom demonstration.
class CryptoStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context, "encrypted_prefs", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val file = File(context.applicationInfo.dataDir, "shared_prefs/encrypted_prefs.xml")

    fun save(token: String) {
        check(prefs.edit().putString("token", token).commit()) { "Không ghi được file preferences" }
    }

    fun read(): String? = prefs.getString("token", null)

    fun ciphertext(): String {
        if (!file.exists()) return "Chưa có dữ liệu đã lưu."
        // Read the XML itself: EncryptedSharedPreferences.getString() would decrypt it.
        file.reader().use { reader ->
            val parser = Xml.newPullParser()
            parser.setInput(reader)
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "string") {
                    val name = parser.getAttributeValue(null, "name")
                    val value = parser.nextText()
                    if (!name.startsWith("__androidx_security_crypto_")) {
                        return "Tên mục đã mã hóa\n$name\n\nGiá trị đã mã hóa\n$value"
                    }
                }
            }
        }
        return "Chưa có token đã lưu."
    }
}
