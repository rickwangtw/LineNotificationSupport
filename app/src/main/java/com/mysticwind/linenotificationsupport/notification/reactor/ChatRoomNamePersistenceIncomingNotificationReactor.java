package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.GroupChatNameDataAccessor;
import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class ChatRoomNamePersistenceIncomingNotificationReactor implements IncomingNotificationReactor {

    private static final Set<String> INTERESTED_PACKAGES = ImmutableSet.of(Constants.LINE_PACKAGE_NAME);

    private final GroupChatNameDataAccessor groupChatNameDataAccessor;

    @Inject
    public ChatRoomNamePersistenceIncomingNotificationReactor(final GroupChatNameDataAccessor groupChatNameDataAccessor) {
        this.groupChatNameDataAccessor = Objects.requireNonNull(groupChatNameDataAccessor);
    }

    @Override
    public Collection<String> interestedPackages() {
        return INTERESTED_PACKAGES;
    }

    @Override
    public boolean isInterestInNotificationGroup() {
        return true;
    }

    @Override
    public Reaction reactToIncomingNotification(final StatusBarNotification statusBarNotification) {
        Objects.requireNonNull(statusBarNotification);

        final String chatId = NotificationExtractor.getLineChatId(statusBarNotification.getNotification());
        final String groupChatTitle = getGroupChatTitle(statusBarNotification);
        if (StringUtils.isNotBlank(chatId) && StringUtils.isNotBlank(groupChatTitle)) {
            Timber.d("Identified map of chat ID [%s] to chat name [%s] from a notification key [%s] isSummary [%s] package [%s]",
                    chatId,
                    groupChatTitle,
                    statusBarNotification.getKey(),
                    StatusBarNotificationExtractor.isSummary(statusBarNotification),
                    statusBarNotification.getPackageName());

            groupChatNameDataAccessor.persistRelationship(chatId, groupChatTitle);
        }
        return Reaction.NONE;
    }

    // TODO this is copied from ChatTitleAndSenderResolver
    private String getGroupChatTitle(final StatusBarNotification statusBarNotification) {
        // chat groups will have a conversationTitle (but not groups of people)
        return NotificationExtractor.getConversationTitle(statusBarNotification.getNotification());
    }

}
