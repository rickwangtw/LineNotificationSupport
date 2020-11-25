package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.common.collect.Lists;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import timber.log.Timber;

import static android.content.Context.NOTIFICATION_SERVICE;
import static androidx.core.app.NotificationCompat.EXTRA_TEXT;

public class ImageNotificationPublisherAsyncTask extends AsyncTask<String, Void, Bitmap> {

    private final Context context;
    private final String lineNotificationSupportPackageName;
    private final LineNotification lineNotification;
    private final List<CharSequence> currentNotificationMessages = new ArrayList<>();
    private final int notificationId;
    private final GroupIdResolver groupIdResolver;

    public ImageNotificationPublisherAsyncTask(final Context context,
                                               final String packageName,
                                               final LineNotification lineNotification,
                                               final int notificationId,
                                               final GroupIdResolver groupIdResolver) {
        super();
        this.context = context;
        this.lineNotificationSupportPackageName = packageName;
        this.lineNotification = lineNotification;
        this.notificationId = notificationId;
        this.groupIdResolver = groupIdResolver;
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

        List<String> previousNotifications = Arrays.stream(notificationManager.getActiveNotifications())
                .filter(statusBarNotification -> StringUtils.equals(lineNotificationSupportPackageName , statusBarNotification.getPackageName()))
                .filter(statusBarNotification -> StringUtils.equals(lineNotification.getChatId(), statusBarNotification.getNotification().getGroup()))
                .filter(statusBarNotification -> !StatusBarNotificationExtractor.isSummary(statusBarNotification))
                .map(statusBarNotification -> (String) statusBarNotification.getNotification().extras.getCharSequence(EXTRA_TEXT))
                .collect(Collectors.toList());

        currentNotificationMessages.addAll(previousNotifications);
    }

    @Override
    protected void onPostExecute(Bitmap downloadedImage) {
        super.onPostExecute(downloadedImage);

        final NotificationCompat.Style style = buildMessageStyle(downloadedImage);

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://line.me/R/nv/chat"));
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        final Optional<String> channelId = createNotificationChannel();

        Notification singleNotification = new NotificationCompat.Builder(context, lineNotification.getChatId())
                .setStyle(style)
                .setContentTitle(lineNotification.getTitle())
                .setContentText(lineNotification.getMessage())
                .setGroup(lineNotification.getChatId())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(lineNotification.getIcon())
                .setContentIntent(pendingIntent)
                .setChannelId(channelId.orElse(null))
                .build();

        addActionInNotification(singleNotification);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, singleNotification);

        if (currentNotificationMessages.size() > 1) {
            showGroupNotification(channelId.orElse(null));
        }
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

    private void showGroupNotification(String channelId) {
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (CharSequence text: currentNotificationMessages) {
            style.addLine(text);
        }
        style.setSummaryText(currentNotificationMessages.size() + " new notifications");

        Notification groupNotification = new NotificationCompat.Builder(context, lineNotification.getChatId())
                .setStyle(style)
                .setContentTitle(lineNotification.getTitle())
                .setContentText(currentNotificationMessages.get(0))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(lineNotification.getIcon())
                .setGroup(lineNotification.getChatId())
                .setGroupSummary(true)
                .setChannelId(channelId)
                .build();


        int groupId = groupIdResolver.resolveGroupId(lineNotification.getChatId());

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(groupId, groupNotification);
    }

}
