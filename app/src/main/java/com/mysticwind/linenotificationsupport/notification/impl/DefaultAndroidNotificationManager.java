package com.mysticwind.linenotificationsupport.notification.impl;

import android.app.NotificationManager;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.notification.AndroidNotificationManager;
import com.mysticwind.linenotificationsupport.notification.NotificationFilterStrategy;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class DefaultAndroidNotificationManager implements AndroidNotificationManager {

    private final NotificationManager notificationManager;
    private final String packageName;

    private Supplier<List<StatusBarNotification>> otherPackageNotificationSupplier;
    private Consumer<String> otherPackageNotificationCanceller;

    @Inject
    public DefaultAndroidNotificationManager(final NotificationManager notificationManager,
                                             @HiltQualifiers.PackageName final String packageName) {
        this.notificationManager = Objects.requireNonNull(notificationManager);
        this.packageName = Validate.notBlank(packageName);
    }

    public void initialize(final Supplier<List<StatusBarNotification>> otherPackageNotificationSupplier,
                           final Consumer<String> otherPackageNotificationCanceller) {
        this.otherPackageNotificationSupplier = Objects.requireNonNull(otherPackageNotificationSupplier);
        this.otherPackageNotificationCanceller = Objects.requireNonNull(otherPackageNotificationCanceller);
    }


    @Override
    public List<StatusBarNotification> getNotificationsOfPackage(final String packageName) {
        if (otherPackageNotificationSupplier == null) {
            Timber.w("Cannot fetch notifications of package [%s] - otherPackageNotificationSupplier not initialized", packageName);
            return Collections.EMPTY_LIST;
        }
        return otherPackageNotificationSupplier.get().stream()
                .filter(notification -> notification.getPackageName().equals(packageName))
                .collect(Collectors.toList());
    }

    @Override
    public List<StatusBarNotification> getOrderedLineNotificationSupportNotificationsOfChatId(final String chatId, int notificationFilterStrategy) {
        return Arrays.stream(notificationManager.getActiveNotifications())
                .filter(statusBarNotification -> StringUtils.equals(packageName , statusBarNotification.getPackageName()))
                .filter(statusBarNotification -> chatId.equals(NotificationExtractor.getLineNotificationSupportChatId(statusBarNotification.getNotification()).orElse(null)))
                .filter(statusBarNotification -> applyFilter(statusBarNotification, notificationFilterStrategy))
                .sorted((statusBarNotification1, statusBarNotification2) ->
                        (int) (statusBarNotification1.getNotification().when - statusBarNotification2.getNotification().when))
                .collect(Collectors.toList());
    }

    @Override
    public List<StatusBarNotification> getOrderedLineNotificationSupportNotifications(final String group, int notificationFilterStrategy) {
        return Arrays.stream(notificationManager.getActiveNotifications())
                .filter(statusBarNotification -> StringUtils.equals(packageName , statusBarNotification.getPackageName()))
                .filter(statusBarNotification -> StringUtils.equals(group, statusBarNotification.getNotification().getGroup()))
                .filter(statusBarNotification -> applyFilter(statusBarNotification, notificationFilterStrategy))
                .sorted((statusBarNotification1, statusBarNotification2) ->
                        (int) (statusBarNotification1.getNotification().when - statusBarNotification2.getNotification().when))
                .collect(Collectors.toList());
    }

    private boolean applyFilter(final StatusBarNotification statusBarNotification, final int notificationFilterStrategy) {
        if ((notificationFilterStrategy & NotificationFilterStrategy.EXCLUDE_SUMMARY) > 0) {
            return !StatusBarNotificationExtractor.isSummary(statusBarNotification);
        }
        return true;
    }

    @Override
    public void cancelNotificationById(int notificationId) {
        notificationManager.cancel(notificationId);
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
    public void cancelNotificationOfPackage(final String key) {
        Validate.notBlank(key);

        if (otherPackageNotificationCanceller == null) {
            Timber.w("Cannot cancel notification of key [%s] - otherPackageNotificationCanceller not initialized", key);
            return;
        }

        otherPackageNotificationCanceller.accept(key);
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
