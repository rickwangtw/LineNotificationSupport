package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.common.collect.Lists;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.line.LineLauncher;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import timber.log.Timber;

public class BigPictureStyleImageSupportedNotificationPublisherAsyncTask extends AsyncTask<String, Void, Bitmap> {

    private static final LineLauncher LINE_LAUNCHER = new LineLauncher();

    private final Context context;
    private final LineNotification lineNotification;
    private final int notificationId;

    public BigPictureStyleImageSupportedNotificationPublisherAsyncTask(final Context context,
                                                                       final LineNotification lineNotification,
                                                                       final int notificationId) {
        super();
        this.context = context;
        this.lineNotification = lineNotification;
        this.notificationId = notificationId;
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
            Timber.e(e, String.format("Failed to download image %s: %s",
                    lineNotification.getLineStickerUrl(), e.getMessage()));
            return null;
        }
    }

    @Override
    protected void onPostExecute(Bitmap downloadedImage) {
        super.onPostExecute(downloadedImage);

        final NotificationCompat.Style style = buildMessageStyle(downloadedImage);

        final Optional<String> channelId = createNotificationChannel();

        Notification singleNotification = new NotificationCompat.Builder(context, lineNotification.getChatId())
                .setStyle(style)
                .setContentTitle(lineNotification.getTitle())
                .setContentText(lineNotification.getMessage())
                .setGroup(lineNotification.getChatId())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(lineNotification.getIcon())
                .setContentIntent(LINE_LAUNCHER.buildPendingIntent(context))
                .setChannelId(channelId.orElse(null))
                .setAutoCancel(true)
                .setWhen(lineNotification.getTimestamp())
                .build();

        addActionInNotification(singleNotification);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, singleNotification);
    }

    private NotificationCompat.Style buildMessageStyle(final Bitmap downloadedImage) {
        if (downloadedImage != null) {
            return new NotificationCompat.BigPictureStyle()
                    .bigPicture(downloadedImage)
                    .setSummaryText(lineNotification.getMessage());
        }  else if (lineNotification.getSender().getName().equals(lineNotification.getTitle())) {
            // this is usually the case if you're talking to a single person.
            // Don't set the conversation title in this case.
            return new NotificationCompat.MessagingStyle(lineNotification.getSender())
                    .addMessage(lineNotification.getMessage(),
                            lineNotification.getTimestamp(), lineNotification.getSender());
        }  else {
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
        if (ArrayUtils.isEmpty(notification.actions)) {
            notification.actions = actionsToAdd.toArray(new Notification.Action[actionsToAdd.size()]);
        } else {
            List<Notification.Action> actions = Lists.newArrayList(notification.actions);
            actions.addAll(actionsToAdd);
            notification.actions = actions.toArray(new Notification.Action[actions.size()]);
        }
    }

    private Optional<String> createNotificationChannel() {
        final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        return new NotificationGroupCreator(notificationManager, new AndroidFeatureProvider(),
                new PreferenceProvider(PreferenceManager.getDefaultSharedPreferences(context)))
                .createNotificationChannel(lineNotification.getChatId(), lineNotification.getTitle());
    }

}
