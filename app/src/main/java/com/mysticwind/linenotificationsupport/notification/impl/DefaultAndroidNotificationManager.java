package com.mysticwind.linenotificationsupport.notification.impl;

import android.app.NotificationManager;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.notification.AndroidNotificationManager;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;

import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class DefaultAndroidNotificationManager implements AndroidNotificationManager {

    private final NotificationManager notificationManager;
    private final String packageName;

    @Inject
    public DefaultAndroidNotificationManager(final NotificationManager notificationManager,
                                             @HiltQualifiers.PackageName final String packageName) {
        this.notificationManager = Objects.requireNonNull(notificationManager);
        this.packageName = Validate.notBlank(packageName);
    }

    @Override
    public void cancelNotification(final String chatId) {
        Validate.notBlank(chatId);

        final Optional<StatusBarNotification> statusBarNotification = Arrays.stream(notificationManager.getActiveNotifications())
                .filter(notification -> packageName.equals(notification.getPackageName()))
                .filter(notification -> chatId.equals(
                        NotificationExtractor.getLineNotificationSupportChatId(notification.getNotification()).orElse(null)))
                .findFirst();
        statusBarNotification.ifPresent(notification -> {
                    Timber.d("Cancel notification with ID [%d] for chat [%s]", notification.getId(), chatId);
                    notificationManager.cancel(notification.getId());
                }
        );
    }

    @Override
    public void clearRemoteInputNotificationSpinner(final String chatId) {
        Validate.notBlank(chatId);

        final Optional<StatusBarNotification> statusBarNotification = Arrays.stream(notificationManager.getActiveNotifications())
                .filter(notification -> packageName.equals(notification.getPackageName()))
                .filter(notification -> chatId.equals(
                        NotificationExtractor.getLineNotificationSupportChatId(notification.getNotification()).orElse(null)))
                .findFirst();
        statusBarNotification.ifPresent(notification -> {
                    Timber.d("Clear notification spinner with ID [%d]", notification.getId());
                    notificationManager.notify(notification.getId(), notification.getNotification());
                }
        );
    }

}
