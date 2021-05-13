package com.mysticwind.linenotificationsupport.chatname.dataaccessor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CachingMultiPersonChatNameDataAccessorDecoratorTest {

    private static final String CHAT_ID = "chatId";
    private static final String SENDER = "sender";
    private static final String ANOTHER_SENDER = "anotherSender";
    private static final String EXPECTED_MULTI_SENDER = "anotherSender,sender";

    @Mock
    private MultiPersonChatNameDataAccessor mockedMultiPersonChatNameDataAccessor;

    private CachingMultiPersonChatNameDataAccessorDecorator classUnderTest;

    @Before
    public void setUp() throws Exception {
        when(mockedMultiPersonChatNameDataAccessor.getAllChatIdToSenders()).thenReturn(HashMultimap.create());
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void addRelationshipAndGetChatGroupName_noPreviouslyPersistedSenders() {
        classUnderTest = new CachingMultiPersonChatNameDataAccessorDecorator(mockedMultiPersonChatNameDataAccessor);

        String groupName = classUnderTest.addRelationshipAndGetChatGroupName(CHAT_ID, SENDER);

        assertEquals(SENDER, groupName);
        verify(mockedMultiPersonChatNameDataAccessor).getAllChatIdToSenders();
        verify(mockedMultiPersonChatNameDataAccessor).addRelationshipAndGetChatGroupName(CHAT_ID, SENDER);
        verifyNoMoreInteractions(mockedMultiPersonChatNameDataAccessor);
    }

    @Test
    public void addRelationshipAndGetChatGroupName_hasPreviouslyPersistedSameSender() {
        Multimap<String, String> chatIdToSenderMultimap = HashMultimap.create();
        chatIdToSenderMultimap.put(CHAT_ID, SENDER);
        when(mockedMultiPersonChatNameDataAccessor.getAllChatIdToSenders()).thenReturn(chatIdToSenderMultimap);
        classUnderTest = new CachingMultiPersonChatNameDataAccessorDecorator(mockedMultiPersonChatNameDataAccessor);

        String groupName = classUnderTest.addRelationshipAndGetChatGroupName(CHAT_ID, SENDER);

        assertEquals(SENDER, groupName);
        verify(mockedMultiPersonChatNameDataAccessor).getAllChatIdToSenders();
        verifyNoMoreInteractions(mockedMultiPersonChatNameDataAccessor);
    }

    @Test
    public void addRelationshipAndGetChatGroupName_hasPreviouslyPersistedDifferentSender() {
        Multimap<String, String> chatIdToSenderMultimap = HashMultimap.create();
        chatIdToSenderMultimap.put(CHAT_ID, ANOTHER_SENDER);
        when(mockedMultiPersonChatNameDataAccessor.getAllChatIdToSenders()).thenReturn(chatIdToSenderMultimap);
        classUnderTest = new CachingMultiPersonChatNameDataAccessorDecorator(mockedMultiPersonChatNameDataAccessor);

        String groupName = classUnderTest.addRelationshipAndGetChatGroupName(CHAT_ID, SENDER);

        assertEquals(EXPECTED_MULTI_SENDER, groupName);
        verify(mockedMultiPersonChatNameDataAccessor).getAllChatIdToSenders();
        verify(mockedMultiPersonChatNameDataAccessor).addRelationshipAndGetChatGroupName(CHAT_ID, SENDER);
        verifyNoMoreInteractions(mockedMultiPersonChatNameDataAccessor);
    }

    @Test
    public void getAllChatIdToSenders() {
        Multimap<String, String> chatIdToSenderMultimap = HashMultimap.create();
        chatIdToSenderMultimap.put(CHAT_ID, SENDER);
        when(mockedMultiPersonChatNameDataAccessor.getAllChatIdToSenders()).thenReturn(chatIdToSenderMultimap);
        classUnderTest = new CachingMultiPersonChatNameDataAccessorDecorator(mockedMultiPersonChatNameDataAccessor);

        Multimap<String, String> chatIdToSenders = classUnderTest.getAllChatIdToSenders();

        assertEquals(ImmutableMap.of(CHAT_ID, ImmutableSet.of(SENDER)), chatIdToSenderMultimap.asMap());
        verify(mockedMultiPersonChatNameDataAccessor).getAllChatIdToSenders();
        verifyNoMoreInteractions(mockedMultiPersonChatNameDataAccessor);
    }
}