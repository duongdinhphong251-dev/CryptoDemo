package com.example.cryptodemo

import android.graphics.Bitmap
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
            compose.onAllNodes(hasText("Đọc & giải mã") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun saveReadAndReopen() {
        ready()
        val token = "Demo-47-tiếng-Việt"
        compose.onNodeWithText("Token thử nghiệm").performTextReplacement("")
        compose.onNodeWithText("Mã hóa & lưu").assertIsNotEnabled()
        compose.onNodeWithText("Token thử nghiệm").performTextReplacement(token)
        compose.onNodeWithText("Mã hóa & lưu").performClick()
        ready()
        compose.onNodeWithText("Đã mã hóa và ghi thành công vào file.").assertExists()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.applicationInfo.dataDir, "shared_prefs/encrypted_prefs.xml")
        val first = file.readText()
        assertFalse(first.contains(token))
        assertFalse(first.contains("name=\"token\""))
        compose.onNodeWithText("Đọc & giải mã").performClick()
        ready()
        compose.onNodeWithText("✓ Khớp với ô nhập hiện tại").assertExists()
        // Saving the same plaintext again should produce a new GCM ciphertext.
        compose.onNodeWithText("Mã hóa & lưu").performClick()
        ready()
        val second = file.readText()
        assertNotEquals(first, second)
        compose.activityRule.scenario.recreate()
        ready()
        assertEquals(second, file.readText())
        compose.onNodeWithText("Chưa đọc").assertExists()
        compose.onNodeWithText("Đọc & giải mã").performClick()
        ready()
        compose.onNodeWithText(token).assertExists()
        compose.onNodeWithText("Khác với ô nhập hiện tại").assertExists()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        File(context.getExternalFilesDir(null), "demo-screen.png").outputStream().use {
            automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Xem cấu hình Kotlin").performScrollTo().performClick()
        compose.onNodeWithText("MasterKey.KeyScheme.AES256_GCM", substring = true)
            .performScrollTo().assertIsDisplayed()
        File(context.getExternalFilesDir(null), "demo-algorithms.png").outputStream().use {
            automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
