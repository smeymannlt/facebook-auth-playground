package com.playgrounds.facebooklogin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class AppViewModel : ViewModel() {
    sealed class FacebookLoginStatus {
        object NotLoggedIn : FacebookLoginStatus()
        data class LoggedIn(val token: String?) : FacebookLoginStatus()
        data class Error(val exception: Exception?) : FacebookLoginStatus()
    }

    sealed class FacebookLoginResult {
        data class Success(val loginResult: LoginResult?) : FacebookLoginResult()
        object Cancelled : FacebookLoginResult()
        data class Failed(val errorException: Exception) : FacebookLoginResult()
    }

    private var callbackManager: CallbackManager = CallbackManager.Factory.create()
    private val loginManager = LoginManager.getInstance()
    val loginLiveData = MutableLiveData<FacebookLoginResult>()

    init {
        loginManager.registerCallback(callbackManager, FbLoginCallback(loginLiveData::postValue))
    }

    fun login(activity: Activity) {
        loginManager.logInWithReadPermissions(activity, listOf("public_profile, email"))
    }

    suspend fun getLoginStatus(context: Context): FacebookLoginStatus {
        return suspendCoroutine { continuation ->
            LoginManager.getInstance().retrieveLoginStatus(context, object : LoginStatusCallback {
                override fun onCompleted(accessToken: AccessToken?) {
                    continuation.resume(FacebookLoginStatus.LoggedIn(accessToken?.token))
                }

                override fun onFailure() {
                    continuation.resume(FacebookLoginStatus.NotLoggedIn)
                }

                override fun onError(exception: java.lang.Exception?) {
                    continuation.resume(FacebookLoginStatus.Error(exception))
                }
            })
        }
    }

    suspend fun getUserMeta(accessToken: AccessToken): GraphResponse? {
        val req = GraphRequest.newMeRequest(accessToken) { obj, response ->
            Log.v("FOO", "$obj, $response")
        }
        val response = withContext(Dispatchers.IO) {
            val bundle = Bundle()
            bundle.putString("fields", "id,name,email,picture")
            req.parameters = bundle
            req.executeAndWait()
        }

        return response
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }


    class FbLoginCallback(private val onResult: (FacebookLoginResult) -> Unit) :
        FacebookCallback<LoginResult?> {
        override fun onSuccess(loginResult: LoginResult?) {
            onResult(FacebookLoginResult.Success(loginResult))
        }

        override fun onCancel() {
            onResult(FacebookLoginResult.Cancelled)
        }

        override fun onError(exception: FacebookException) {
            onResult(FacebookLoginResult.Failed(exception))
        }
    }

    fun logout() {
        loginManager.logOut()
    }
}
