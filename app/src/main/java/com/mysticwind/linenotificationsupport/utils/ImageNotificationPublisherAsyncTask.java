package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

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
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import timber.log.Timber;

import static android.content.Context.NOTIFICATION_SERVICE;
import static androidx.core.app.NotificationCompat.EXTRA_TEXT;

public class ImageNotificationPublisherAsyncTask extends AsyncTask<String, Void, Uri> {

    private static final String AUTHORITY = "com.mysticwind.linenotificationsupport.fileprovider";


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
    protected void onPostExecute(Uri downloadedImageUri) {
        super.onPostExecute(downloadedImageUri);

        final NotificationCompat.Style style = buildMessageStyle(downloadedImageUri);

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
                .setAutoCancel(true)
                .build();

        addActionInNotification(singleNotification);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, singleNotification);

        if (currentNotificationMessages.size() > 1) {
            showGroupNotification(channelId.orElse(null));
        }
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
                .setAutoCancel(true)
                .build();

        int groupId = groupIdResolver.resolveGroupId(lineNotification.getChatId());

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(groupId, groupNotification);
    }

}
