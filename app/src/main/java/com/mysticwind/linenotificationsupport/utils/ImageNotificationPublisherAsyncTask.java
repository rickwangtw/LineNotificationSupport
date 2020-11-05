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
import android.service.notification.StatusBarNotification;
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
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;
import static androidx.core.app.NotificationCompat.EXTRA_TEXT;

public class ImageNotificationPublisherAsyncTask extends AsyncTask<String, Void, Bitmap> {

    private static final String TAG = ImageNotificationPublisherAsyncTask.class.getSimpleName();

    private final Context context;
    private final LineNotification lineNotification;
    private final List<CharSequence> currentNotificationMessages = new ArrayList<>();
    private final int notificationId;
    private final GroupIdResolver groupIdResolver;
    private final boolean shouldReverseActionOrder;

    public ImageNotificationPublisherAsyncTask(final Context context,
                                               final LineNotification lineNotification,
                                               final int notificationId,
                                               final GroupIdResolver groupIdResolver,
                                               final boolean shouldReverseActionOrder) {
        super();
        this.context = context;
        this.lineNotification = lineNotification;
        this.notificationId = notificationId;
        this.groupIdResolver = groupIdResolver;
        this.shouldReverseActionOrder = shouldReverseActionOrder;
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
    protected void onPreExecute() {
        super.onPreExecute();

        if (StringUtils.isBlank(lineNotification.getChatId())) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        currentNotificationMessages.add(lineNotification.getMessage());

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        final int groupId = groupIdResolver.resolveGroupId(lineNotification.getChatId());
        for (final StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
            if (statusBarNotification.getId() != groupId &&
                    lineNotification.getChatId().equalsIgnoreCase(statusBarNotification.getNotification().getGroup())) {
                currentNotificationMessages.add(statusBarNotification.getNotification().extras.getCharSequence(EXTRA_TEXT));
                break;
            }
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

        if (currentNotificationMessages.size() > 1) {
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
        if (lineNotification.getActions().isEmpty()) {
            return;
        }
        final List<Notification.Action> actionsToAdd = lineNotification.getActions();
        if (shouldReverseActionOrder && actionsToAdd.size() >= 2) {
            Notification.Action firstAction = actionsToAdd.get(0);
            actionsToAdd.add(0, actionsToAdd.get(1));
            actionsToAdd.add(1, firstAction);
        }
        if (ArrayUtils.isEmpty(notification.actions)) {
            notification.actions = actionsToAdd.toArray(new Notification.Action[actionsToAdd.size()]);
        } else {
            List<Notification.Action> actions = Lists.newArrayList(notification.actions);
            actions.addAll(actionsToAdd);
            notification.actions = actions.toArray(new Notification.Action[actions.size()]);
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
            channel.enableVibration(true);
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


        int groupId = groupIdResolver.resolveGroupId(lineNotification.getChatId());

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(groupId, groupNotification);
    }

}
