package com.mysticwind.linenotificationsupport.chatname.dataaccessor;

import com.google.common.collect.Multimap;

public interface MultiPersonChatNameDataAccessor {

    String addRelationshipAndGetChatGroupName(final String chatId, final String sender);

    Multimap<String, String> getAllChatIdToSenders();

}
