package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationPrinter;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

// TODO are we supposed to do a merge or a re-read instead of ignoring?
@Singleton
public class SameLineMessageIdFilterIncomingNotificationReactor implements IncomingNotificationReactor {

    private static final Set<String> INTERESTED_PACKAGES = ImmutableSet.of(Constants.LINE_PACKAGE_NAME);

    private final StatusBarNotificationPrinter statusBarNotificationPrinter = new StatusBarNotificationPrinter();

    private final Cache<String, StatusBarNotification> lineMessageIdToNotificationCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .removalListener(new RemovalListener<String, StatusBarNotification>() {
                @Override
                public void onRemoval(RemovalNotification<String, StatusBarNotification> notification) {
                    Timber.d("LINE message ID cache removal detected: reason [%s] LINE message ID [%s], message [%s]",
                            notification.getCause(), notification.getKey(),
                            NotificationExtractor.getMessage(notification.getValue().getNotification()));
                }
            })
            .build();

    @Inject
    public SameLineMessageIdFilterIncomingNotificationReactor() {
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
    public Reaction reactToIncomingNotification(final StatusBarNotification incomingStatusBarNotification) {
        final String lineMessageId = NotificationExtractor.getLineMessageId(incomingStatusBarNotification.getNotification());
        if (StringUtils.isBlank(lineMessageId)) {
            return Reaction.NONE;
        }
        final StatusBarNotification cachedStatusBarNotification = lineMessageIdToNotificationCache.getIfPresent(lineMessageId);
        if (cachedStatusBarNotification == null) {
            Timber.d("Tracking LINE message ID [%s]", lineMessageId);
            lineMessageIdToNotificationCache.put(lineMessageId, incomingStatusBarNotification);
            return Reaction.NONE;
        }
        final String originalMessage = NotificationExtractor.getMessage(cachedStatusBarNotification.getNotification());
        final String newMessage = NotificationExtractor.getMessage(incomingStatusBarNotification.getNotification());

        if (!StringUtils.equals(originalMessage, newMessage)) {
            Timber.d("Detected duplicated notifications: LINE message ID [%s] original [%s] -> new [%s]", lineMessageId,
                    originalMessage,
                    newMessage);
            statusBarNotificationPrinter.print("Received updated notification", incomingStatusBarNotification);
            return Reaction.NONE;
        }

        Timber.d("[STOP] Detected duplicated notifications: LINE message ID [%s] original [%s] -> new [%s]", lineMessageId,
                originalMessage,
                newMessage);
        return Reaction.STOP_FURTHER_PROCESSING;
    }

}
