package earth.degenerate.blackbeard;

import android.annotation.SuppressLint;
import android.webkit.JavascriptInterface;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(0, 0, 0);
    private static final int TOOLBAR_BACKGROUND = Color.rgb(21, 21, 25);
    private static final int GOLD = Color.rgb(199, 156, 82);
    private static final int GOLD_BORDER = Color.rgb(136, 99, 49);

    private static final String PAGE_PROTECTION_SCRIPT = """
        (function(){
          try {
            window.open = function(){ return null; };
            window.alert = function(){};
            window.confirm = function(){ return false; };
            window.prompt = function(){ return null; };

            function cleanLinks(root) {
              if (!root || !root.querySelectorAll) return;
              root.querySelectorAll('a[target="_blank"],a[target="_new"]').forEach(function(link) {
                link.removeAttribute('target');
                link.removeAttribute('rel');
              });
            }

            function isCompactAdOverlay(element) {
              if (!element || element.nodeType !== 1) return false;
              if (element.matches('[data-shb]')) return true;
              if (element.tagName !== 'IFRAME') return false;
              var style = getComputedStyle(element);
              var rect = element.getBoundingClientRect();
              var zIndex = parseInt(style.zIndex, 10);
              return (style.position === 'fixed' || style.position === 'absolute')
                && Number.isFinite(zIndex)
                && zIndex >= 1000000
                && rect.width >= 100
                && rect.height >= 80
                && rect.width <= 620
                && rect.height <= 620;
            }

            function removeOverlay(element) {
              if (isCompactAdOverlay(element)) element.remove();
            }

            function sweep(root) {
              if (!root || !root.querySelectorAll) return;
              cleanLinks(root);
              removeOverlay(root);
              root.querySelectorAll('[data-shb],iframe').forEach(removeOverlay);
            }

            function isVisible(element, minimumWidth, minimumHeight) {
              var rect = element.getBoundingClientRect();
              var style = getComputedStyle(element);
              return rect.width >= minimumWidth
                && rect.height >= minimumHeight
                && style.display !== 'none'
                && style.visibility !== 'hidden'
                && parseFloat(style.opacity || '1') > 0;
            }

            function mediaActive() {
              var activeVideo = Array.from(document.querySelectorAll('video')).some(function(video) {
                return !video.paused && !video.ended && isVisible(video, 180, 100);
              });
              var host = location.hostname.toLowerCase();
              var path = location.pathname;
              var playerPage =
                ((host === 'aether.bar' || host.endsWith('.aether.bar')) && path.startsWith('/media/'))
                || ((host === 'popcornmovies.io' || host.endsWith('.popcornmovies.io'))
                  && path.startsWith('/watch/'));
              var embeddedPlayer = playerPage
                && Array.from(document.querySelectorAll('iframe')).some(function(frame) {
                  return isVisible(frame, 240, 130);
                });
              return !!(document.fullscreenElement || document.webkitFullscreenElement
                || activeVideo || embeddedPlayer);
            }

            var lastMediaState = null;
            var reportScheduled = false;
            function reportMedia() {
              reportScheduled = false;
              var active = mediaActive();
              if (active === lastMediaState) return;
              lastMediaState = active;
              if (window.BlackBeardMedia) window.BlackBeardMedia.setActive(active);
            }
            function scheduleReport() {
              if (reportScheduled) return;
              reportScheduled = true;
              requestAnimationFrame(reportMedia);
            }

            sweep(document);
            new MutationObserver(function(mutations) {
              mutations.forEach(function(mutation) {
                if (mutation.type === 'attributes') removeOverlay(mutation.target);
                else mutation.addedNodes.forEach(sweep);
              });
              scheduleReport();
            }).observe(document.documentElement || document, {
              attributes: true,
              attributeFilter: ['class', 'style', 'src', 'data-shb'],
              childList: true,
              subtree: true
            });

            document.addEventListener('click', function(event) {
              var node = event.target;
              var link = node && node.closest
                ? node.closest('a[target="_blank"],a[target="_new"]')
                : null;
              if (link) link.removeAttribute('target');
              scheduleReport();
            }, true);
            ['play','playing','pause','ended','emptied','abort','fullscreenchange',
              'webkitfullscreenchange'].forEach(function(eventName) {
              document.addEventListener(eventName, scheduleReport, true);
            });
            window.addEventListener('resize', scheduleReport, true);
            scheduleReport();
          } catch (error) {}
        })();
        """;

    private enum Site {
        CINEJOY("cinejoy", "Cinejoy", "https://cinejoy.to/", "cinejoy.to"),
        AETHER("aether", "Aether", "https://aether.bar/", "aether.bar"),
        POPCORN("popcorn", "Popcorn", "https://popcornmovies.io/", "popcornmovies.io");

        final String id;
        final String title;
        final String url;
        final String rootHost;

        Site(String id, String title, String url, String rootHost) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.rootHost = rootHost;
        }

        boolean includesHost(String host) {
            if (host == null) return false;
            String normalized = host.toLowerCase(Locale.US);
            return normalized.equals(rootHost) || normalized.endsWith("." + rootHost);
        }

        static Site fromID(String id) {
            if (id == null) return null;
            for (Site site : values()) {
                if (site.id.equals(id)) return site;
            }
            return null;
        }
    }

    private FrameLayout root;
    private View landingScreen;
    private LinearLayout browserContent;
    private LinearLayout toolbar;
    private WebView webView;
    private TextView backButton;
    private TextView forwardButton;
    private TextView statusLabel;
    private TextView reloadButton;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private Site selectedSite;
    private boolean inlineMediaActive;
    private final AtomicInteger blockedCount = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(BACKGROUND);
        window.setNavigationBarColor(BACKGROUND);
        window.setBackgroundDrawable(new ColorDrawable(BACKGROUND));

        buildInterface();
        configureWebView();

        selectedSite = savedInstanceState == null
            ? null
            : Site.fromID(savedInstanceState.getString("selectedSite"));

        if (selectedSite == null) {
            displayLandingScreen(false);
        } else {
            displayBrowser();
            if (webView.restoreState(savedInstanceState) == null) {
                webView.loadUrl(selectedSite.url);
            }
        }

        showLaunchArtwork();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            WebView.startSafeBrowsing(this, null);
        }

        webView.setWebViewClient(new ProtectedWebViewClient());
        webView.setWebChromeClient(new ProtectedChromeClient());
        webView.addJavascriptInterface(new MediaStateBridge(), "BlackBeardMedia");
    }

    private void buildInterface() {
        root = new FrameLayout(this);
        root.setBackgroundColor(BACKGROUND);

        landingScreen = buildLandingScreen();
        root.addView(landingScreen, matchParentFrameParams());

        browserContent = new LinearLayout(this);
        browserContent.setOrientation(LinearLayout.VERTICAL);
        browserContent.setBackgroundColor(BACKGROUND);

        webView = new WebView(this);
        webView.setBackgroundColor(BACKGROUND);
        browserContent.addView(webView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));

        buildToolbar();
        browserContent.addView(toolbar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(56)
        ));
        root.addView(browserContent, matchParentFrameParams());

        setContentView(root);
        updateNavigationState();
    }

    private View buildLandingScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BACKGROUND);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(dp(24), dp(24), dp(24), dp(24));
        column.setBackgroundColor(BACKGROUND);

        column.addView(space(24));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.blackbeard_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setContentDescription(null);
        column.addView(logo, new LinearLayout.LayoutParams(dp(176), dp(176)));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dp(16);
        column.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.choose_course);
        subtitle.setTextColor(Color.rgb(165, 165, 170));
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(4);
        subtitleParams.bottomMargin = dp(22);
        column.addView(subtitle, subtitleParams);

        int availableWidth = getResources().getDisplayMetrics().widthPixels - dp(48);
        int buttonWidth = Math.min(availableWidth, dp(420));
        for (Site site : Site.values()) {
            TextView button = makeSiteButton(site);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                buttonWidth,
                dp(58)
            );
            params.bottomMargin = dp(12);
            column.addView(button, params);
        }

        column.addView(space(24));
        scrollView.addView(column, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return scrollView;
    }

    private TextView makeSiteButton(Site site) {
        TextView button = new TextView(this);
        button.setText(getString(R.string.site_button_label, site.title));
        button.setTextColor(GOLD);
        button.setTextSize(18);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(20), 0, dp(20), 0);
        button.setSingleLine(true);
        button.setContentDescription("Open " + site.title + " in the protected browser");

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(10, 10, 10));
        background.setStroke(dp(1), GOLD_BORDER);
        background.setCornerRadius(dp(12));
        button.setBackground(background);
        button.setOnClickListener(view -> openSite(site));
        return button;
    }

    private void buildToolbar() {
        toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(6), dp(5), dp(6), dp(5));
        toolbar.setBackgroundColor(TOOLBAR_BACKGROUND);

        backButton = makeToolbarButton("←", "Back", view -> {
            if (webView.canGoBack()) webView.goBack();
        });
        forwardButton = makeToolbarButton("→", "Forward", view -> {
            if (webView.canGoForward()) webView.goForward();
        });

        statusLabel = new TextView(this);
        statusLabel.setTextColor(Color.WHITE);
        statusLabel.setTextSize(14);
        statusLabel.setGravity(Gravity.CENTER);
        statusLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statusLabel.setText(R.string.protected_browser);
        statusLabel.setContentDescription("BlackBeard protected browser");

        TextView menuButton = makeToolbarButton("⌂", "Main menu", view -> displayLandingScreen(true));
        reloadButton = makeToolbarButton("↻", "Reload", view -> {
            if (webView.getProgress() < 100) webView.stopLoading();
            else webView.reload();
        });

        toolbar.addView(backButton, toolbarButtonParams());
        toolbar.addView(forwardButton, toolbarButtonParams());
        toolbar.addView(statusLabel, new LinearLayout.LayoutParams(0, dp(46), 1f));
        toolbar.addView(menuButton, toolbarButtonParams());
        toolbar.addView(reloadButton, toolbarButtonParams());
    }

    private TextView makeToolbarButton(
        String text,
        String description,
        View.OnClickListener listener
    ) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(25);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(description);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout.LayoutParams toolbarButtonParams() {
        return new LinearLayout.LayoutParams(dp(46), dp(46));
    }

    private FrameLayout.LayoutParams matchParentFrameParams() {
        return new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    private Space space(int height) {
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return space;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showLaunchArtwork() {
        ImageView artwork = new ImageView(this);
        artwork.setImageResource(R.drawable.blackbeard_loading);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artwork.setBackgroundColor(BACKGROUND);
        artwork.setContentDescription(null);
        root.addView(artwork, matchParentFrameParams());

        artwork.postDelayed(() ->
            artwork.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> root.removeView(artwork))
                .start(),
            900
        );
    }

    private void openSite(Site site) {
        setInlineMediaActive(false);
        selectedSite = site;
        blockedCount.set(0);
        statusLabel.setText(R.string.protected_browser);
        landingScreen.setVisibility(View.GONE);
        browserContent.setVisibility(View.VISIBLE);
        webView.stopLoading();
        webView.clearHistory();
        webView.loadUrl(site.url);
        updateNavigationState();
    }

    private void displayBrowser() {
        setInlineMediaActive(false);
        landingScreen.setVisibility(View.GONE);
        browserContent.setVisibility(View.VISIBLE);
        statusLabel.setText(R.string.protected_browser);
    }

    private void displayLandingScreen(boolean clearBrowser) {
        setInlineMediaActive(false);
        if (customView != null) hideCustomView();
        if (clearBrowser) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
        }
        selectedSite = null;
        blockedCount.set(0);
        statusLabel.setText(R.string.protected_browser);
        browserContent.setVisibility(View.GONE);
        landingScreen.setVisibility(View.VISIBLE);
        updateNavigationState();
    }

    private void updateNavigationState() {
        boolean canBack = selectedSite != null && webView != null && webView.canGoBack();
        boolean canForward = selectedSite != null && webView != null && webView.canGoForward();
        backButton.setEnabled(canBack);
        backButton.setAlpha(canBack ? 1f : 0.28f);
        forwardButton.setEnabled(canForward);
        forwardButton.setAlpha(canForward ? 1f : 0.28f);
        reloadButton.setText(webView != null && webView.getProgress() < 100 ? "×" : "↻");
    }

    private void recordBlockedRequest() {
        int count = blockedCount.incrementAndGet();
        runOnUiThread(() ->
            statusLabel.setText(getString(R.string.blocked_status, count))
        );
    }

    private boolean isAllowedTopLevel(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        if (scheme == null) return false;
        if ("about".equalsIgnoreCase(scheme)
            || "blob".equalsIgnoreCase(scheme)
            || "data".equalsIgnoreCase(scheme)) {
            return true;
        }
        return "https".equalsIgnoreCase(scheme)
            && selectedSite != null
            && selectedSite.includesHost(uri.getHost());
    }

    private static boolean isBlockedAdHost(String host) {
        if (host == null) return false;
        host = host.toLowerCase(Locale.US);
        return host.equals("b.5ei1zlm7w7ut9bezxfj5.cfd")
            || host.equals("blirtonethe.com")
            || host.endsWith(".blirtonethe.com")
            || host.equals("popads.net")
            || host.endsWith(".popads.net")
            || host.equals("popcash.net")
            || host.endsWith(".popcash.net")
            || host.equals("propellerads.com")
            || host.endsWith(".propellerads.com")
            || host.equals("onclicka.com")
            || host.endsWith(".onclicka.com")
            || host.equals("adsterra.com")
            || host.endsWith(".adsterra.com")
            || host.equals("doubleclick.net")
            || host.endsWith(".doubleclick.net")
            || host.equals("googlesyndication.com")
            || host.endsWith(".googlesyndication.com")
            || host.equals("googleadservices.com")
            || host.endsWith(".googleadservices.com")
            || host.equals("butyrhopers.com")
            || host.endsWith(".butyrhopers.com")
            || host.equals("cutchbatete.com")
            || host.endsWith(".cutchbatete.com")
            || host.equals("rostelshute.shop")
            || host.endsWith(".rostelshute.shop")
            || host.equals("khalatisort.cyou")
            || host.endsWith(".khalatisort.cyou")
            || host.equals("mrdreamzone.com")
            || host.endsWith(".mrdreamzone.com")
            || host.equals("woolderstrolld.qpon")
            || host.endsWith(".woolderstrolld.qpon");
    }

    private void setInlineMediaActive(boolean active) {
        inlineMediaActive = active;
        if (toolbar != null) toolbar.setVisibility(active ? View.GONE : View.VISIBLE);
    }

    private final class MediaStateBridge {
        @JavascriptInterface
        public void setActive(boolean active) {
            runOnUiThread(() -> setInlineMediaActive(active));
        }
    }

    private final class ProtectedWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            setInlineMediaActive(false);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (!request.isForMainFrame()) return false;
            boolean shouldBlock = !isAllowedTopLevel(request.getUrl());
            if (shouldBlock) recordBlockedRequest();
            return shouldBlock;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(
            WebView view,
            WebResourceRequest request
        ) {
            Uri uri = request.getUrl();
            boolean isPopcornAdFeed = selectedSite == Site.POPCORN
                && selectedSite.includesHost(uri.getHost())
                && uri.getPath() != null
                && uri.getPath().startsWith("/api/ads");
            if (isPopcornAdFeed || isBlockedAdHost(uri.getHost())) {
                recordBlockedRequest();
                return new WebResourceResponse(
                    "text/plain",
                    "UTF-8",
                    204,
                    "Blocked",
                    Collections.emptyMap(),
                    new ByteArrayInputStream(new byte[0])
                );
            }
            return null;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            view.evaluateJavascript(PAGE_PROTECTION_SCRIPT, null);
            updateNavigationState();
        }
    }

    private final class ProtectedChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            updateNavigationState();
        }

        @Override
        public boolean onCreateWindow(
            WebView view,
            boolean isDialog,
            boolean isUserGesture,
            Message resultMsg
        ) {
            recordBlockedRequest();
            return false;
        }

        @Override
        public boolean onJsAlert(
            WebView view,
            String url,
            String message,
            JsResult result
        ) {
            recordBlockedRequest();
            result.cancel();
            return true;
        }

        @Override
        public boolean onJsConfirm(
            WebView view,
            String url,
            String message,
            JsResult result
        ) {
            recordBlockedRequest();
            result.cancel();
            return true;
        }

        @Override
        public boolean onJsPrompt(
            WebView view,
            String url,
            String message,
            String defaultValue,
            JsPromptResult result
        ) {
            recordBlockedRequest();
            result.cancel();
            return true;
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }

            customView = view;
            customViewCallback = callback;
            landingScreen.setVisibility(View.GONE);
            browserContent.setVisibility(View.GONE);
            root.addView(customView, matchParentFrameParams());
            enterImmersiveFullscreen();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }
    }

    private void enterImmersiveFullscreen() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    private void leaveImmersiveFullscreen() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void hideCustomView() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        leaveImmersiveFullscreen();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        if (selectedSite == null) {
            landingScreen.setVisibility(View.VISIBLE);
            browserContent.setVisibility(View.GONE);
        } else {
            landingScreen.setVisibility(View.GONE);
            browserContent.setVisibility(View.VISIBLE);
        }

        if (customViewCallback != null) customViewCallback.onCustomViewHidden();
        customViewCallback = null;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && customView != null) enterImmersiveFullscreen();
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            hideCustomView();
        } else if (selectedSite != null && webView.canGoBack()) {
            webView.goBack();
        } else if (selectedSite != null) {
            displayLandingScreen(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (selectedSite != null) {
            outState.putString("selectedSite", selectedSite.id);
            webView.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
