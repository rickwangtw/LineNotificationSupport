package com.mysticwind.linenotificationsupport.utils;

import static java.util.Collections.EMPTY_LIST;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.Target;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.conversationstarter.ConversationStarterNotificationManager;
import com.mysticwind.linenotificationsupport.line.LineLauncher;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;
import com.mysticwind.linenotificationsupport.model.NotificationExtraConstants;
import com.mysticwind.linenotificationsupport.model.NotificationHistoryEntry;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import timber.log.Timber;

public class MessageStyleImageSupportedNotificationPublisherAsyncTask extends AsyncTask<String, Void, NotificationCompat.Style> {

    private static final String SINGLE_NOTIFICATION_GROUP = "single-notification-group";

    private static final Set<String> NOT_CHAT_IDS = ImmutableSet.of(
            LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID,
            LineNotificationBuilder.DEFAULT_CHAT_ID,
            ConversationStarterNotificationManager.CONVERSATION_STARTER_CHAT_ID
    );

    private static final LineLauncher LINE_LAUNCHER = new LineLauncher();

    private static final String AUTHORITY = "com.mysticwind.linenotificationsupport.fileprovider";

    private final Context context;
    private final NotificationGroupCreator notificationGroupCreator;
    private final LineNotification lineNotification;
    private final int notificationId;
    private final boolean useSingleNotificationConversations;

    public MessageStyleImageSupportedNotificationPublisherAsyncTask(final Context context,
                                                                    final NotificationGroupCreator notificationGroupCreator,
                                                                    final LineNotification lineNotification,
                                                                    final int notificationId,
                                                                    final boolean useSingleNotificationConversations) {
        super();
        this.context = context;
        this.notificationGroupCreator = Objects.requireNonNull(notificationGroupCreator);
        this.lineNotification = lineNotification;
        this.notificationId = notificationId;
        this.useSingleNotificationConversations = useSingleNotificationConversations;
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

            message.getExtras().putString(NotificationExtraConstants.CHAT_ID, lineNotification.getChatId());
            message.getExtras().putString(NotificationExtraConstants.MESSAGE_ID, entry.getLineMessageId());
            message.getExtras().putString(NotificationExtraConstants.STICKER_URL, entry.getLineStickerUrl().orElse(null));
            message.getExtras().putString(NotificationExtraConstants.SENDER_NAME, entry.getSender().getName().toString());

            messageListBuilder.add(message);
        }

        final List<String> splitMessages = CollectionUtils.isEmpty(lineNotification.getMessages()) ?
                EMPTY_LIST : lineNotification.getMessages();

        // TODO we should be able to drastically reduce duplicated code by compiling the messages first
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
            Timber.e(e, "Failed to download image %s: %s", lineStickerUrl, e.getMessage());
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
                .setGroup(resolveGroup())
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
        Timber.d("Publishing notification id [%d] channel [%s] group [%s] text [%s] timestamp [%d]",
                notificationId,
                singleNotification.getChannelId(),
                singleNotification.getGroup(),
                NotificationExtractor.getMessage(singleNotification),
                singleNotification.when);
        notificationManager.notify(notificationId, singleNotification);
    }

    private PendingIntent resolveContentIntent(final Context context, final LineNotification lineNotification) {
        return lineNotification.getClickIntent().orElse(
                LINE_LAUNCHER.buildPendingIntent(context, lineNotification.getChatId())
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
        if (lineNotification.isSelfResponse()) {
            return notificationGroupCreator.createSelfResponseNotificationChannel();
        } else {
            return notificationGroupCreator.createNotificationChannel(lineNotification.getChatId(), lineNotification.getTitle());
        }
    }

    private String resolveGroup() {
        if (!useSingleNotificationConversations) {
            return lineNotification.getChatId();
        }
        if (NOT_CHAT_IDS.contains(lineNotification.getChatId())) {
            return lineNotification.getChatId();
        }
        return SINGLE_NOTIFICATION_GROUP;
    }

}
