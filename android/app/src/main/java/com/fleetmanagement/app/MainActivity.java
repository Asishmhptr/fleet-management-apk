package com.fleetmanagement.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import androidx.core.app.ActivityCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.WebViewListener;
import java.util.Collections;

public class MainActivity extends BridgeActivity {
    private static final String REGISTER_PLUGIN_POLYFILL =
        "(function() {\n" +
        "  if (typeof window === 'undefined') return;\n" +
        "  window.Capacitor = window.Capacitor || {};\n" +
        "  if (typeof window.Capacitor.registerPlugin !== 'function') {\n" +
        "    window.Capacitor.registerPlugin = function(name) {\n" +
        "      return (window.Capacitor.Plugins && window.Capacitor.Plugins[name]) || null;\n" +
        "    };\n" +
        "  }\n" +
        "})();\n";

    public static final String ALERTS_CHANNEL_ID = "fleet_alerts";
    public static final String TRIP_CHANNEL_ID = "trip_tracking_channel";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createNotificationChannels();
        requestNotificationPermissionOnLaunch();

        WebView webView = getBridge().getWebView();
        if (webView != null) {
            webView.addJavascriptInterface(new FleetNativeInterface(), "FleetNative");

            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                try {
                    WebViewCompat.addDocumentStartJavaScript(
                        webView,
                        REGISTER_PLUGIN_POLYFILL,
                        Collections.singleton("*")
                    );
                } catch (Exception ignored) {}
            }

            getBridge().addWebViewListener(new WebViewListener() {
                @Override
                public void onPageStarted(WebView view) {
                    view.evaluateJavascript(REGISTER_PLUGIN_POLYFILL, null);
                }

                @Override
                public void onPageCommitVisible(WebView view, String url) {
                    view.evaluateJavascript(REGISTER_PLUGIN_POLYFILL, null);
                }

                @Override
                public void onPageLoaded(WebView view) {
                    view.evaluateJavascript(REGISTER_PLUGIN_POLYFILL, null);
                }
            });
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel alertsChannel = new NotificationChannel(
                    ALERTS_CHANNEL_ID,
                    "Fleet alerts",
                    NotificationManager.IMPORTANCE_HIGH
                );
                alertsChannel.setDescription("Trip and vehicle alerts");
                alertsChannel.enableVibration(true);
                alertsChannel.enableLights(true);
                alertsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                nm.createNotificationChannel(alertsChannel);

                NotificationChannel tripChannel = new NotificationChannel(
                    TRIP_CHANNEL_ID,
                    "Trip Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
                );
                tripChannel.setDescription("Shows active trip tracking");
                nm.createNotificationChannel(tripChannel);
            }
        }
    }

    private void requestNotificationPermissionOnLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    101
                );
            }
        }
    }

    public class FleetNativeInterface {
        @JavascriptInterface
        public void requestDriverPermissions() {
            runOnUiThread(() -> {
                promptDriverLocationAndBattery();
            });
        }

        @JavascriptInterface
        public void requestNotificationPermission() {
            runOnUiThread(() -> {
                requestNotificationPermissionOnLaunch();
            });
        }

        @JavascriptInterface
        public boolean isBatteryOptimizationIgnored() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    return pm.isIgnoringBatteryOptimizations(getPackageName());
                }
            }
            return true;
        }

        @JavascriptInterface
        public void requestIgnoreBatteryOptimization() {
            runOnUiThread(() -> {
                promptBatteryOptimization();
            });
        }
    }

    private void promptDriverLocationAndBattery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasFine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            if (!hasFine || !hasCoarse) {
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    102
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                boolean hasBg = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (!hasBg) {
                    ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        103
                    );
                }
            }
        }

        promptBatteryOptimization();
    }

    private void promptBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(intent);
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
