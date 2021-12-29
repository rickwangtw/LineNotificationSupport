package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.conversationstarter.ConversationStarterNotificationManager;
import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ConversationStarterNotificationReactor implements IncomingNotificationReactor {

    private static final Set<String> INTERESTED_PACKAGES = ImmutableSet.of(Constants.LINE_PACKAGE_NAME);

    private final ConversationStarterNotificationManager conversationStarterNotificationManager;

    private Set<String> knownAvailableChatIds = new HashSet<>();

    public ConversationStarterNotificationReactor(final ConversationStarterNotificationManager conversationStarterNotificationManager) {
        this.conversationStarterNotificationManager = Objects.requireNonNull(conversationStarterNotificationManager);
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
        final String chatId = NotificationExtractor.getLineChatId(statusBarNotification.getNotification());
        if (StringUtils.isBlank(chatId)) {
            return Reaction.NONE;
        }
        if (knownAvailableChatIds.contains(chatId)) {
            return Reaction.NONE;
        }
        final Set<String> chatIds = conversationStarterNotificationManager.publishNotification();
        knownAvailableChatIds.addAll(chatIds);
        return Reaction.NONE;
    }

}
