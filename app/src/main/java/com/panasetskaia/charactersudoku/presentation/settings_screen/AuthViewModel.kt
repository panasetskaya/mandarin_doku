package com.panasetskaia.charactersudoku.presentation.settings_screen

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.panasetskaia.charactersudoku.R
import com.panasetskaia.charactersudoku.presentation.base.BaseViewModel
import com.panasetskaia.charactersudoku.utils.Event
import com.panasetskaia.charactersudoku.utils.myLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AuthViewModel @Inject constructor() : BaseViewModel() {

    private val _toastFlow = MutableStateFlow<Event<Int>?>(null)
    val toastFlow: StateFlow<Event<Int>?>
        get() = _toastFlow

    private val _isUserSignedInFlow = MutableStateFlow(false)
    val isUserSignedInFlow: StateFlow<Boolean>
        get() = _isUserSignedInFlow

    override fun deleteThisCategory(cat: String) {
    }

    fun goToSignUp() {
        navigate(SignInFragmentDirections.actionSignInFragmentToSignUpFragment())
    }

    fun checkSignIn(auth: FirebaseAuth) {
        val currentUser = auth.currentUser
        _isUserSignedInFlow.value = currentUser != null
    }

    fun signInWithEmail(auth: FirebaseAuth, email: String, password: String, activity: Activity) {
        if (email != "" && password != "") {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        myLog("signInWithEmail:success")
                        _isUserSignedInFlow.value = true
                    } else {
                        myLog("signInWithEmail:failure: ${task.exception}")
                        _toastFlow.value = Event(R.string.auth_failed)
                        _isUserSignedInFlow.value = false
                    }
                }
        }
    }

    fun signInWithGoogle(
        auth: FirebaseAuth,
        oneTapClient: SignInClient,
        data: Intent?,
        activity: Activity
    ) {
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken
            when {
                idToken != null -> {
                    myLog("signInWithGoogle: Got ID token.")
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(activity) { task ->
                            if (task.isSuccessful) {
                                myLog("signInWithGoogle:success")
                                _toastFlow.value = Event(R.string.welcome)
                                _isUserSignedInFlow.value = true
                            } else {
                                myLog("signInWithCredential:failure: ${task.exception}")
                                _toastFlow.value = Event(R.string.auth_failed)
                                _isUserSignedInFlow.value = false
                            }
                        }
                }

                else -> {
                    myLog("signInWithGoogle:failure: no ID token!")
                }
            }
        } catch (e: Exception) {
            myLog("signInWithGoogle:failure: $e ${e.message}")
        }
    }

    fun showWrongPasswordToast() {
        _toastFlow.value = Event(R.string.password_not_match)
    }

    fun signupWithEmail(auth: FirebaseAuth, email: String, password: String, activity: Activity) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    myLog("createUserWithEmail:success")
                    _isUserSignedInFlow.value = true
                } else {
                    val msg = when (task.exception) {
                        is com.google.firebase.auth.FirebaseAuthUserCollisionException -> R.string.already_in_use
                        is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> R.string.too_weak
                        else -> R.string.signup_error
                    }
                    myLog("createUserWithEmail:failure: ${task.exception}")
                    _toastFlow.value = Event(msg)
                    _isUserSignedInFlow.value = false
                }
            }
    }
}
