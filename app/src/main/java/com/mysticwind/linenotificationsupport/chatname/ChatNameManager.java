package com.mysticwind.linenotificationsupport.chatname;

import com.mysticwind.linenotificationsupport.chatname.dataaccessor.GroupChatNameDataAccessor;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.MultiPersonChatNameDataAccessor;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

import timber.log.Timber;

public class ChatNameManager {

    private final GroupChatNameDataAccessor groupChatNameDataAccessor;
    private final MultiPersonChatNameDataAccessor multiPersonChatNameDataAccessor;

    public ChatNameManager(final GroupChatNameDataAccessor groupChatNameDataAccessor,
                           final MultiPersonChatNameDataAccessor multiPersonChatNameDataAccessor) {
        this.groupChatNameDataAccessor = Objects.requireNonNull(groupChatNameDataAccessor);
        this.multiPersonChatNameDataAccessor = Objects.requireNonNull(multiPersonChatNameDataAccessor);
    }

    public String getChatName(final String chatId, final String sender) {
        return getChatName(chatId, sender, null);
    }

    public String getChatName(final String chatId, final String sender, final String highConfidenceChatGroupName) {
        final Optional<String> chatGroupName = groupChatNameDataAccessor.getChatGroupName(chatId);
        if (chatGroupName.isPresent()) {
            if (StringUtils.isNotBlank(highConfidenceChatGroupName) &&
                    !chatGroupName.get().equals(highConfidenceChatGroupName)) {
                groupChatNameDataAccessor.persistRelationship(chatId, highConfidenceChatGroupName);
                return highConfidenceChatGroupName;
            }
            if (StringUtils.isBlank(highConfidenceChatGroupName)) {
                Timber.w("Override with chat room name: " + chatGroupName.get());
            }
            return chatGroupName.get();
        }
        if (StringUtils.isNotBlank(highConfidenceChatGroupName)) {
            groupChatNameDataAccessor.persistRelationship(chatId, highConfidenceChatGroupName);
            return highConfidenceChatGroupName;
        }
        final String chatName = multiPersonChatNameDataAccessor.addRelationshipAndGetChatGroupName(chatId, sender);
        return chatName;
    }

}
