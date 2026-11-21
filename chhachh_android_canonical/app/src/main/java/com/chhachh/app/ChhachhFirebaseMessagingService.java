package com.chhachh.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/** Chhachh native push delivery: message/like/comment/follow and announcements. */
public final class ChhachhFirebaseMessagingService extends FirebaseMessagingService {
    public static final String CHANNEL_SOCIAL = "chhachh_updates";
    public static final String CHANNEL_ANNOUNCEMENTS = "chhachh_announcements";
    private static final String BASE = "https://chhachh.com/";
    public static void ensureChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel social = new NotificationChannel(CHANNEL_SOCIAL, "Messages and activity",
                NotificationManager.IMPORTANCE_DEFAULT);
        social.setDescription("Messages, replies, comments, likes, follows and profile updates");
        NotificationChannel announcements = new NotificationChannel(CHANNEL_ANNOUNCEMENTS,
                "Live voice announcements", NotificationManager.IMPORTANCE_HIGH);
        announcements.setDescription("Announcements from Chhachh");
        nm.createNotificationChannel(social);
        nm.createNotificationChannel(announcements);
    }
    @Override public void onNewToken(String token) {
        super.onNewToken(token);
        // Tokens must be associated with an authenticated user by the server.
        // Never send a token over untrusted URLs or log it.
        getSharedPreferences("chhachh_push", MODE_PRIVATE).edit().putString("current_fcm_token", token).apply();
    }
    @Override public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        final Map<String, String> data = message.getData();
        String type = data.get("type");
        if (type == null) type = "activity";
        final String channel = "announcement".equals(type) || "live_announcement".equals(type)
                ? CHANNEL_ANNOUNCEMENTS : CHANNEL_SOCIAL;
        String title = data.get("title");
        String body = data.get("body");
        if (title == null || title.trim().isEmpty()) title =
                message.getNotification() != null ? message.getNotification().getTitle() : "Chhachh";
        if (body == null) body = message.getNotification() != null ?
                message.getNotification().getBody() : "";
        if (title == null) title = "Chhachh";
        if (body == null) body = "";
        String destination = data.get("url");
        Uri uri;
        try { uri = destination == null ? Uri.parse(BASE) : Uri.parse(destination); }
        catch (Exception ignored) { uri = Uri.parse(BASE); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                !("chhachh.com".equalsIgnoreCase(uri.getHost()) ||
                  "www.chhachh.com".equalsIgnoreCase(uri.getHost()))) uri = Uri.parse(BASE);
        ensureChannels(this);
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return;
        Intent tap = new Intent(this, MainActivity.class).setAction(Intent.ACTION_VIEW)
                .setData(uri).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int id = (int) (System.currentTimeMillis() & 0x7fffffff);
        PendingIntent pi = PendingIntent.getActivity(this, id, tap,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new android.app.Notification.Builder(this, channel)
                : new android.app.Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title.length() > 100 ? title.substring(0, 100) : title)
                .setContentText(body.length() > 250 ? body.substring(0, 250) : body)
                .setAutoCancel(true).setContentIntent(pi)
                .setColor(Color.rgb(0, 242, 255));
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(id, b.build());
    }
}
