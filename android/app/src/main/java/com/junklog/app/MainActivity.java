package com.junklog.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Notifier.createChannel(this);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.loadUrl("file:///android_asset/index.html");

        setContentView(web);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    private boolean notificationsAllowed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        runOnUiThread(() -> {
            if (web != null) web.evaluateJavascript(
                "window.__refreshPermission && window.__refreshPermission()", null);
        });
    }

    /** Everything the web app can call, exposed as window.AndroidBridge. */
    public class Bridge {

        @JavascriptInterface
        public boolean hasPermission() {
            return notificationsAllowed();
        }

        @JavascriptInterface
        public void requestPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed()) {
                runOnUiThread(() -> ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101));
            }
        }

        @JavascriptInterface
        public void notify(String title, String body) {
            Notifier.show(MainActivity.this, title, body);
        }

        /** time arrives as "HH:mm". */
        @JavascriptInterface
        public void setReminder(boolean enabled, String time) {
            int hour = 23, minute = 0;
            try {
                String[] bits = time.split(":");
                hour = Integer.parseInt(bits[0]);
                minute = Integer.parseInt(bits[1]);
            } catch (Exception ignored) { }

            Scheduler.save(MainActivity.this, enabled, hour, minute);
            if (enabled) Scheduler.schedule(MainActivity.this, hour, minute);
            else Scheduler.cancel(MainActivity.this);
        }
    }
}
