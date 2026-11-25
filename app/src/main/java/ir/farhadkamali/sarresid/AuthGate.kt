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
import androidx.compose.material3.TextButton
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

private fun authErrorMessage(error: Exception?): String {
    val code = (error as? FirebaseAuthException)?.errorCode
    val detail = error?.localizedMessage
        ?.replace(Regex("<[^>]*>"), " ")
        ?.replace(Regex("\\s+"), " ")
        ?.take(140)
    val friendly = when {
        error is FirebaseNetworkException -> "ارتباط اینترنت برقرار نیست؛ اتصال را بررسی کن."
        code == "ERROR_INVALID_EMAIL" -> "فرمت ایمیل صحیح نیست."
        code == "ERROR_USER_NOT_FOUND" || code == "ERROR_WRONG_PASSWORD" ||
            code == "ERROR_INVALID_CREDENTIAL" -> "ایمیل یا رمز عبور اشتباه است."
        code == "ERROR_EMAIL_ALREADY_IN_USE" -> "این ایمیل قبلاً ثبت شده؛ از گزینه ورود استفاده کن."
        code == "ERROR_WEAK_PASSWORD" -> "رمز عبور باید حداقل ۶ کاراکتر باشد."
        code == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
            "این ایمیل قبلاً با روش دیگری ثبت شده؛ با ایمیل و رمز قبلی وارد شو."
        code == "ERROR_OPERATION_NOT_ALLOWED" -> "این روش ورود در تنظیمات Firebase فعال نیست."
        code == "ERROR_TOO_MANY_REQUESTS" -> "تعداد تلاش‌ها زیاد بوده؛ کمی بعد دوباره امتحان کن."
        else -> "عملیات انجام نشد."
    }
    return if (code != null) "$friendly\nکد خطا: $code" +
        (detail?.let { " | $it" } ?: "") else friendly +
        (detail?.let { "\\n$it" } ?: "")
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

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            busy = false
            message = "ورود با گوگل لغو شد."
        } else {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val token = account.idToken
                if (token.isNullOrBlank()) {
                    busy = false
                    message = "توکن گوگل دریافت نشد؛ تنظیمات شناسه OAuth را بررسی کن."
                } else {
                    val credential = GoogleAuthProvider.getCredential(token, null)
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        busy = false
                        if (!task.isSuccessful) message = authErrorMessage(task.exception)
                    }
                }
            } catch (e: ApiException) {
                busy = false
                message = "انتخاب حساب گوگل انجام نشد. کد فنی: ${e.statusCode}"
            } catch (e: Exception) {
                busy = false
                message = authErrorMessage(e)
            }
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
            Text("برای ورود یا ساخت حساب، یکی از روش‌ها را انتخاب کن.")
            Spacer(Modifier.height(22.dp))

            OutlinedButton(
                onClick = {
                    message = ""
                    busy = true
                    try {
                        googleLauncher.launch(googleClient.signInIntent)
                    } catch (e: Exception) {
                        busy = false
                        message = authErrorMessage(e)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (busy) CircularProgressIndicator() else Text("ورود با گوگل")
            }

            Spacer(Modifier.height(18.dp))
            Text("یا ورود با ایمیل و رمز عبور")
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
                label = { Text("رمز عبور (حداقل ۶ کاراکتر)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !busy
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val cleanEmail = email.trim()
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
                enabled = !busy,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (busy) CircularProgressIndicator() else Text("ورود")
            }

            TextButton(
                onClick = {
                    val cleanEmail = email.trim()
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
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            ) { Text("ساخت حساب جدید") }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun GoogleAccountLinkButton() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val googleOptions = remember(context) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(context.getString(R.string.google_web_client_id))
            .build()
    }
    val googleClient = remember(context, googleOptions) {
        GoogleSignIn.getClient(context, googleOptions)
    }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val alreadyLinked = auth.currentUser?.providerData?.any { it.providerId == "google.com" } == true

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            busy = false
            message = "اتصال گوگل لغو شد."
        } else {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val token = account.idToken
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    busy = false
                    message = "ابتدا با حساب قبلی وارد شو."
                } else if (token.isNullOrBlank()) {
                    busy = false
                    message = "توکن گوگل دریافت نشد."
                } else {
                    val credential = GoogleAuthProvider.getCredential(token, null)
                    currentUser.linkWithCredential(credential).addOnCompleteListener { task ->
                        busy = false
                        message = if (task.isSuccessful) {
                            "حساب گوگل به همین حساب قبلی متصل شد؛ سوابق حفظ شدند."
                        } else authErrorMessage(task.exception)
                    }
                }
            } catch (e: Exception) {
                busy = false
                message = authErrorMessage(e)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            onClick = {
                message = ""
                busy = true
                try {
                    launcher.launch(googleClient.signInIntent)
                } catch (e: Exception) {
                    busy = false
                    message = authErrorMessage(e)
                }
            },
            enabled = !busy && !alreadyLinked,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (busy) CircularProgressIndicator()
            else Text(if (alreadyLinked) "حساب گوگل متصل است" else "اتصال گوگل به حساب قبلی")
        }
        if (message.isNotBlank()) {
            Text(
                message,
                color = if (message.contains("متصل شد")) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}
