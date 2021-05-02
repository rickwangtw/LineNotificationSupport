package com.mysticwind.linenotificationsupport.utils;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.HashMultimap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class ChatTitleAndSenderResolver {

    private final HashMultimap<String, String> chatIdToSenderMultimap = HashMultimap.create();
    // there are crazy weird situations where LINE don't provide chat room names. This acts as a workaround.
    private final Map<String, String> chatIdToChatRoomNameMap = new HashMap<>();

    public Pair<String, String> resolveTitleAndSender(final StatusBarNotification statusBarNotification) {
        // individual: android.title is the sender
        // group chat: android.title is "group title：sender", android.conversationTitle is group title
        // chat with multi-folks: android.title is also the sender, no way to differentiate between individual and multi-folks :(

        // it is straightforward for chat groups
        if (isChatGroup(statusBarNotification)) {
            final String title = getGroupChatTitle(statusBarNotification);
            cacheGroupTitle(statusBarNotification, title);
            final String sender = calculateGroupSender(statusBarNotification);
            return Pair.of(title, sender);
        }
        // for others, it can be an individual or multiple folks without a group name
        final String sender = getAndroidTitle(statusBarNotification);

        // just use the sender if not chat (e.g. calls)
        final String chatId = NotificationExtractor.getLineChatId(statusBarNotification.getNotification());
        if (StringUtils.isBlank(chatId)) {
            return Pair.of(sender, sender);
        }

        chatIdToSenderMultimap.put(chatId, sender);

        final String title;
        final String highConfidenceChatRoomName = chatIdToChatRoomNameMap.get(chatId);
        if (StringUtils.isNotBlank(highConfidenceChatRoomName)) {
            title = highConfidenceChatRoomName;
            Timber.w("Override with chat room name: " + title);
        } else {
            title = sortAndMerge(chatIdToSenderMultimap.get(chatId));
        }
        return Pair.of(title, sender);
    }

    private boolean isChatGroup(final StatusBarNotification statusBarNotification) {
        final String title = getGroupChatTitle(statusBarNotification);
        return StringUtils.isNotBlank(title);
    }

    public void addChatIdToChatNameMap(final String chatId, final String chatName) {
        if (StringUtils.isBlank(chatId) || StringUtils.isBlank(chatName)) {
            return;
        }
        chatIdToChatRoomNameMap.put(chatId, chatName);
    }

    private void cacheGroupTitle(final StatusBarNotification statusBarNotification, final String groupName) {
        final String chatId = NotificationExtractor.getLineChatId(statusBarNotification.getNotification());
        addChatIdToChatNameMap(chatId, groupName);
    }

    private String getGroupChatTitle(final StatusBarNotification statusBarNotification) {
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

    private String sortAndMerge(Set<String> senders) {
        return senders.stream()
                .sorted()
                .reduce((sender1, sender2) -> sender1 + "," + sender2)
                .get(); // there should always be at least one sender
    }

}
