package com.example.cryptodemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.AEADBadTagException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF126B58))) { CryptoDemo() }
        }
    }
}

@Composable
private fun CryptoDemo() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var store by remember { mutableStateOf<CryptoStore?>(null) }
    var token by remember { mutableStateOf("secret123") }
    var output by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var decoded by remember { mutableStateOf<String?>(null) }
    var encryptStatus by remember { mutableStateOf("") }
    var decryptStatus by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(true) }
    var decryptFailed by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val (opened, saved) = withContext(Dispatchers.IO) {
                val opened = CryptoStore(context)
                opened to opened.savedCiphertext()
            }
            store = opened
            output = saved.orEmpty()
            encryptStatus = if (saved == null) "Sẵn sàng để mã hóa." else "Đã nạp bản mã từ lần lưu trước."
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            encryptStatus = "Không mở được khóa hoặc dữ liệu: ${error.localizedMessage}"
        } finally {
            busy = false
        }
    }

    fun changeInput(value: String) {
        input = value
        decoded = null
        decryptStatus = ""
        decryptFailed = false
    }

    fun operate(encrypt: Boolean) {
        if (busy) return
        val currentStore = store ?: return
        val value = if (encrypt) token else input
        keyboard?.hide()
        busy = true
        if (encrypt) {
            encryptStatus = "Đang mã hóa…"
        } else {
            decoded = null
            decryptFailed = false
            decryptStatus = "Đang kiểm tra và giải mã…"
        }
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    if (encrypt) currentStore.encrypt(value) else currentStore.decrypt(value)
                }
                if (encrypt) {
                    output = result
                    encryptStatus = "Đã mã hóa và lưu. Copy bản mã để chuyển sang phần giải mã."
                } else {
                    decoded = result
                    decryptStatus = "✓ Giải mã và kiểm tra tính toàn vẹn thành công."
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (encrypt) {
                    encryptStatus = "Mã hóa thất bại: ${error.localizedMessage}"
                } else {
                    decryptFailed = true
                    decryptStatus = if (error is AEADBadTagException)
                        "Không giải mã được: bản mã đã bị sửa hoặc được tạo bằng khóa khác."
                    else "Không giải mã được: ${error.localizedMessage}"
                }
            } finally {
                busy = false
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = Color(0xFFF3F6F4)) {
        Column(
            Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("CryptoDemo", style = MaterialTheme.typography.headlineLarge)
            Text("Token → Bản mã → Copy → Dán → Giải mã", style = MaterialTheme.typography.bodyMedium)
            Text("Khóa AES-256 được app giữ trong Android Keystore. Bản mã dùng lại được trên cùng bản cài app này.",
                style = MaterialTheme.typography.bodySmall)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            DemoCard("Mã hóa", Color(0xFFE4F2EC)) {
                OutlinedTextField(
                    token, { token = it }, label = { Text("Token cần mã hóa") },
                    enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Button({ operate(true) }, enabled = !busy && store != null && token.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()) { Text("Mã hóa token") }
                Text(encryptStatus, style = MaterialTheme.typography.bodySmall)
                if (output.isNotEmpty()) {
                    Text("Bản mã vừa tạo hoặc đã lưu", style = MaterialTheme.typography.labelLarge)
                    SelectionContainer {
                        Text(output, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton({
                        clipboard.setText(AnnotatedString(output))
                        encryptStatus = "Đã copy bản mã. Bấm Dán bản mã ở phần giải mã."
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Copy bản mã") }
                }
            }
            DemoCard("Giải mã") {
                Text("Dán bản mã vào ô bên dưới rồi bấm Giải mã bản mã.",
                    style = MaterialTheme.typography.bodyMedium)
                TextButton({
                    val pasted = clipboard.getText()?.text
                    if (pasted.isNullOrBlank()) {
                        decryptStatus = "Clipboard chưa có bản mã. Hãy bấm Copy bản mã trước."
                    } else changeInput(pasted)
                }, enabled = !busy) { Text("Dán bản mã") }
                OutlinedTextField(
                    input, { changeInput(it) }, label = { Text("Bản mã cần giải mã") },
                    enabled = !busy, minLines = 3, maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Button({ operate(false) }, enabled = !busy && store != null && input.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()) { Text("Giải mã bản mã") }
                if (decryptStatus.isNotEmpty()) Text(
                    decryptStatus, style = MaterialTheme.typography.bodySmall,
                    color = if (decryptFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text("Token khôi phục", style = MaterialTheme.typography.labelLarge)
                SelectionContainer {
                    Text(decoded ?: "Chưa giải mã", style = MaterialTheme.typography.titleLarge)
                }
            }
            Text("Thử sửa một ký tự trong bản mã rồi giải mã lại để kiểm tra phát hiện thay đổi.",
                style = MaterialTheme.typography.bodySmall)
            TextButton({ showDetails = !showDetails }) {
                Text(if (showDetails) "Ẩn cách mã hóa hoạt động" else "Xem cách mã hóa hoạt động")
            }
            if (showDetails) DemoCard("Thuật toán và lưu trữ") {
                Text("Bản mã copy/dán dùng AES-256-GCM của Android Cipher.")
                Text("CD1. + Base64(nonce 12 byte + dữ liệu mã hóa + tag 16 byte)",
                    fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                Text("Giải mã dùng chính bản mã bạn nhập và khóa trong Keystore, đồng thời xác thực tag.")
                Text("Token và bản mã gần nhất được lưu bằng EncryptedSharedPreferences: AES-256-SIV cho tên mục, AES-256-GCM cho giá trị. MasterKey bảo vệ các bộ khóa.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DemoCard(title: String, color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}
