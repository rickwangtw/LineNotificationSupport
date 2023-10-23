package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.line.LineLauncher;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.NotificationExtraConstants;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import timber.log.Timber;

public class BigPictureStyleImageSupportedNotificationPublisherAsyncTask extends AsyncTask<String, Void, Bitmap> {

    private static final LineLauncher LINE_LAUNCHER = new LineLauncher();

    private final Context context;
    private final NotificationGroupCreator notificationGroupCreator;
    private final LineNotification lineNotification;
    private final int notificationId;

    public BigPictureStyleImageSupportedNotificationPublisherAsyncTask(final Context context,
                                                                       final NotificationGroupCreator notificationGroupCreator,
                                                                       final LineNotification lineNotification,
                                                                       final int notificationId) {
        super();
        this.context = context;
        this.notificationGroupCreator = Objects.requireNonNull(notificationGroupCreator);
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
            Timber.e(e, "Failed to download image %s: %s", lineNotification.getLineStickerUrl(), e.getMessage());
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
                .setSmallIcon(R.drawable.ic_new_message)
                .setLargeIcon(lineNotification.getIcon())
                .setContentIntent(resolveContentIntent(context, lineNotification))
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
        singleNotification.extras.putString(NotificationExtraConstants.CHAT_ID, lineNotification.getChatId());
        singleNotification.extras.putString(NotificationExtraConstants.MESSAGE_ID, lineNotification.getLineMessageId());
        singleNotification.extras.putString(NotificationExtraConstants.STICKER_URL, lineNotification.getLineStickerUrl());
        singleNotification.extras.putString(NotificationExtraConstants.SENDER_NAME, lineNotification.getSender().getName().toString());

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, singleNotification);
    }

    private NotificationCompat.Style buildMessageStyle(final Bitmap downloadedImage) {
        if (downloadedImage != null) {
            return new NotificationCompat.BigPictureStyle()
                    .bigPicture(downloadedImage)
                    .setSummaryText(lineNotification.getMessage());
        }

        final NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(lineNotification.getSender());

        buildMessages(lineNotification).forEach(message ->
                messagingStyle.addMessage(message)
        );

        if (!lineNotification.getSender().getName().equals(lineNotification.getTitle())) {
            // Don't set the conversation title for single person chat
            messagingStyle.setConversationTitle(lineNotification.getTitle());
        }
        return messagingStyle;
    }

    private List<NotificationCompat.MessagingStyle.Message> buildMessages(final LineNotification lineNotification) {
        final List<String> messages = CollectionUtils.isEmpty(lineNotification.getMessages()) ?
                ImmutableList.of(lineNotification.getMessage()) : lineNotification.getMessages();
        final ImmutableList.Builder<NotificationCompat.MessagingStyle.Message> messageListBuilder = ImmutableList.builder();
        for (final String message : messages) {
            final NotificationCompat.MessagingStyle.Message messagingStyleMessage =
                    new NotificationCompat.MessagingStyle.Message(
                            message, lineNotification.getTimestamp(), lineNotification.getSender());
            messagingStyleMessage.getExtras().putString(NotificationExtraConstants.CHAT_ID, lineNotification.getChatId());
            messagingStyleMessage.getExtras().putString(NotificationExtraConstants.MESSAGE_ID, lineNotification.getLineMessageId());
            messagingStyleMessage.getExtras().putString(NotificationExtraConstants.STICKER_URL, lineNotification.getLineStickerUrl());
            messagingStyleMessage.getExtras().putString(NotificationExtraConstants.SENDER_NAME, lineNotification.getSender().getName().toString());

            messageListBuilder.add(messagingStyleMessage);
        }
        return messageListBuilder.build();
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
        if (lineNotification.isSelfResponse()) {
            return notificationGroupCreator.createSelfResponseNotificationChannel();
        } else {
            return notificationGroupCreator.createNotificationChannel(lineNotification.getChatId(), lineNotification.getTitle());
        }
    }

    private PendingIntent resolveContentIntent(final Context context, final LineNotification lineNotification) {
        return lineNotification.getClickIntent().orElse(
                LINE_LAUNCHER.buildPendingIntent(context, lineNotification.getChatId())
        );
    }

}
