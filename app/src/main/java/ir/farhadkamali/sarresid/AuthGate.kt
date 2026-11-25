package ir.farhadkamali.sarresid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

@Composable
fun AuthGate(content: @Composable () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    var checking by remember { mutableStateOf(true) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
            checking = false
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    when {
        checking -> Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
        }
        user != null -> content()
        else -> SignInScreen(auth)
    }
}

private fun authErrorMessage(error: Exception?): String {
    if (error is FirebaseNetworkException) {
        return "اتصال اینترنت برقرار نیست. اینترنت را بررسی کن و دوباره تلاش کن. (network-error)"
    }

    val code = (error as? FirebaseAuthException)?.errorCode ?: "unknown"
    return when (code) {
        "ERROR_INVALID_EMAIL" -> "فرمت ایمیل صحیح نیست. (invalid-email)"
        "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL" ->
            "این ایمیل در همین پروژه Firebase حساب قابل ورود ندارد یا رمز عبور اشتباه است. (invalid-credential)"
        "ERROR_WRONG_PASSWORD" -> "رمز عبور اشتباه است. (wrong-password)"
        "ERROR_USER_DISABLED" -> "این حساب غیرفعال شده است. (user-disabled)"
        "ERROR_OPERATION_NOT_ALLOWED" ->
            "ورود با ایمیل و رمز عبور در Firebase فعال نیست. (operation-not-allowed)"
        "ERROR_TOO_MANY_REQUESTS" -> "تلاش‌های ناموفق زیاد بوده؛ کمی بعد دوباره امتحان کن. (too-many-requests)"
        "ERROR_EMAIL_ALREADY_IN_USE" -> "این ایمیل قبلاً حساب دارد؛ از دکمه ورود استفاده کن. (email-already-in-use)"
        "ERROR_WEAK_PASSWORD" -> "رمز عبور باید حداقل ۶ کاراکتر باشد. (weak-password)"
        "ERROR_NETWORK_REQUEST_FAILED" -> "ارتباط با Firebase برقرار نشد؛ اینترنت را بررسی کن. (network-request-failed)"
        else -> "ورود انجام نشد. کد خطا: $code"
    }
}

@Composable
private fun SignInScreen(auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("سررسید", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("برای ورود یا ساخت حساب، ایمیل و رمز عبور را وارد کن.")
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ایمیل") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("رمز عبور (حداقل ۶ کاراکتر)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            if (busy) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val cleanEmail = email.trim().lowercase()
                        if (cleanEmail.isBlank() || password.length < 6) {
                            message = "ایمیل معتبر و رمز عبور حداقل ۶ کاراکتری وارد کن."
                        } else {
                            busy = true
                            message = ""
                            auth.signInWithEmailAndPassword(cleanEmail, password)
                                .addOnCompleteListener { task ->
                                    busy = false
                                    if (!task.isSuccessful) message = authErrorMessage(task.exception)
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("ورود") }
                TextButton(
                    onClick = {
                        val cleanEmail = email.trim().lowercase()
                        if (cleanEmail.isBlank() || password.length < 6) {
                            message = "برای ساخت حساب، ایمیل معتبر و رمز حداقل ۶ کاراکتری وارد کن."
                        } else {
                            busy = true
                            message = ""
                            auth.createUserWithEmailAndPassword(cleanEmail, password)
                                .addOnCompleteListener { task ->
                                    busy = false
                                    if (!task.isSuccessful) message = authErrorMessage(task.exception)
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ساخت حساب جدید") }
            }
            if (message.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
