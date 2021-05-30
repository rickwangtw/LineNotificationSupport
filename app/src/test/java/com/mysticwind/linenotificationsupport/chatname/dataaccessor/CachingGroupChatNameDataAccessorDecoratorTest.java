package com.mysticwind.linenotificationsupport.chatname.dataaccessor;

import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CachingGroupChatNameDataAccessorDecoratorTest {

    private static final String CHAT_ID = "chatId";
    private static final String GROUP_NAME = "groupName";

    @Mock
    private GroupChatNameDataAccessor mockedGroupChatNameDataAccessor;

    private CachingGroupChatNameDataAccessorDecorator classUnderTest;

    @Before
    public void setUp() throws Exception {
        when(mockedGroupChatNameDataAccessor.getAllChatGroups()).thenReturn(Collections.emptyMap());
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void persistRelationship() {
        classUnderTest = new CachingGroupChatNameDataAccessorDecorator(mockedGroupChatNameDataAccessor);

        classUnderTest.persistRelationship(CHAT_ID, GROUP_NAME);

        verify(mockedGroupChatNameDataAccessor).getAllChatGroups();
        verify(mockedGroupChatNameDataAccessor).persistRelationship(CHAT_ID, GROUP_NAME);
    }

    @Test
    public void getChatGroupName_noPreviousPersistedChatGroup() {
        classUnderTest = new CachingGroupChatNameDataAccessorDecorator(mockedGroupChatNameDataAccessor);

        Optional<String> groupName = classUnderTest.getChatGroupName(CHAT_ID);

        assertFalse(groupName.isPresent());
        verify(mockedGroupChatNameDataAccessor).getAllChatGroups();
        verifyNoMoreInteractions(mockedGroupChatNameDataAccessor);
    }

    @Test
    public void getChatGroupName_hasPreviousPersistedChatGroup() {
        when(mockedGroupChatNameDataAccessor.getAllChatGroups()).thenReturn(ImmutableMap.of(CHAT_ID, GROUP_NAME));
        classUnderTest = new CachingGroupChatNameDataAccessorDecorator(mockedGroupChatNameDataAccessor);

        Optional<String> groupName = classUnderTest.getChatGroupName(CHAT_ID);

        assertTrue(groupName.isPresent());
        assertEquals(GROUP_NAME, groupName.get());
        verify(mockedGroupChatNameDataAccessor).getAllChatGroups();
        verifyNoMoreInteractions(mockedGroupChatNameDataAccessor);
    }

    @Test
    public void getAllChatGroups_noPreviousPersistedChatGroup() {
        classUnderTest = new CachingGroupChatNameDataAccessorDecorator(mockedGroupChatNameDataAccessor);

        Map<String, String> chatGroups = classUnderTest.getAllChatGroups();

        assertTrue(chatGroups.isEmpty());
        verify(mockedGroupChatNameDataAccessor).getAllChatGroups();
        verifyNoMoreInteractions(mockedGroupChatNameDataAccessor);
    }

    @Test
    public void getAllChatGroups() {
        when(mockedGroupChatNameDataAccessor.getAllChatGroups()).thenReturn(ImmutableMap.of(CHAT_ID, GROUP_NAME));
        classUnderTest = new CachingGroupChatNameDataAccessorDecorator(mockedGroupChatNameDataAccessor);

        Map<String, String> chatGroups = classUnderTest.getAllChatGroups();

        assertEquals(ImmutableMap.of(CHAT_ID, GROUP_NAME), chatGroups);
        verify(mockedGroupChatNameDataAccessor).getAllChatGroups();
        verifyNoMoreInteractions(mockedGroupChatNameDataAccessor);
    }

}