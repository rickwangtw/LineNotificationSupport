package com.mysticwind.linenotificationsupport.chatname.dataaccessor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class CachingMultiPersonChatNameDataAccessorDecorator implements MultiPersonChatNameDataAccessor {

    private final MultiPersonChatNameDataAccessor multiPersonChatNameDataAccessor;
    private final Multimap<String, String> chatIdToSenderMultimap;

    public CachingMultiPersonChatNameDataAccessorDecorator(final MultiPersonChatNameDataAccessor multiPersonChatNameDataAccessor) {
        this.multiPersonChatNameDataAccessor = Objects.requireNonNull(multiPersonChatNameDataAccessor);
        this.chatIdToSenderMultimap = HashMultimap.create(multiPersonChatNameDataAccessor.getAllChatIdToSenders());
    }

    @Override
    public String addRelationshipAndGetChatGroupName(String chatId, String sender) {
        final Collection<String> senders = this.chatIdToSenderMultimap.get(chatId);
        if (senders.contains(sender)) {
            return sortAndMerge(senders);
        }
        this.chatIdToSenderMultimap.put(chatId, sender);
        // TODO make this async
        multiPersonChatNameDataAccessor.addRelationshipAndGetChatGroupName(chatId, sender);
        return sortAndMerge(this.chatIdToSenderMultimap.get(chatId));
    }

    private String sortAndMerge(Collection<String> senders) {
        return new HashSet<>(senders).stream()
                .sorted()
                .reduce((sender1, sender2) -> sender1 + "," + sender2)
                .get(); // there should always be at least one sender
    }

    @Override
    public Multimap<String, String> getAllChatIdToSenders() {
        return HashMultimap.create(chatIdToSenderMultimap);
    }

}
