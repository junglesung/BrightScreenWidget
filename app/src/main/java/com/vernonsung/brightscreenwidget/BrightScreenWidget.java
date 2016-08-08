package com.vernonsung.brightscreenwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.List;
import java.util.Objects;

/**
 * Implementation of App Widget functionality.
 */
public class BrightScreenWidget extends AppWidgetProvider {
    static final String LOG_TAG = "testtest";
    static final String SHARED_PREFERENCE_NAME = "MainSharedPreference";
    static final String SETTING_SCREEN_BRIGHTNESS = "ScreenBrightness";           // int: 0~255
    static final String SETTING_SCREEN_BRIGHTNESS_MODE = "ScreenBrightnessMode";  // Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC or Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
    static final String ON_CLICK_INTENT_ACTION = "com.vernonsung.brightscreenwidget.CLICK";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        CharSequence widgetText = context.getString(R.string.up);
        if (isLightUp(context)) {
            Log.d(LOG_TAG, "Show down");
            widgetText = context.getString(R.string.down);
        }
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.bright_screen_widget);
        views.setTextViewText(R.id.appwidget_text, widgetText);

        // Send intent when user click the widget
        Intent intent = new Intent(context, BrightScreenWidget.class);
        intent.setAction(ON_CLICK_INTENT_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
        Log.d(LOG_TAG, "Widget updated");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "Received an intent");
        if (Objects.equals(intent.getAction(), ON_CLICK_INTENT_ACTION)) {
            Log.d(LOG_TAG, "CLICK action");
            changeLight(context);
        }
        super.onReceive(context,intent);
    }

    static void setScreenBrightnessManual(Context context) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    static void setScreenBrightnessMax(Context context) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
    }

    /**
     * Store current screen brightness settings to shared preference.
     * @param context
     */
    static void rememberCurrentScreenBrightnessSetting(Context context) {
        int brightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        int mode = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(SETTING_SCREEN_BRIGHTNESS, brightness);
        editor.putInt(SETTING_SCREEN_BRIGHTNESS_MODE, mode);
        editor.apply();
    }

    /**
     * Restore last screen brightness settings from shared preference.
     * @param context
     */
    static void restoreScreenBrightnessSetting(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        int brightness = settings.getInt(SETTING_SCREEN_BRIGHTNESS, 128);
        int mode = settings.getInt(SETTING_SCREEN_BRIGHTNESS_MODE, 0);
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }

    /**
     * Maximize the screen brightness
     * @param context
     */
    static void lightUp(Context context) {
        Log.d(LOG_TAG, "Light up");
        rememberCurrentScreenBrightnessSetting(context);
        setScreenBrightnessManual(context);
        setScreenBrightnessMax(context);
    }

    static void lightDown(Context context) {
        Log.d(LOG_TAG, "Light down");
        restoreScreenBrightnessSetting(context);
    }

    static boolean isLightUp(Context context) {
        return (255 == Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0) &&
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL == Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0));
    }

    static void changeLight(Context context) {
        if (!requestWriteSettingPermission(context)) {
            return;
        }
        if (isLightUp(context)) {
            lightDown(context);
        } else {
            lightUp(context);
        }
        changeWidgetIcon(context);
    }

    static void changeWidgetIcon(Context context) {
        // Simulate that Android updates widgets.
        // Send an explicit broadcast intent to myself
        // with action AppWidgetManager.ACTION_APPWIDGET_UPDATE
        // and widget ID array as an extra.
        Intent intent = new Intent(context, BrightScreenWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, BrightScreenWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    /**
     * From Android 6.0 (API 23), permissions are dynamically granted.
     * To modify system setting, this widget must be granted permission ACTION_MANAGE_WRITE_SETTINGS.
     * The protection level of ACTION_MANAGE_WRITE_SETTINGS is "signature".
     * So do what it says. https://developer.android.com/reference/android/Manifest.permission.html#WRITE_SETTINGS
     * @param context
     * @return Whether WRITE_SETTINGS is granted.
     */
    static boolean requestWriteSettingPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (Settings.System.canWrite(context)) {
            return true;
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Check activities to handle the intent
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
            if (activities.size() > 0) {
                context.startActivity(intent);
                Toast.makeText(context, R.string.allow_the_app_to_change_screen_brightness, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, R.string.this_phone_forbids_system_setting_modification, Toast.LENGTH_LONG).show();
            }
            return false;
        }
    }
}

