package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.line.LineLauncher;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;

import timber.log.Timber;

public class ImageNotificationPublisherAsyncTask extends AsyncTask<String, Void, Uri> {

    private static final LineLauncher LINE_LAUNCHER = new LineLauncher();

    private static final String AUTHORITY = "com.mysticwind.linenotificationsupport.donate.fileprovider";

    private final Context context;
    private final LineNotification lineNotification;
    private final int notificationId;

    public ImageNotificationPublisherAsyncTask(final Context context,
                                               final LineNotification lineNotification,
                                               final int notificationId) {
        super();
        this.context = context;
        this.lineNotification = lineNotification;
        this.notificationId = notificationId;
    }

    @Override
    protected Uri doInBackground(String... params) {
        if (StringUtils.isBlank(lineNotification.getLineStickerUrl())) {
            return null;
        }
        // Glide auto-caches
        final FutureTarget<File> target = Glide.with(context)
                .downloadOnly()
                .load(lineNotification.getLineStickerUrl())
                .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);

        // https://stackoverflow.com/questions/63988424/show-an-image-in-messagingstyle-notification-in-android
        try {
            final File file = target.get(); // needs to be called on background thread
            final Uri uri = FileProvider.getUriForFile(context, AUTHORITY, file);
            Timber.i("URL %s downloaded at: %s", lineNotification.getLineStickerUrl(), uri);
            return uri;
        } catch (Exception e) {
            Timber.e(e, String.format("Failed to download image %s: %s",
                    lineNotification.getLineStickerUrl(), e.getMessage()));
            return null;
        }
    }

    @Override
    protected void onPostExecute(Uri downloadedImageUri) {
        super.onPostExecute(downloadedImageUri);

        final NotificationCompat.Style style = buildMessageStyle(downloadedImageUri);

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

    private NotificationCompat.Style buildMessageStyle(final Uri downloadedImageUri) {
        final String conversationTitle;
        if (lineNotification.getSender().getName().equals(lineNotification.getTitle())) {
            // this is usually the case if you're talking to a single person.
            // Don't set the conversation title in this case.
            conversationTitle = null;
        } else {
            conversationTitle = lineNotification.getTitle();
        }

        final NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(lineNotification.getSender())
                        .setConversationTitle(conversationTitle);

        final List<NotificationCompat.MessagingStyle.Message> messages = buildMessages(lineNotification, downloadedImageUri);
        for (final NotificationCompat.MessagingStyle.Message message : messages) {
            messagingStyle.addMessage(message);
        }
        return messagingStyle;
    }

    private List<NotificationCompat.MessagingStyle.Message> buildMessages(final LineNotification lineNotification,
                                                                          final Uri downloadedImageUri) {
        if (downloadedImageUri == null) {
            return ImmutableList.of(
                    new NotificationCompat.MessagingStyle.Message(
                            lineNotification.getMessage(), lineNotification.getTimestamp(), lineNotification.getSender()));
        }

        return ImmutableList.of(
                new NotificationCompat.MessagingStyle.Message(
                        lineNotification.getMessage(), lineNotification.getTimestamp(), lineNotification.getSender())
                        .setData("image/", downloadedImageUri),
                new NotificationCompat.MessagingStyle.Message(
                        lineNotification.getMessage(), lineNotification.getTimestamp(), lineNotification.getSender())
        );
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
