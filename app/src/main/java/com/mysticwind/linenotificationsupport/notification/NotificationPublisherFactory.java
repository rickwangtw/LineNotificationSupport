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
    private final SimpleNotificationPublisher simpleNotificationPublisher;
    private final Handler handler;
    private final PreferenceProvider preferenceProvider;
    private final SlotAvailabilityChecker slotAvailabilityChecker;

    private ResendUnsentNotificationsNotificationSentListener resendUnsentNotificationsNotificationSentListener;

    @Inject
    public NotificationPublisherFactory(@ApplicationContext final Context context,
                                        final SimpleNotificationPublisher simpleNotificationPublisher,
                                        final Handler handler,
                                        final PreferenceProvider preferenceProvider,
                                        final SlotAvailabilityChecker slotAvailabilityChecker) {
        this.context = Objects.requireNonNull(context);
        this.simpleNotificationPublisher = Objects.requireNonNull(simpleNotificationPublisher);
        this.handler = Objects.requireNonNull(handler);
        this.preferenceProvider = Objects.requireNonNull(preferenceProvider);
        this.slotAvailabilityChecker = Objects.requireNonNull(slotAvailabilityChecker);
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
            resendUnsentNotificationsNotificationSentListener = new ResendUnsentNotificationsNotificationSentListener(handler, this);
            notificationSentListeners.add(resendUnsentNotificationsNotificationSentListener);
        } else {
            resendUnsentNotificationsNotificationSentListener = null;
        }

        NotificationPublisher notificationPublisher = simpleNotificationPublisher;
        simpleNotificationPublisher.setNotificationSentListeners(notificationSentListeners);

        // this should come after HistoryProvidingNotificationPublisherDecorator as it changes the notification ID
        notificationPublisher =
                new DismissActionInjectorNotificationPublisherDecorator(
                        notificationPublisher, context);

        if (preferenceProvider.shouldUseSingleNotificationForConversations()) {
            // do this before LinkActionInjectorNotificationPublisherDecorator
            // so that link mutations are also persisted
            notificationPublisher = new HistoryProvidingNotificationPublisherDecorator(
                    notificationPublisher, preferenceProvider, existingNotifications);
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

    public void trackNotificationPublished(int id) {
        if (resendUnsentNotificationsNotificationSentListener != null) {
            resendUnsentNotificationsNotificationSentListener.notificationReceived(id);
        }
    }

}
