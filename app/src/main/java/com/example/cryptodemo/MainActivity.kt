package com.example.cryptodemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF126B58))) {
                CryptoDemo()
            }
        }
    }
}

@Composable
private fun CryptoDemo() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var store by remember { mutableStateOf<CryptoStore?>(null) }
    var token by remember { mutableStateOf("secret123") }
    var ciphertext by remember { mutableStateOf("Đang đọc file…") }
    var decoded by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Đang mở Android Keystore…") }
    var busy by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var showCode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val (opened, raw) = withContext(Dispatchers.IO) {
                val opened = CryptoStore(context)
                opened to opened.ciphertext()
            }
            store = opened
            ciphertext = raw
            status = "Sẵn sàng. Bấm lưu token mới hoặc đọc token đã lưu."
        } catch (error: Exception) {
            failed = true
            status = "Không mở được dữ liệu: ${error.localizedMessage}"
        } finally {
            busy = false
        }
    }

    fun operate(save: Boolean) {
        val currentStore = store ?: return
        val input = token
        keyboard?.hide()
        busy = true
        failed = false
        decoded = null
        scope.launch {
            try {
                val (value, raw) = withContext(Dispatchers.IO) {
                    val value = if (save) {
                        currentStore.save(input)
                        null
                    } else currentStore.read()
                    value to currentStore.ciphertext()
                }
                decoded = value
                ciphertext = raw
                status = when {
                    save -> "Đã mã hóa và ghi thành công vào file."
                    value == null -> "Chưa có token đã lưu."
                    else -> "Đã đọc và giải mã token từ dữ liệu đã lưu."
                }
            } catch (error: Exception) {
                failed = true
                status = "Thao tác thất bại: ${error.localizedMessage}"
            } finally {
                busy = false
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF3F6F4)) {
        Column(
            modifier = Modifier.safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("CryptoDemo", style = MaterialTheme.typography.headlineLarge)
            Text("Mã hóa dữ liệu lưu trên Android", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = token, onValueChange = { token = it },
                label = { Text("Token thử nghiệm") }, singleLine = true,
                enabled = !busy, modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { operate(true) }, enabled = !busy && store != null && token.isNotEmpty(),
                    modifier = Modifier.weight(1f)) { Text("Mã hóa & lưu") }
                OutlinedButton(onClick = { operate(false) }, enabled = !busy && store != null,
                    modifier = Modifier.weight(1f)) { Text("Đọc & giải mã") }
            }
            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(status, color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            DemoCard("Token đọc lại") {
                SelectionContainer {
                    Text(decoded ?: "Chưa đọc", style = MaterialTheme.typography.titleLarge)
                }
                decoded?.let {
                    Text(if (it == token) "✓ Khớp với ô nhập hiện tại" else "Khác với ô nhập hiện tại")
                }
            }
            DemoCard("Dữ liệu thực tế trong file") {
                Text("encrypted_prefs.xml • mục token", style = MaterialTheme.typography.labelLarge)
                SelectionContainer {
                    Text(ciphertext, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
                Text("Đọc trực tiếp tên và giá trị đã mã hóa trong XML. Hai mục keyset của thư viện được ẩn ở đây.",
                    style = MaterialTheme.typography.bodySmall)
            }
            DemoCard("Thuật toán đang sử dụng") {
                Text("Tên mục token → AES-256-SIV\nGiá trị token → AES-256-GCM\nMasterKey → AES-256-GCM trong Android Keystore")
                Text("MasterKey bảo vệ các bộ khóa. AES-GCM mã hóa giá trị và kiểm tra tính toàn vẹn.",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { showCode = !showCode }) {
                    Text(if (showCode) "Ẩn cấu hình Kotlin" else "Xem cấu hình Kotlin")
                }
                if (showCode) SelectionContainer {
                    Text("MasterKey.KeyScheme.AES256_GCM\nPrefKeyEncryptionScheme.AES256_SIV\nPrefValueEncryptionScheme.AES256_GCM",
                        fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Thử lưu một token tự chọn → Force stop app → mở lại → bấm Đọc & giải mã. App chỉ ghi khi bạn bấm Lưu.",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DemoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
