package com.mysticwind.linenotificationsupport.utils;

import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.chatname.ChatNameManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class ChatTitleAndSenderResolver {

    private ChatNameManager chatNameManager;

    @Inject
    public ChatTitleAndSenderResolver(final ChatNameManager chatNameManager) {
        this.chatNameManager = Objects.requireNonNull(chatNameManager);
    }

    public Pair<String, String> resolveTitleAndSender(final StatusBarNotification statusBarNotification) {
        // individual: android.title is the sender
        // group chat: android.title is "group title：sender", android.conversationTitle is group title
        // chat with multi-folks: android.title is also the sender, no way to differentiate between individual and multi-folks :(

        // it is straightforward for chat groups
        String sender = getAndroidTitle(statusBarNotification);

        // just use the sender if not chat (e.g. calls)
        final String chatId = NotificationExtractor.getLineChatId(statusBarNotification.getNotification());
        if (StringUtils.isBlank(chatId)) {
            return Pair.of(sender, sender);
        }

        String highConfidenceChatGroupName = null;
        if (isChatGroup(statusBarNotification)) {
            highConfidenceChatGroupName = getGroupChatTitle(statusBarNotification);
            sender = calculateGroupSender(statusBarNotification);
        }

        final String chatName = chatNameManager.getChatName(chatId, sender, highConfidenceChatGroupName);
        return Pair.of(chatName, sender);
    }

    private boolean isChatGroup(final StatusBarNotification statusBarNotification) {
        final String title = getGroupChatTitle(statusBarNotification);
        return StringUtils.isNotBlank(title);
    }

    private String getGroupChatTitle(final StatusBarNotification statusBarNotification) {
        final String subText = NotificationExtractor.getSubText(statusBarNotification.getNotification());
        if (StringUtils.isNotBlank(subText)) {
            return subText;
        }
        // chat groups will have a conversationTitle (but not groups of people)
        return NotificationExtractor.getConversationTitle(statusBarNotification.getNotification());
    }

    private String calculateGroupSender(StatusBarNotification statusBarNotification) {
        final String groupName = getGroupChatTitle(statusBarNotification);
        // group title will be something like GROUP_NAME: SENDER
        final String androidTitle = getAndroidTitle(statusBarNotification);
        // tickerText will be something like SENDER: message
        final String tickerText = statusBarNotification.getNotification().tickerText.toString();

        // step 1: remove GROUP_NAME from androidTitle (remainder: ": SENDER")
        final String androidTitleWithoutGroupName = androidTitle.replace(groupName, "");
        // step 2: find the common substring from the results in step 1 and 2
        for (int index = 0 ; index < androidTitleWithoutGroupName.length() ; ++index) {
            char character = androidTitleWithoutGroupName.charAt(index);
            if (character == tickerText.charAt(0)) {
                // we might have found a match - may not be a match if the sender starts with colon (is it possible?)
                final String potentialMatch = androidTitleWithoutGroupName.substring(index);
                if (tickerText.startsWith(potentialMatch)) {
                    return potentialMatch;
                }
            }
        }
        // fallback if we can't find a common substring for whatever reason
        Timber.w(String.format("Cannot find common substring with group:(%s) title:(%s) ticker(%s)",
                groupName, androidTitle, tickerText));
        return tickerText;
    }

    private String getAndroidTitle(final StatusBarNotification statusBarNotification) {
        return NotificationExtractor.getTitle(statusBarNotification.getNotification());
    }

}
