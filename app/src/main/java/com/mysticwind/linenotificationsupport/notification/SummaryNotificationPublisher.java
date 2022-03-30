package com.mysticwind.linenotificationsupport.notification;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;

import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@Singleton
public class SummaryNotificationPublisher {

    private final Context context;
    private final AndroidNotificationManager androidNotificationManager;
    private final String packageName;
    private final GroupIdResolver groupIdResolver;

    @Inject
    public SummaryNotificationPublisher(@ApplicationContext final Context context,
                                        final AndroidNotificationManager androidNotificationManager,
                                        @HiltQualifiers.PackageName final String packageName,
                                        final GroupIdResolver groupIdResolver) {
        this.context = context;
        this.androidNotificationManager = androidNotificationManager;
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

        final List<StatusBarNotification> notifications =
                androidNotificationManager.getOrderedLineNotificationSupportNotifications(group, NotificationFilterStrategy.EXCLUDE_SUMMARY);

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
                .setContentIntent(lastNotification.contentIntent)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                .build();


        groupNotification.actions = lastNotification.actions;

        final int groupId = groupIdResolver.resolveGroupId(group);

        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(groupId, groupNotification);

        Timber.d("Created/Updated summary group id [%d] channel [%s] group [%s] text [%s]",
                groupId,
                groupNotification.getChannelId(),
                groupNotification.getGroup(),
                groupNotification.tickerText);
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
        final List<StatusBarNotification> notifications = androidNotificationManager.getOrderedLineNotificationSupportNotifications(group, NotificationFilterStrategy.ALL);

        final long nonSummaryNotificationCount = notifications.stream()
                .filter(notification -> !StatusBarNotificationExtractor.isSummary(notification))
                .count();

        if (nonSummaryNotificationCount > 0) {
            // do nothing if the summary should still exist (we should show even there is only one notification)
            return;
        }

        // cancel the group summary
        notifications.stream()
                .filter(notification -> StatusBarNotificationExtractor.isSummary(notification))
                .forEach(notification -> {
                            Timber.d("Notification group [%s] remaining [%d] dismissing group [%d]",
                                    group, nonSummaryNotificationCount, notification.getId());
                            androidNotificationManager.cancelNotificationById(notification.getId());
                        }
                );
    }

}
