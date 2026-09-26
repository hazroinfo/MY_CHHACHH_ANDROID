package com.mychhachh.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.trusted.TrustedWebActivityIntentBuilder

class MainActivity : Activity() {

    private var customTabsClient: CustomTabsClient? = null
    private var customTabsSession: CustomTabsSession? = null
    private var serviceConnection: CustomTabsServiceConnection? = null
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindBrowserAndLaunch()
    }

    private fun bindBrowserAndLaunch() {
        val browserPackage = CustomTabsClient.getPackageName(this, null)
        if (browserPackage == null) {
            openFallbackBrowser()
            return
        }

        val connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                name: ComponentName,
                client: CustomTabsClient
            ) {
                customTabsClient = client
                client.warmup(0L)
                val session = client.newSession(null)
                if (session == null) {
                    openFallbackBrowser()
                    return
                }
                customTabsSession = session
                launchTrustedWebActivity(session)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                customTabsClient = null
                customTabsSession = null
            }
        }

        serviceConnection = connection

        val bound = CustomTabsClient.bindCustomTabsService(
            this,
            browserPackage,
            connection
        )

        if (!bound) {
            serviceConnection = null
            openFallbackBrowser()
        }
    }

    private fun launchTrustedWebActivity(session: CustomTabsSession) {
        if (launched) return
        launched = true

        TrustedWebActivityIntentBuilder(HOME_URL)
            .build(session)
            .launchTrustedWebActivity(this)

        finish()
    }

    private fun openFallbackBrowser() {
        if (launched) return
        launched = true
        startActivity(Intent(Intent.ACTION_VIEW, HOME_URL))
        finish()
    }

    override fun onDestroy() {
        serviceConnection?.let {
            try {
                unbindService(it)
            } catch (_: IllegalArgumentException) {
            }
        }
        serviceConnection = null
        customTabsSession = null
        customTabsClient = null
        super.onDestroy()
    }

    companion object {
        private val HOME_URL: Uri = Uri.parse("https://chhachh.pages.dev/")
    }
}
