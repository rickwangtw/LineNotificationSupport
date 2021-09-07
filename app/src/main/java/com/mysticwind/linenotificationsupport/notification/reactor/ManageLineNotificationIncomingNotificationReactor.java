package com.mysticwind.linenotificationsupport.notification.reactor;

import static com.mysticwind.linenotificationsupport.line.Constants.LINE_PACKAGE_NAME;

import android.os.Handler;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import timber.log.Timber;

public class ManageLineNotificationIncomingNotificationReactor implements IncomingNotificationReactor {

    private static final long LINE_NOTIFICATION_DISMISS_RETRY_TIMEOUT = 500L;
    private static final long PRINT_LINE_NOTIFICATION_WAIT_TIME = 200L;

    private static final Set<String> INTERESTED_PACKAGES = ImmutableSet.of(Constants.LINE_PACKAGE_NAME);

    private final PreferenceProvider preferenceProvider;
    private final Handler handler;
    private final Supplier<List<StatusBarNotification>> activeStatusBarNotificationSupplier;
    private final Consumer<String> notificationCanceller;

    public ManageLineNotificationIncomingNotificationReactor(final PreferenceProvider preferenceProvider,
                                                             final Handler handler,
                                                             final Supplier<List<StatusBarNotification>> activeStatusBarNotificationSupplier,
                                                             final Consumer<String> notificationCanceller) {
        this.preferenceProvider = Objects.requireNonNull(preferenceProvider);
        this.handler = Objects.requireNonNull(handler);
        this.activeStatusBarNotificationSupplier = Objects.requireNonNull(activeStatusBarNotificationSupplier);
        this.notificationCanceller = Objects.requireNonNull(notificationCanceller);
    }

    @Override
    public Collection<String> interestedPackages() {
        return INTERESTED_PACKAGES;
    }

    @Override
    public boolean isInterestInNotificationGroup() {
        return false;
    }

    @Override
    public Reaction reactToIncomingNotification(StatusBarNotification statusBarNotification) {
        if (shouldScheduleRefetch(statusBarNotification)) {
            // Don't dismiss LINE notifications as we'll need to re-fetch them
            return Reaction.NONE;
        }

        if (preferenceProvider.shouldManageLineMessageNotifications()) {
            dismissLineNotification(statusBarNotification);

            // there are situations where LINE messages are not dismissed, do this again
            handler.postDelayed(
                    () -> {
                        Timber.d("Retry dismissing LINE notifications again: key [%s] message [%s]",
                                statusBarNotification.getKey(), statusBarNotification.getNotification().tickerText);
                        dismissLineNotification(statusBarNotification);
                    },
                    LINE_NOTIFICATION_DISMISS_RETRY_TIMEOUT);
        }
        return Reaction.NONE;
    }

    // TODO share this method with NotificationListenerService
    private boolean shouldScheduleRefetch(final StatusBarNotification statusBarNotification) {
        // There are notifications that will not have actions and don't need to retry.
        // For example: notifications of someone added to a chat
        if (StringUtils.isBlank(NotificationExtractor.getLineMessageId(statusBarNotification.getNotification()))) {
            return false;
        }
        return StatusBarNotificationExtractor.isMessage(statusBarNotification) &&
                statusBarNotification.getNotification().actions == null;
    }

    private void dismissLineNotification(final StatusBarNotification statusBarNotification) {
        // we only dismiss notifications that are in the message category
        if (!StatusBarNotificationExtractor.isMessage(statusBarNotification)) {
            Timber.d("LINE notification not message category but [%s]: [%s]",
                    statusBarNotification.getNotification().category, statusBarNotification.getNotification().tickerText);
            return;
        }

        final Optional<String> summaryKey = findLineNotificationSummary(statusBarNotification.getNotification().getGroup());
        summaryKey.ifPresent(
                key -> {
                    Timber.d("Cancelling LINE summary: [%s]", key);
                    notificationCanceller.accept(key);
                }
        );

        Timber.d("Dismiss LINE notification: key[%s] tag[%s] id[%d]",
                statusBarNotification.getKey(), statusBarNotification.getTag(), statusBarNotification.getId());
        notificationCanceller.accept(statusBarNotification.getKey());

        handler.postDelayed(
                () -> printLineNotifications(statusBarNotification.getNotification().getGroup()),
                PRINT_LINE_NOTIFICATION_WAIT_TIME);
    }

    private Optional<String> findLineNotificationSummary(String group) {
        return activeStatusBarNotificationSupplier.get().stream()
                .filter(notification -> notification.getPackageName().equals(LINE_PACKAGE_NAME))
                .peek(notification -> Timber.d("LINE notification key [%s] category [%s] group [%s] isSummary [%s] title [%s] message [%s]",
                        notification.getKey(), notification.getNotification().category,
                        notification.getNotification().getGroup(),
                        StatusBarNotificationExtractor.isSummary(notification),
                        NotificationExtractor.getTitle(notification.getNotification()),
                        NotificationExtractor.getMessage(notification.getNotification())))
                .filter(notification -> StatusBarNotificationExtractor.isMessage(notification))
                .filter(notification -> StatusBarNotificationExtractor.isSummary(notification))
                .filter(notification -> StringUtils.equals(group, notification.getNotification().getGroup()))
                .map(notification -> notification.getKey())
                .findFirst();
    }

    private void printLineNotifications(final String groupThatShouldBeDismissed) {
        activeStatusBarNotificationSupplier.get().stream()
                .filter(notification -> notification.getPackageName().equals(LINE_PACKAGE_NAME))
                .forEach(notification -> {
                    Timber.w("%sPrint LINE notification that are not dismissed key [%s] category [%s] group [%s] isSummary [%s] isClearable [%s] title [%s] message [%s]",
                            StringUtils.equals(notification.getNotification().getGroup(), groupThatShouldBeDismissed) ? "[SHOULD_DISMISS] " : "",
                            notification.getKey(), notification.getNotification().category,
                            notification.getNotification().getGroup(),
                            StatusBarNotificationExtractor.isSummary(notification),
                            notification.isClearable(),
                            NotificationExtractor.getTitle(notification.getNotification()),
                            NotificationExtractor.getMessage(notification.getNotification()));
                });
    }

}
