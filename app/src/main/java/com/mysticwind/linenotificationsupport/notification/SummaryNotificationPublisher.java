package com.mysticwind.linenotificationsupport.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;

import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.line.LineLauncher;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import timber.log.Timber;

public class SummaryNotificationPublisher {

    private static final LineLauncher LINE_LAUNCHER = new LineLauncher();

    private final Context context;
    private final NotificationManager notificationManager;
    private final String packageName;
    private final GroupIdResolver groupIdResolver;

    public SummaryNotificationPublisher(final Context context,
                                        final NotificationManager notificationManager,
                                        final String packageName,
                                        final GroupIdResolver groupIdResolver) {
        this.context = context;
        this.notificationManager = notificationManager;
        this.packageName = packageName;
        this.groupIdResolver = groupIdResolver;
    }

    public void updateSummaryWhenNotificationsPublished(final String group) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        if (StringUtils.isBlank(group)) {
            return;
        }

        final List<StatusBarNotification> notifications = Arrays.stream(notificationManager.getActiveNotifications())
                .filter(statusBarNotification -> StringUtils.equals(packageName , statusBarNotification.getPackageName()))
                .filter(statusBarNotification -> StringUtils.equals(group, statusBarNotification.getNotification().getGroup()))
                .filter(statusBarNotification -> !StatusBarNotificationExtractor.isSummary(statusBarNotification))
                .sorted((statusBarNotification1, statusBarNotification2) ->
                        (int) (statusBarNotification1.getNotification().when - statusBarNotification2.getNotification().when))
                .collect(Collectors.toList());

        // summaries are only published if there are more than 2 notifications in the same group
        if (notifications.size() <= 1) {
            return;
        }

        final NotificationCompat.MessagingStyle style = buildMessagingStyleFromHistory(notifications);

        final Notification lastNotification = notifications.get(notifications.size() - 1).getNotification();
        Timber.d("Last notification with message: [%s]", NotificationExtractor.getMessage(lastNotification));

        final Notification groupNotification = new NotificationCompat.Builder(context, group)
                .setStyle(style)
                .setContentTitle(NotificationExtractor.getTitle(lastNotification))
                .setContentText(lastNotification.tickerText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setGroup(lastNotification.getGroup())
                .setGroupSummary(true)
                .setChannelId(lastNotification.getChannelId())
                .setAutoCancel(true)
                .setContentIntent(LINE_LAUNCHER.buildPendingIntent(context))
                .build();

        groupNotification.actions = lastNotification.actions;

        final int groupId = groupIdResolver.resolveGroupId(group);

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(groupId, groupNotification);
    }

    private NotificationCompat.MessagingStyle buildMessagingStyleFromHistory(List<StatusBarNotification> notifications) {
        final Notification lastNotification = notifications.get(notifications.size() - 1).getNotification();
        final NotificationCompat.MessagingStyle messagingStyle =
                new NotificationCompat.MessagingStyle(buildPerson(lastNotification))
                        .setConversationTitle(buildTitle(lastNotification));

        for (final StatusBarNotification notification : notifications) {
            messagingStyle.addMessage(
                    new NotificationCompat.MessagingStyle.Message(
                            NotificationExtractor.getMessage(notification.getNotification()),
                            notification.getNotification().when, buildPerson(notification.getNotification())));
        }

        return messagingStyle;
    }

    private String buildTitle(Notification notification) {
        return notification.extras.getString(Notification.EXTRA_CONVERSATION_TITLE);
    }

    private Person buildPerson(Notification notification) {
        String senderName = notification.extras.getString(Notification.EXTRA_SELF_DISPLAY_NAME);
        return new Person.Builder()
                .setName(senderName)
                .build();
    }

    public void updateSummaryWhenNotificationsDismissed(final String group) {
        // TODO implement this. Do nothing right now due to unnecessary vibrations and occasionally weird summaries to be sent out
    }

}
