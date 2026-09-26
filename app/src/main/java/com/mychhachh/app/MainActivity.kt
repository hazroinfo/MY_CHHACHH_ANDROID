package com.mychhachh.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openInBrowserEngine()
    }

    private fun openInBrowserEngine() {
        val url = Uri.parse(HOME_URL)

        val primary = CustomTabsIntent.Builder()
            .setShowTitle(false)
            .setUrlBarHidingEnabled(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()

        try {
            primary.intent.setPackage(CHROME_PACKAGE)
            primary.launchUrl(this, url)
        } catch (_: ActivityNotFoundException) {
            val fallback = CustomTabsIntent.Builder()
                .setShowTitle(false)
                .setUrlBarHidingEnabled(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build()
            fallback.launchUrl(this, url)
        }

        finish()
    }

    companion object {
        private const val HOME_URL = "https://chhachh.pages.dev/"
        private const val CHROME_PACKAGE = "com.android.chrome"
    }
}
