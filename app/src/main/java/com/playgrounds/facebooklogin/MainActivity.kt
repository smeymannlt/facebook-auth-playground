package com.playgrounds.facebooklogin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.facebook.AccessToken
import com.facebook.login.widget.LoginButton
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: AppViewModel

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_META_DATA
        )?.applicationInfo?.metaData?.putString(
            "com.facebook.sdk.ApplicationId",
            getString(R.string.facebook_app_id)
        )

        val tv = findViewById<TextView>(R.id.tv)
        viewModel = ViewModelProvider.NewInstanceFactory().create(AppViewModel::class.java)
        viewModel.loginLiveData.observe(this) { status ->
            tv.text = when (status) {
                AppViewModel.FacebookLoginResult.Cancelled -> "Cancelled"
                is AppViewModel.FacebookLoginResult.Failed -> "Failed on ${status.errorException}"
                is AppViewModel.FacebookLoginResult.Success -> "Logged in with ${status.loginResult?.accessToken}"
            }
        }


        val loginButton = findViewById<LoginButton>(R.id.login_button)
        loginButton.setPermissions(listOf("email"))

        val loginManually: Button = findViewById(R.id.login_manually_button)
        loginManually.setOnClickListener { viewModel.login(this@MainActivity) }

        findViewById<Button>(R.id.logout_button).setOnClickListener {
            viewModel.logout()
            lifecycleScope.launch {
                tv.text = refreshByLoginStatus(this@MainActivity)
            }
        }

//        lifecycleScope.launch {
//            tv.text = refreshByLoginStatus(this@MainActivity)
//        }
        tv.text = AccessToken.getCurrentAccessToken()
            ?.run { listOf(permissions, graphDomain, token).joinToString("\n") }

        lifecycleScope.launch {
            val token = AccessToken.getCurrentAccessToken() ?: return@launch
            tv.text = viewModel.getUserMeta(token).toString()
        }
    }

    private suspend fun refreshByLoginStatus(context: Context): String =
        when (val status = viewModel.getLoginStatus(context)) {
            is AppViewModel.FacebookLoginStatus.Error -> "Error ${status.exception}"
            is AppViewModel.FacebookLoginStatus.LoggedIn -> "Logged in with ${status.token}"
            AppViewModel.FacebookLoginStatus.NotLoggedIn -> "Not logged in"
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(requestCode, resultCode, data)
    }
}