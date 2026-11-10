# CryptoDemo

Mini-project môn **Introduction to Cryptography**, minh họa mã hóa token và bảo vệ dữ liệu lưu trên Android.

**Đề tài:** Data-at-Rest Encryption in Android Applications Using EncryptedSharedPreferences.

Ứng dụng có giao diện Jetpack Compose để nhập token, mã hóa, copy bản mã, dán vào ô giải mã và khôi phục token. Token và bản mã gần nhất được lưu bằng EncryptedSharedPreferences.

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | Kotlin |
| Giao diện | Jetpack Compose, Material Design 3 |
| Mã hóa bản mã copy/dán | Android Cipher, AES-256-GCM |
| Lưu trữ | AndroidX EncryptedSharedPreferences 1.1.0 |
| Mã hóa tên mục lưu trữ | AES-256-SIV |
| Mã hóa giá trị lưu trữ | AES-256-GCM |
| Quản lý khóa | MasterKey, Android Keystore |
| Xử lý bất đồng bộ | Kotlin Coroutines, Dispatchers.IO |
| Package | `com.example.cryptodemo` |

## Chạy ứng dụng

1. Mở thư mục project bằng Android Studio và đợi Gradle Sync hoàn tất.
2. Cài Android SDK Platform 37 theo cấu hình `compileSdk` và `targetSdk` hiện tại. Gradle daemon của project dùng JDK 25.
3. Chọn emulator hoặc thiết bị Android API 26 trở lên. Thiết bị đã dùng để kiểm thử là Pixel 6 API 36.
4. Đợi thiết bị khởi động xong, chọn cấu hình **app** và bấm **Run**.

Nếu emulator báo timeout khi deploy, kiểm tra thiết bị đã boot xong. Với Pixel 6 từng gặp treo đồ họa, có thể chọn **Software Graphics** và **Cold Boot** trong Device Manager rồi chạy lại.

Project đã có `android.overridePathCheck=true` để cho phép đường dẫn chứa tiếng Việt trên Windows. Nếu `gradlew.bat` vẫn lỗi đường dẫn, chạy Gradle Wrapper bằng JDK 25 từ thư mục gốc trong PowerShell:

```powershell
& "$env:JAVA_HOME/bin/java.exe" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug
```

`JAVA_HOME` cần trỏ đến thư mục JDK 25 trên máy đang chạy.

## Demo mã hóa và giải mã

```text
Nhập token
    ↓
Mã hóa token
    ↓
Copy bản mã
    ↓
Dán bản mã vào phần Giải mã
    ↓
Giải mã bản mã
    ↓
Hiển thị Token khôi phục
```

1. Nhập `secret123` vào ô **Token cần mã hóa**.
2. Bấm **Mã hóa token**. Kết quả là một chuỗi bắt đầu bằng `CD1.`.
3. Bấm **Copy bản mã**.
4. Cuộn xuống khu vực **Giải mã**, bấm **Dán bản mã**.
5. Bấm **Giải mã bản mã**. Mục **Token khôi phục** hiển thị `secret123` cùng thông báo xác thực thành công.

Phần giải mã xử lý nội dung trong ô **Bản mã cần giải mã**. Có thể dán bản mã cũ để khôi phục token cũ dù đã tạo bản mã mới.

### Kiểm chứng tính toàn vẹn

Sửa một ký tự ở giữa phần Base64 của bản mã rồi bấm giải mã. Khi thay đổi làm dữ liệu xác thực không hợp lệ, app báo lỗi và xóa kết quả giải mã cũ. Chuỗi sai định dạng hoặc thiếu tiền tố cũng bị từ chối.

Bài kiểm tra tự động sửa trực tiếp một byte trong tag rồi mã hóa Base64 lại để kiểm chứng lỗi xác thực với một đầu vào đúng định dạng.

### Kiểm chứng dữ liệu qua lần chạy mới

1. Mã hóa một token và giữ bản mã đã copy.
2. Force stop ứng dụng, sau đó mở lại.
3. Dán bản mã và giải mã. Nếu clipboard đã mất nội dung, copy lại bản mã được nạp ở khu vực Mã hóa.
4. Kiểm tra token khôi phục có đúng nội dung trước đó.

Khóa giải mã gắn với bản cài ứng dụng trên thiết bị. Gỡ ứng dụng hoặc xóa dữ liệu có thể làm mất khóa cần để giải mã các bản mã cũ.

