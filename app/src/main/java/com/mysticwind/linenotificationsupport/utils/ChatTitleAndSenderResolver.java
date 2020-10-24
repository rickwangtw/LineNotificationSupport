package com.mysticwind.linenotificationsupport.utils;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.HashMultimap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class ChatTitleAndSenderResolver {

    private final HashMultimap<String, String> chatIdToSenderMultimap = HashMultimap.create();

    public Pair<String, String> resolveTitleAndSender(final StatusBarNotification statusBarNotification) {
        // individual: android.title is the sender
        // group chat: android.title is "group title：sender", android.conversationTitle is group title
        // chat with multi-folks: android.title is also the sender, no way to differentiate between individual and multi-folks :(

        // it is straightforward for chat groups
        if (isChatGroup(statusBarNotification)) {
            final String title = getGroupChatTitle(statusBarNotification);
            final String androidTitle = getAndroidTitle(statusBarNotification);
            final String sender = androidTitle.replace(title + "：", "");
            return Pair.of(title, sender);
        }
        // for others, it can be an individual or multiple folks without a group name
        final String sender = getAndroidTitle(statusBarNotification);

        // just use the sender if not chat (e.g. calls)
        String chatId = getChatId(statusBarNotification);
        if (StringUtils.isBlank(chatId)) {
            return Pair.of(sender, sender);
        }

        chatIdToSenderMultimap.put(chatId, sender);
        return Pair.of(sortAndMerge(chatIdToSenderMultimap.get(chatId)), sender);
    }

    private boolean isChatGroup(final StatusBarNotification statusBarNotification) {
        final String title = getGroupChatTitle(statusBarNotification);
        return StringUtils.isNotBlank(title);
    }

    private String getGroupChatTitle(final StatusBarNotification statusBarNotification) {
        // chat groups will have a conversationTitle (but not groups of people)
        return statusBarNotification.getNotification().extras.getString("android.conversationTitle");
    }

    private String getAndroidTitle(final StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().extras.getString("android.title");
    }

    private String getChatId(final StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().extras.getString("line.chat.id");
    }

    private String sortAndMerge(Set<String> senders) {
        return senders.stream()
                .sorted()
                .reduce((sender1, sender2) -> sender1 + "," + sender2)
                .get(); // there should always be one senders
    }

}
