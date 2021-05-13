package com.mysticwind.linenotificationsupport.chatname.dataaccessor;

import java.util.Map;
import java.util.Optional;

public interface GroupChatNameDataAccessor {

    void persistRelationship(final String chatId, final String chatGroupName);

    Optional<String> getChatGroupName(final String chatId);

    Map<String, String> getAllChatGroups();

}