## Cơ chế mật mã

Ứng dụng có hai phần xử lý phục vụ việc trình diễn và lưu trữ.

### Bản mã copy và dán

`CryptoStore.encrypt()` chuyển token sang UTF-8 và gọi `Cipher` với `AES/GCM/NoPadding`.

- Khóa AES 256 bit được quản lý trong Android Keystore dưới alias `cryptodemo_export_key`.
- Cipher sinh nonce 12 byte cho mỗi lần mã hóa.
- Kết quả gồm dữ liệu mã hóa và tag xác thực 16 byte.
- Dữ liệu liên kết (AAD) là chuỗi UTF-8 `CryptoDemo:CD1`.

Định dạng đầu ra:

```text
CD1. + Base64(nonce 12 byte || dữ liệu mã hóa || tag 16 byte)
```

`||` biểu thị nối các dãy byte. Base64 biểu diễn các byte bằng ký tự để thuận tiện copy/dán.

`CryptoStore.decrypt()` kiểm tra định dạng, tách nonce, sử dụng khóa và cùng AAD để giải mã và xác thực tag. Đầu ra hợp lệ được chuyển từ UTF-8 thành token. Khoảng trắng và xuống dòng trong chuỗi bản mã được bỏ qua khi xử lý.

### Dữ liệu lưu trên thiết bị

Sau khi mã hóa thành công, token và bản mã được ghi cùng một lần `commit()` vào EncryptedSharedPreferences:

| Tên mục trước khi mã hóa | Nội dung |
| --- | --- |
| `token` | Token vừa nhập |
| `exported_ciphertext` | Chuỗi bản mã `CD1.` gần nhất |

Lớp lưu trữ mã hóa tên mục bằng AES-256-SIV và giá trị bằng AES-256-GCM. Một MasterKey riêng bảo vệ các bộ khóa lưu trữ. Các thuật toán được gọi thông qua API Android và AndroidX.

Để xem file trên bản debug, mở **Device Explorer** trong Android Studio và tìm:

```text
/data/data/com.example.cryptodemo/shared_prefs/encrypted_prefs.xml
```

File chứa dữ liệu mã hóa cùng các mục keyset được bảo vệ. Chuỗi `CD1.` hiển thị trên giao diện là kết quả của luồng Cipher; bản lưu của chuỗi đó trong XML tiếp tục được lớp EncryptedSharedPreferences mã hóa.

## Cấu trúc code

```text
app/src/main/java/com/example/cryptodemo/
├── MainActivity.kt          Giao diện, trạng thái, copy/dán và coroutine
└── CryptoStore.kt           Mã hóa, giải mã, quản lý khóa và lưu trữ

app/src/androidTest/java/com/example/cryptodemo/
└── CryptoDemoTest.kt        Kiểm thử luồng thao tác và xác thực bản mã
```

Các hàm chính trong `CryptoStore`:

- `encrypt(token)` tạo bản mã và lưu token cùng kết quả gần nhất.
- `decrypt(input)` giải mã bản mã được truyền vào.
- `savedCiphertext()` nạp bản mã đã lưu để hiển thị khi mở app.
- `read()` đọc token đã lưu trong EncryptedSharedPreferences.

## Kiểm thử

Mở `CryptoDemoTest.kt` trong Android Studio và chạy bài kiểm tra trên emulator hoặc thiết bị. Có thể chạy từ PowerShell:

```powershell
& "$env:JAVA_HOME/bin/java.exe" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:connectedDebugAndroidTest
```

Bài kiểm tra hiện tại bao gồm:

- Khóa nút thao tác khi ô nhập rỗng.
- Mã hóa token tiếng Việt, copy và dán qua clipboard, giải mã thành công.
- Giải mã bản mã cũ sau khi tạo token mới và tạo lại màn hình.
- Xử lý bản mã có khoảng trắng hoặc xuống dòng.
- Từ chối tag bị sửa và chuỗi sai định dạng; xóa kết quả giải mã cũ khi có lỗi.
- Kiểm tra token thử nghiệm mới không xuất hiện dạng rõ trong XML mã hóa.

EncryptedSharedPreferences và MasterKey phát sinh cảnh báo deprecated khi build với dependency hiện tại. Chúng được sử dụng để minh họa cơ chế lưu trữ theo đề tài của project.
