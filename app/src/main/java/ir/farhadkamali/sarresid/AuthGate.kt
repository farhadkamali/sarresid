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
fun AuthGate(content: @Composable (String) -> Unit) {
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
        user != null -> content(user!!.uid)
        else -> SignInScreen(auth)
    }
}

private fun signInErrorMessage(error: Exception?): String {
    if (error is FirebaseNetworkException) {
        return "ارتباط اینترنت برقرار نیست؛ اتصال را بررسی کن و دوباره تلاش کن."
    }

    val code = (error as? FirebaseAuthException)?.errorCode
    return when (code) {
        "ERROR_INVALID_EMAIL" -> "فرمت ایمیل صحیح نیست."
        "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD" ->
            "ایمیل یا رمز عبور صحیح نیست."
        "ERROR_USER_DISABLED" -> "این حساب غیرفعال شده است."
        "ERROR_OPERATION_NOT_ALLOWED" -> "ورود با ایمیل و رمز عبور در Firebase فعال نیست."
        "ERROR_TOO_MANY_REQUESTS" -> "تلاش‌های ناموفق زیاد بوده؛ کمی بعد دوباره امتحان کن."
        "ERROR_NETWORK_REQUEST_FAILED" -> "ارتباط با سرویس ورود برقرار نشد؛ اینترنت را بررسی کن."
        else -> {
            val detail = error?.localizedMessage
                ?.replace(Regex("<[^>]*>"), " ")
                ?.replace(Regex("\\s+"), " ")
                ?.take(220)
                ?.takeIf { it.isNotBlank() }
            "ورود انجام نشد." + (if (detail != null) " جزئیات: $detail" else " دوباره تلاش کن.")
        }
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
            Text("برای ورود، ایمیل و رمز عبور حساب قبلی را وارد کن.")
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ایمیل") },
                singleLine = true,
                enabled = !busy
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("رمز عبور") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !busy
            )
            Spacer(Modifier.height(16.dp))
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
                                if (!task.isSuccessful) {
                                    message = signInErrorMessage(task.exception)
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (busy) CircularProgressIndicator()
                else Text("ورود")
            }
            if (message.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
