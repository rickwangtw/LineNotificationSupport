package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.common.collect.Lists;
import com.mysticwind.linenotificationsupport.MainActivity;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ImageNotificationPublisherAsyncTask extends AsyncTask<String, Void, Bitmap> {

    private static final String TAG = ImageNotificationPublisherAsyncTask.class.getSimpleName();

    private Context context;
    private LineNotification lineNotification;
    private boolean showGroupNotification;
    private List<CharSequence> currentNotificationMessages;
    private int notificationId;
    private int groupId;

    public ImageNotificationPublisherAsyncTask(Context context,
                                               LineNotification lineNotification,
                                               boolean shouldShowGroupNotification,
                                               List<CharSequence> currentNotificationMessages,
                                               int notificationId, int groupId) {
        super();
        this.context = context;
        this.lineNotification = lineNotification;
        this.showGroupNotification = shouldShowGroupNotification;
        this.currentNotificationMessages = currentNotificationMessages;
        this.notificationId = notificationId;
        this.groupId = groupId;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        if (StringUtils.isBlank(lineNotification.getLineStickerUrl())) {
            return null;
        }

        InputStream in;
        try {
            URL url = new URL(lineNotification.getLineStickerUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            in = connection.getInputStream();
            return BitmapFactory.decodeStream(in);
        } catch (final Exception e) {
            Log.e(TAG, String.format("Failed to download image %s: %s",
                    lineNotification.getLineStickerUrl(), e.getMessage()), e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(Bitmap downloadedImage) {
        super.onPostExecute(downloadedImage);

        final NotificationCompat.Style style = buildMessageStyle(downloadedImage);

        final Intent intent = new Intent(context, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification singleNotification = new NotificationCompat.Builder(context, lineNotification.getChatId())
                .setStyle(style)
                .setContentTitle(lineNotification.getTitle())
                .setContentText(lineNotification.getMessage())
                .setGroup(lineNotification.getChatId())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(lineNotification.getIcon())
                .setContentIntent(pendingIntent)
                .build();

        addActionInNotification(singleNotification);

        createNotificationChannel();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, singleNotification);

        if (showGroupNotification) {
            showGroupNotification();
        }
    }

    private NotificationCompat.Style buildMessageStyle(final Bitmap downloadedImage) {
        if (downloadedImage != null) {
            return new NotificationCompat.BigPictureStyle()
                    .bigPicture(downloadedImage)
                    .setSummaryText(lineNotification.getMessage());
        } else {
            return new NotificationCompat.MessagingStyle(lineNotification.getSender())
                    .setConversationTitle(lineNotification.getTitle())
                    .addMessage(lineNotification.getMessage(),
                            lineNotification.getTimestamp(), lineNotification.getSender());
        }
    }

    private void addActionInNotification(Notification notification) {
        if (lineNotification.getReplyAction() == null) {
            return;
        }
        if (ArrayUtils.isEmpty(notification.actions)) {
            notification.actions = new Notification.Action[] { lineNotification.getReplyAction() };
        } else {
            List<Notification.Action> actions = Lists.newArrayList(notification.actions);
            actions.add(lineNotification.getReplyAction());
            notification.actions = (Notification.Action[]) actions.toArray();
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            final String channelId = lineNotification.getChatId();
            final String channelName = getChannelName();
            final String description = "Notification channel for " + channelName;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private String getChannelName() {
        if (LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID.equals(lineNotification.getChatId())) {
            return "Calls";
        }
        return lineNotification.getTitle();
    }

    private void showGroupNotification() {
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (CharSequence text: currentNotificationMessages) {
            style.addLine(text);
        }
        int groupCount = currentNotificationMessages.size() + 1;
        style.setSummaryText(groupCount + " new notifications");

        Notification groupNotification = new NotificationCompat.Builder(context, lineNotification.getChatId())
                .setStyle(style)
                .setContentTitle(lineNotification.getTitle())
                .setContentText(currentNotificationMessages.get(0))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(lineNotification.getIcon())
                .setGroup(lineNotification.getChatId())
                .setGroupSummary(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(groupId, groupNotification);
    }

}
