package com.mychhachh.app

import android.app.Activity
import android.os.Bundle
import android.net.Uri
import androidx.browser.trusted.TrustedWebActivityIntentBuilder

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = TrustedWebActivityIntentBuilder(Uri.parse(HOME_URL))
            .build(this)

        intent.launchTrustedWebActivity(this)
        finish()
    }

    companion object {
        private const val HOME_URL = "https://chhachh.pages.dev/"
    }
}
