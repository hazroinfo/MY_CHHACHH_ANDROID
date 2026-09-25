package com.mychhachh.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingMediaRequest: PermissionRequest? = null
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = fileCallback ?: return@registerForActivityResult
        fileCallback = null
        callback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
    }

    private val mediaPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val request = pendingMediaRequest ?: return@registerForActivityResult
        pendingMediaRequest = null
        val allowed = request.resources.filter { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                else -> false
            }
        }.toTypedArray()
        if (allowed.isNotEmpty()) request.grant(allowed) else request.deny()
    }

    private val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val origin = pendingGeoOrigin
        val callback = pendingGeoCallback
        pendingGeoOrigin = null
        pendingGeoCallback = null
        if (origin != null && callback != null) {
            val granted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            callback.invoke(origin, granted, false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WebView.setWebContentsDebuggingEnabled(false)

        webView = WebView(this)
        val topInset = (4f * resources.displayMetrics.density).toInt()
        webView.setPadding(0, topInset, 0, 0)
        webView.clipToPadding = false
        webView.overScrollMode = android.view.View.OVER_SCROLL_NEVER
        setContentView(webView)

        configureWebView()

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(HOME_URL)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun configureWebView() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            allowContentAccess = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            loadWithOverviewMode = false
            useWideViewPort = false
            textZoom = 100
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = userAgentString + " MyChhachhAndroid/2.0"
        }

        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                applyAppOnlyPolish(view)
            }

            override fun onPageCommitVisible(view: WebView, url: String?) {
                super.onPageCommitVisible(view, url)
                applyAppOnlyPolish(view)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                applyAppOnlyPolish(view)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return handleUri(request.url)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUri(Uri.parse(url))
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    val intent = fileChooserParams?.createIntent()
                        ?: Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                    filePicker.launch(intent)
                    true
                } catch (_: Exception) {
                    fileCallback = null
                    callback?.onReceiveValue(null)
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val needed = mutableListOf<String>()
                    if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                    ) {
                        needed += Manifest.permission.RECORD_AUDIO
                    }
                    if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    ) {
                        needed += Manifest.permission.CAMERA
                    }

                    if (needed.isEmpty()) {
                        request.grant(request.resources)
                    } else {
                        pendingMediaRequest?.deny()
                        pendingMediaRequest = request
                        mediaPermissions.launch(needed.distinct().toTypedArray())
                    }
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                val alreadyGranted =
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (alreadyGranted) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    locationPermissions.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }
    }

    private fun applyAppOnlyPolish(view: WebView) {
        val script = """
            (function() {
              try {
                var STYLE_ID = 'mc-android-app-only-polish';
                var style = document.getElementById(STYLE_ID);
                if (!style) {
                  style = document.createElement('style');
                  style.id = STYLE_ID;
                  (document.head || document.documentElement).appendChild(style);
                }
                style.textContent = [
                  '[data-mc-hide-top-line="1"]{display:none!important;opacity:0!important;visibility:hidden!important;}',
                  '[data-mc-hide-top-before="1"]::before,[data-mc-hide-top-after="1"]::after{display:none!important;opacity:0!important;visibility:hidden!important;content:none!important;}',
                  'body.mc-android-scrolling .card,body.mc-android-scrolling .top,body.mc-android-scrolling .header-search,body.mc-android-scrolling .side-menu{backdrop-filter:none!important;-webkit-backdrop-filter:none!important;}',
                  'body.mc-android-scrolling #chhachhWeatherBg,body.mc-android-scrolling #chhachhWeatherBg *{animation-play-state:paused!important;}',
                  '#chhachhWeatherBg{background-attachment:scroll!important;}'
                ].join('');

                function forceHeader() {
                  var h = document.querySelector('header.top,.top');
                  if (h) {
                    h.style.setProperty('top', '0px', 'important');
                    h.style.setProperty('margin-top', '0px', 'important');
                  }
                }

                function isThinTop(cs, rect) {
                  if (!cs || !rect) return false;
                  var positioned = cs.position === 'fixed' || cs.position === 'absolute' || cs.position === 'sticky';
                  var top = parseFloat(cs.top || '9999');
                  var height = rect.height || parseFloat(cs.height || '0');
                  var width = rect.width || parseFloat(cs.width || '0');
                  return positioned && (rect.top <= 12 || top <= 12) &&
                         height > 0 && height <= 9 &&
                         width >= Math.max(120, innerWidth * 0.35);
                }

                function sweepTopLines() {
                  var nodes = document.querySelectorAll('body *');
                  for (var i = 0; i < nodes.length; i++) {
                    var el = nodes[i];
                    if (!el || el.matches('header.top,.top,.menu-toggle,.brand,.top-actions')) continue;
                    try {
                      var rect = el.getBoundingClientRect();
                      var cs = getComputedStyle(el);
                      var name = ((el.id || '') + ' ' + (typeof el.className === 'string' ? el.className : '')).toLowerCase();
                      if (isThinTop(cs, rect) && /(load|progress|pace|bar|line|route|nav)/.test(name)) {
                        el.setAttribute('data-mc-hide-top-line','1');
                      }

                      var before = getComputedStyle(el,'::before');
                      if (before && before.content !== 'none') {
                        var bh = parseFloat(before.height || '0');
                        var bt = parseFloat(before.top || '9999');
                        var bp = before.position;
                        if ((bp === 'fixed' || bp === 'absolute' || bp === 'sticky') &&
                            bt <= 12 && bh > 0 && bh <= 9) {
                          el.setAttribute('data-mc-hide-top-before','1');
                        }
                      }

                      var after = getComputedStyle(el,'::after');
                      if (after && after.content !== 'none') {
                        var ah = parseFloat(after.height || '0');
                        var at = parseFloat(after.top || '9999');
                        var ap = after.position;
                        if ((ap === 'fixed' || ap === 'absolute' || ap === 'sticky') &&
                            at <= 12 && ah > 0 && ah <= 9) {
                          el.setAttribute('data-mc-hide-top-after','1');
                        }
                      }
                    } catch (_) {}
                  }
                }

                forceHeader();
                sweepTopLines();

                if (!window.__mcAndroidPolishTimer) {
                  window.__mcAndroidPolishTimer = setInterval(function(){
                    forceHeader();
                    sweepTopLines();
                  }, 250);
                }

                if (!window.__mcAndroidScrollFix) {
                  window.__mcAndroidScrollFix = true;
                  var t = 0;
                  addEventListener('scroll', function(){
                    document.body && document.body.classList.add('mc-android-scrolling');
                    clearTimeout(t);
                    t = setTimeout(function(){
                      document.body && document.body.classList.remove('mc-android-scrolling');
                    }, 180);
                  }, {passive:true});
                }

                if (!window.__mcAndroidPolishObserver) {
                  window.__mcAndroidPolishObserver = new MutationObserver(function(){
                    forceHeader();
                    sweepTopLines();
                  });
                  window.__mcAndroidPolishObserver.observe(document.documentElement, {
                    childList:true,
                    subtree:true,
                    attributes:true,
                    attributeFilter:['class','style']
                  });
                }
              } catch (e) {}
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    private fun handleUri(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()

        if ((scheme == "https" || scheme == "http") &&
            (host == "chhachh.pages.dev" || host.endsWith(".chhachh.pages.dev"))
        ) {
            return false
        }

        return try {
            when (scheme) {
                "http", "https", "tel", "mailto", "geo", "sms", "market", "intent" -> {
                    val intent = if (scheme == "intent") {
                        Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    } else {
                        Intent(Intent.ACTION_VIEW, uri)
                    }
                    startActivity(intent)
                    true
                }
                else -> {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onPause() {
        webView.onPause()
        webView.pauseTimers()
        super.onPause()
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        fileCallback = null
        pendingMediaRequest?.deny()
        pendingMediaRequest = null
        webView.apply {
            stopLoading()
            webChromeClient = null
            webViewClient = WebViewClient()
            destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val HOME_URL = "https://chhachh.pages.dev/"
    }
}
