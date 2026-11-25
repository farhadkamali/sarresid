package ir.farhadkamali.sarresid

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider

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
    val type = error?.javaClass?.simpleName ?: "UnknownException"
    val authCode = (error as? FirebaseAuthException)?.errorCode
    val detail = error?.localizedMessage
        ?.replace(Regex("<[^>]*>"), " ")
        ?.replace(Regex("\\s+"), " ")
        ?.take(180)
        ?.takeIf { it.isNotBlank() }

    val friendly = when {
        error is FirebaseNetworkException ->
            "ارتباط با سرویس ورود برقرار نشد."
        authCode == "ERROR_INVALID_EMAIL" -> "فرمت ایمیل صحیح نیست."
        authCode == "ERROR_USER_NOT_FOUND" ||
            authCode == "ERROR_INVALID_CREDENTIAL" ||
            authCode == "ERROR_WRONG_PASSWORD" -> "ایمیل یا رمز عبور صحیح نیست."
        authCode == "ERROR_USER_DISABLED" -> "این حساب غیرفعال شده است."
        authCode == "ERROR_OPERATION_NOT_ALLOWED" ->
            "این روش ورود در Firebase فعال نیست."
        authCode == "ERROR_TOO_MANY_REQUESTS" ->
            "تلاش‌های ناموفق زیاد بوده؛ کمی بعد دوباره امتحان کن."
        authCode == "ERROR_NETWORK_REQUEST_FAILED" ->
            "درخواست ورود به سرور کامل نشد؛ ممکن است پاسخ سرور یا محدودیت دسترسی باشد."
        else -> "ورود انجام نشد."
    }

    val diagnostic = buildString {
        append("کد فنی: ")
        append(authCode ?: type)
        if (detail != null) {
            append(" | ")
            append(detail)
        }
    }
    return "$friendly\n$diagnostic"
}

@Composable
private fun SignInScreen(auth: FirebaseAuth) {
    val context = LocalContext.current
    val googleOptions = remember(context) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(context.getString(R.string.google_web_client_id))
            .build()
    }
    val googleClient = remember(context, googleOptions) {
        GoogleSignIn.getClient(context, googleOptions)
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    busy = false
                    message = "توکن گوگل دریافت نشد. تنظیمات OAuth را بررسی کن."
                } else {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            busy = false
                            if (!task.isSuccessful) {
                                message = signInErrorMessage(task.exception)
                            }
                        }
                }
            } catch (error: ApiException) {
                busy = false
                message = "انتخاب حساب گوگل انجام نشد. کد فنی: ${error.statusCode}"
            } catch (error: Exception) {
                busy = false
                message = signInErrorMessage(error)
            }
        } else {
            busy = false
            message = "ورود با گوگل لغو شد."
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("سررسید", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("برای ورود، یکی از روش‌های زیر را انتخاب کن.")
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = {
                    message = ""
                    busy = true
                    try {
                        googleSignInLauncher.launch(googleClient.signInIntent)
                    } catch (error: Exception) {
                        busy = false
                        message = signInErrorMessage(error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (busy) CircularProgressIndicator()
                else Text("ورود با گوگل")
            }
            Spacer(Modifier.height(18.dp))
            Text("یا ورود با ایمیل و رمز عبور", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
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
