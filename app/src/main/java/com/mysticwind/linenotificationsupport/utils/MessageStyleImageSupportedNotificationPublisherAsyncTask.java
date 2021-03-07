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
import com.mysticwind.linenotificationsupport.model.NotificationHistoryEntry;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;

import timber.log.Timber;

public class MessageStyleImageSupportedNotificationPublisherAsyncTask extends AsyncTask<String, Void, NotificationCompat.Style> {

    private static final LineLauncher LINE_LAUNCHER = new LineLauncher();

    private static final String AUTHORITY = "com.mysticwind.linenotificationsupport.fileprovider";

    private final Context context;
    private final LineNotification lineNotification;
    private final int notificationId;

    public MessageStyleImageSupportedNotificationPublisherAsyncTask(final Context context,
                                                                    final LineNotification lineNotification,
                                                                    final int notificationId) {
        super();
        this.context = context;
        this.lineNotification = lineNotification;
        this.notificationId = notificationId;
    }

    @Override
    protected NotificationCompat.Style doInBackground(String... params) {
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

        final List<NotificationCompat.MessagingStyle.Message> messages = buildMessages();
        for (final NotificationCompat.MessagingStyle.Message message : messages) {
            messagingStyle.addMessage(message);
        }
        return messagingStyle;
    }

    private List<NotificationCompat.MessagingStyle.Message> buildMessages() {
        final ImmutableList.Builder<NotificationCompat.MessagingStyle.Message> messageListBuilder = ImmutableList.builder();
        for (final NotificationHistoryEntry entry : lineNotification.getHistory()) {

            final NotificationCompat.MessagingStyle.Message message =
                    new NotificationCompat.MessagingStyle.Message(
                            entry.getMessage(), entry.getTimestamp(), entry.getSender());

            entry.getLineStickerUrl().ifPresent(url ->
                    getLineStickerUri(url).ifPresent(uri ->
                            message.setData("image/", uri)
                    )
            );

            messageListBuilder.add(message);
        }

        final List<String> splitMessages = CollectionUtils.isEmpty(lineNotification.getMessages()) ?
                ImmutableList.of(lineNotification.getMessage()) : lineNotification.getMessages();

        if (splitMessages.isEmpty()) {
            final NotificationCompat.MessagingStyle.Message message = new NotificationCompat.MessagingStyle.Message(
                    lineNotification.getMessage(), lineNotification.getTimestamp(), lineNotification.getSender());
            if (StringUtils.isNotBlank(lineNotification.getLineStickerUrl())) {
                getLineStickerUri(lineNotification.getLineStickerUrl()).ifPresent(uri ->
                        message.setData("image/", uri)
                );
            }
            messageListBuilder.add(message);
        } else {
            // this also means we don't support attaching the sticker to split messages. We probably
            // don't need to support that.
            for (final String message : splitMessages) {
                messageListBuilder.add(
                        new NotificationCompat.MessagingStyle.Message(
                                message, lineNotification.getTimestamp(), lineNotification.getSender())
                );
            }
        }

        return messageListBuilder.build();
    }

    private Optional<Uri> getLineStickerUri(final String lineStickerUrl) {
        // Glide auto-caches
        final FutureTarget<File> target = Glide.with(context)
                .downloadOnly()
                .load(lineStickerUrl)
                .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);

        // https://stackoverflow.com/questions/63988424/show-an-image-in-messagingstyle-notification-in-android
        try {
            final File file = target.get(); // needs to be called on background thread
            final Uri uri = FileProvider.getUriForFile(context, AUTHORITY, file);
            Timber.i("URL %s downloaded at: %s", lineStickerUrl, uri);
            return Optional.of(uri);
        } catch (Exception e) {
            Timber.e(e, String.format("Failed to download image %s: %s",
                    lineStickerUrl, e.getMessage()));
            return Optional.empty();
        }
    }

    @Override
    protected void onPostExecute(NotificationCompat.Style notificationStyle) {
        super.onPostExecute(notificationStyle);

        final Optional<String> channelId = createNotificationChannel();

        final Notification singleNotification = new NotificationCompat.Builder(context, lineNotification.getChatId())
                .setStyle(notificationStyle)
                .setContentTitle(lineNotification.getTitle())
                .setContentText(lineNotification.getMessage())
                .setGroup(lineNotification.getChatId())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(lineNotification.getIcon())
                .setContentIntent(LINE_LAUNCHER.buildPendingIntent(context, lineNotification.getChatId()))
                .setChannelId(channelId.orElse(null))
                .setAutoCancel(true)
                .setWhen(lineNotification.getTimestamp())
                .build();

        addActionInNotification(singleNotification);
        if (lineNotification.getMessages().size() > 1) {
            Timber.w("Multi-messages, override the tickerText to be the first page [%s]",
                    lineNotification.getMessages().get(0));
            singleNotification.extras.putString(Notification.EXTRA_TEXT, lineNotification.getMessages().get(0));
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, singleNotification);
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
