package com.example.cryptodemo

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CryptoDemoTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun ready() {
        compose.waitUntil(30_000) {
            compose.onAllNodes(hasText("Mã hóa token") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun decrypt(input: String) {
        compose.onNodeWithText("Bản mã cần giải mã").performScrollTo().performTextReplacement(input)
        compose.onNodeWithText("Giải mã bản mã").performScrollTo().performClick()
        ready()
    }

    @Test fun copyPasteDecryptAndRejectTampering() {
        ready()
        val token = "Demo-47-tiếng-Việt"
        compose.onNodeWithText("Giải mã bản mã").assertIsNotEnabled()
        compose.onNodeWithText("Token cần mã hóa").performScrollTo().performTextReplacement("")
        compose.onNodeWithText("Mã hóa token").assertIsNotEnabled()
        compose.onNodeWithText("Token cần mã hóa").performTextReplacement(token)
        compose.onNodeWithText("Mã hóa token").performClick()
        ready()
        compose.onNodeWithText("Copy bản mã").performScrollTo().performClick()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var exported = ""
        compose.runOnIdle {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            exported = clipboard.primaryClip!!.getItemAt(0).text.toString()
        }
        assertTrue(exported.startsWith("CD1."))
        assertFalse(exported.contains(token))
        compose.onNodeWithText("Dán bản mã").performScrollTo().performClick()
        compose.onNodeWithText("Bản mã cần giải mã").assertTextContains(exported)
        compose.onNodeWithText("Giải mã bản mã").performScrollTo().performClick()
        ready()
        compose.onNodeWithText("✓ Giải mã và kiểm tra tính toàn vẹn thành công.").assertExists()

        // Generating a new token must not change what an older pasted ciphertext decrypts to.
        compose.onNodeWithText("Token cần mã hóa").performScrollTo().performTextReplacement("Another-token")
        compose.onNodeWithText("Mã hóa token").performClick()
        ready()
        compose.activityRule.scenario.recreate()
        ready()
        compose.onNodeWithText("Chưa giải mã").assertExists()
        decrypt("  " + exported.chunked(16).joinToString("\n") + "  ")
        compose.onNodeWithText(token).assertExists()

        // Modify an actual tag byte, keeping a valid Base64 envelope.
        val bytes = Base64.decode(exported.removePrefix("CD1."), Base64.NO_WRAP)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        decrypt("CD1." + Base64.encodeToString(bytes, Base64.NO_WRAP))
        compose.onNodeWithText("Không giải mã được: bản mã đã bị sửa hoặc được tạo bằng khóa khác.").assertExists()
        compose.onNodeWithText("Chưa giải mã").assertExists()
        decrypt("invalid-ciphertext")
        compose.onNodeWithText("Không giải mã được:", substring = true).assertExists()
        compose.onNodeWithText("Chưa giải mã").assertExists()
        decrypt(exported)
        compose.onNodeWithText(token).assertExists()

        val file = File(context.applicationInfo.dataDir, "shared_prefs/encrypted_prefs.xml")
        assertFalse(file.readText().contains("Another-token"))
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        compose.onNodeWithText("CryptoDemo").performScrollTo()
        File(context.getExternalFilesDir(null), "demo-screen.png").outputStream().use {
            automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Token khôi phục").performScrollTo()
        File(context.getExternalFilesDir(null), "demo-decrypt.png").outputStream().use {
            automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
