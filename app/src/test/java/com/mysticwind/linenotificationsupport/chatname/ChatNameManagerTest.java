package com.mysticwind.linenotificationsupport.chatname;

import com.mysticwind.linenotificationsupport.chatname.dataaccessor.GroupChatNameDataAccessor;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.MultiPersonChatNameDataAccessor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ChatNameManagerTest {

    private static final String CHAT_ID = "chatId";
    private static final String SENDER = "sender";
    private static final String SENDER_2 = "sender2";
    private static final String MULTI_PERSON_CHAT_NAME = "multiPersonChatName";
    private static final String HIGH_CONFIDENCE_GROUP_NAME = "highConfidenceGroupName";
    private static final String DIFFERENT_CHAT_NAME = "differentChatName";

    @Mock
    private GroupChatNameDataAccessor mockedGroupChatNameDataAccessor;

    @Mock
    private MultiPersonChatNameDataAccessor mockedMultiPersonChatNameDataAccessor;

    private ChatNameManager classUnderTest;

    @Before
    public void setUp() throws Exception {
        when(mockedGroupChatNameDataAccessor.getChatGroupName(anyString())).thenReturn(Optional.empty());
        when(mockedMultiPersonChatNameDataAccessor.addRelationshipAndGetChatGroupName(anyString(), anyString())).thenReturn(MULTI_PERSON_CHAT_NAME);

        classUnderTest = new ChatNameManager(mockedGroupChatNameDataAccessor, mockedMultiPersonChatNameDataAccessor);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(
                mockedGroupChatNameDataAccessor,
                mockedMultiPersonChatNameDataAccessor
        );
    }

    @Test
    public void getChatName_singlePersonChat() {
        final String chatName = classUnderTest.getChatName(CHAT_ID, SENDER);

        assertEquals(MULTI_PERSON_CHAT_NAME, chatName);
        verify(mockedGroupChatNameDataAccessor).getChatGroupName(CHAT_ID);
        verify(mockedMultiPersonChatNameDataAccessor).addRelationshipAndGetChatGroupName(CHAT_ID, SENDER);
    }

    @Test
    public void getChatName_multiPersonChat() {
        String chatName = classUnderTest.getChatName(CHAT_ID, SENDER);

        assertEquals(MULTI_PERSON_CHAT_NAME, chatName);
        verify(mockedGroupChatNameDataAccessor).getChatGroupName(CHAT_ID);
        verify(mockedMultiPersonChatNameDataAccessor).addRelationshipAndGetChatGroupName(CHAT_ID, SENDER);

        chatName = classUnderTest.getChatName(CHAT_ID, SENDER_2);

        assertEquals(MULTI_PERSON_CHAT_NAME, chatName);
        verify(mockedGroupChatNameDataAccessor, times(2)/* includes the first call */).getChatGroupName(CHAT_ID);
        verify(mockedMultiPersonChatNameDataAccessor).addRelationshipAndGetChatGroupName(CHAT_ID, SENDER_2);
    }

    @Test
    public void getChatName_groupChatWithHighConfidenceGroupName_firstCall() {
        final String chatName = classUnderTest.getChatName(CHAT_ID, SENDER, HIGH_CONFIDENCE_GROUP_NAME);

        assertEquals(HIGH_CONFIDENCE_GROUP_NAME, chatName);
        verify(mockedGroupChatNameDataAccessor).getChatGroupName(CHAT_ID);
        verify(mockedGroupChatNameDataAccessor).persistRelationship(CHAT_ID, HIGH_CONFIDENCE_GROUP_NAME);
    }

    @Test
    public void getChatName_groupChatWithHighConfidenceGroupName_secondCall_sameChatName() {
        when(mockedGroupChatNameDataAccessor.getChatGroupName(anyString())).thenReturn(Optional.of(HIGH_CONFIDENCE_GROUP_NAME));

        final String chatName = classUnderTest.getChatName(CHAT_ID, SENDER, HIGH_CONFIDENCE_GROUP_NAME);

        assertEquals(HIGH_CONFIDENCE_GROUP_NAME, chatName);
        verify(mockedGroupChatNameDataAccessor).getChatGroupName(CHAT_ID);
    }

    @Test
    public void getChatName_groupChatWithHighConfidenceGroupName_secondCall_differentChatName() {
        when(mockedGroupChatNameDataAccessor.getChatGroupName(anyString())).thenReturn(Optional.of(DIFFERENT_CHAT_NAME));

        final String chatName = classUnderTest.getChatName(CHAT_ID, SENDER, HIGH_CONFIDENCE_GROUP_NAME);

        assertEquals(HIGH_CONFIDENCE_GROUP_NAME, chatName);
        verify(mockedGroupChatNameDataAccessor).getChatGroupName(CHAT_ID);
        verify(mockedGroupChatNameDataAccessor).persistRelationship(CHAT_ID, HIGH_CONFIDENCE_GROUP_NAME);
    }

    @Test
    public void getChatName_groupChatWithoutHighConfidenceGroupName() {
        when(mockedGroupChatNameDataAccessor.getChatGroupName(anyString())).thenReturn(Optional.of(HIGH_CONFIDENCE_GROUP_NAME));

        final String chatName = classUnderTest.getChatName(CHAT_ID, SENDER);

        assertEquals(HIGH_CONFIDENCE_GROUP_NAME, chatName);
        verify(mockedGroupChatNameDataAccessor).getChatGroupName(CHAT_ID);
    }

    @Test
    public void getChatName_groupChatWithoutHighConfidenceGroupName_noPersistedGroupChatName() {
        final String chatName = classUnderTest.getChatName(CHAT_ID, SENDER);

        assertEquals(MULTI_PERSON_CHAT_NAME, chatName);
        verify(mockedGroupChatNameDataAccessor).getChatGroupName(CHAT_ID);
        verify(mockedMultiPersonChatNameDataAccessor).addRelationshipAndGetChatGroupName(CHAT_ID, SENDER);
    }

}