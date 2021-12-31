package com.mysticwind.linenotificationsupport.notification;

import android.content.Context;
import android.os.Handler;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;

import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class NotificationPublisherFactory {

    private NotificationPublisher notificationPublisher = NullNotificationPublisher.INSTANCE;

    private final Context context;
    private final Handler handler;
    private final PreferenceProvider preferenceProvider;
    private final SlotAvailabilityChecker slotAvailabilityChecker;
    private final GroupIdResolver groupIdResolver;
    private final String packageName;

    @Inject
    public NotificationPublisherFactory(@ApplicationContext final Context context,
                                        final Handler handler,
                                        final PreferenceProvider preferenceProvider,
                                        final SlotAvailabilityChecker slotAvailabilityChecker,
                                        final GroupIdResolver groupIdResolver,
                                        @HiltQualifiers.PackageName final String packageName) {
        this.context = Objects.requireNonNull(context);
        this.handler = Objects.requireNonNull(handler);
        this.preferenceProvider = Objects.requireNonNull(preferenceProvider);
        this.slotAvailabilityChecker = Objects.requireNonNull(slotAvailabilityChecker);
        this.groupIdResolver = Objects.requireNonNull(groupIdResolver);
        this.packageName = Validate.notBlank(packageName);
    }

    public NotificationPublisher get() {
        return notificationPublisher;
    }

    public void notifyChange() {
        this.notificationPublisher = buildNotificationPublisherWithPreviousStateRestored(Collections.EMPTY_LIST);
    }

    public void notifyChangeWithExistingNotifications(final List<StatusBarNotification> existingNotifications) {
        this.notificationPublisher = buildNotificationPublisherWithPreviousStateRestored(existingNotifications);
    }

    private NotificationPublisher buildNotificationPublisherWithPreviousStateRestored(final List<StatusBarNotification> existingNotifications) {
        final boolean shouldExecuteMaxNotificationWorkaround = preferenceProvider.shouldExecuteMaxNotificationWorkaround();

        final List<NotificationSentListener> notificationSentListeners = new ArrayList<>();
        // don't enable this for single notification conversations just yet because we may still
        // exceed 25 chats
        if (shouldExecuteMaxNotificationWorkaround) {
            notificationSentListeners.add(new ResendUnsentNotificationsNotificationSentListener(handler, this));
        }

        NotificationPublisher notificationPublisher =
                new SimpleNotificationPublisher(context, packageName, groupIdResolver,
                        preferenceProvider, notificationSentListeners);

        // this should come after HistoryProvidingNotificationPublisherDecorator as it changes the notification ID
        notificationPublisher =
                new DismissActionInjectorNotificationPublisherDecorator(
                        notificationPublisher, context);

        if (preferenceProvider.shouldUseSingleNotificationForConversations()) {
            // do this before LinkActionInjectorNotificationPublisherDecorator
            // so that link mutations are also persisted
            notificationPublisher = new HistoryProvidingNotificationPublisherDecorator(notificationPublisher, existingNotifications);
        }

        notificationPublisher =
                new LinkActionInjectorNotificationPublisherDecorator(
                        notificationPublisher, context);

        if (shouldExecuteMaxNotificationWorkaround) {
            notificationPublisher = new MaxNotificationHandlingNotificationPublisherDecorator(
                    handler, notificationPublisher, slotAvailabilityChecker);
        }

        if (preferenceProvider.shouldUseMessageSplitter()) {
            notificationPublisher = new BigNotificationSplittingNotificationPublisherDecorator(
                    notificationPublisher,
                    preferenceProvider);
        }

        notificationPublisher = new NotificationMergingNotificationPublisherDecorator(notificationPublisher);

        return notificationPublisher;
    }

}
