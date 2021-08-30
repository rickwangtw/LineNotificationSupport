package com.mysticwind.linenotificationsupport.notificationgroup;

import static com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator.CALL_NOTIFICATION_GROUP_ID;
import static com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator.MESSAGE_NOTIFICATION_GROUP_ID;
import static com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator.OTHERS_NOTIFICATION_GROUP_ID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class NotificationGroupCreatorTest {

    private static final String SOME_RANDOM_CHAT_ID = "chatId";

    @Mock
    private NotificationManager mockedNotificationManager;

    @Mock
    private AndroidFeatureProvider mockedAndroidFeatureProvider;

    @Mock
    private PreferenceProvider mockedPreferenceProvider;

    @Mock
    private NotificationChannelGroup callNotificationChannelGroup;

    @Mock
    private NotificationChannelGroup messageNotificationChannelGroup;

    @Mock
    private NotificationChannelGroup otherNotificationChannelGroup;

    @Captor
    private ArgumentCaptor<NotificationChannelGroup> notificationChannelGroupCaptor;

    private NotificationGroupCreator classUnderTest;

    @Before
    public void setUp() {
        when(mockedAndroidFeatureProvider.hasNotificationChannelSupport()).thenReturn(true);
        when(callNotificationChannelGroup.getId()).thenReturn(CALL_NOTIFICATION_GROUP_ID);
        when(messageNotificationChannelGroup.getId()).thenReturn(MESSAGE_NOTIFICATION_GROUP_ID);
        when(otherNotificationChannelGroup.getId()).thenReturn(OTHERS_NOTIFICATION_GROUP_ID);

        classUnderTest = new NotificationGroupCreator(mockedNotificationManager, mockedAndroidFeatureProvider, mockedPreferenceProvider);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(mockedNotificationManager, mockedAndroidFeatureProvider, mockedPreferenceProvider);
    }

    @Test
    public void createNotificationGroupsNoGroupsCreated() {
        when(mockedNotificationManager.getNotificationChannelGroups()).thenReturn(Collections.EMPTY_LIST);
        NotificationChannel notificationChannel1 = buildNotificationChannelWithoutGroup(LineNotificationBuilder.DEFAULT_CHAT_ID);
        NotificationChannel notificationChannel2 = buildNotificationChannelWithoutGroup(LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID);
        NotificationChannel notificationChannel3 = buildNotificationChannelWithoutGroup(SOME_RANDOM_CHAT_ID);
        List<NotificationChannel> notificationChannels = ImmutableList.of(
                notificationChannel1, notificationChannel2, notificationChannel3
        );
        when(mockedNotificationManager.getNotificationChannels()).thenReturn(notificationChannels);

        classUnderTest.createNotificationGroups();

        verify(mockedAndroidFeatureProvider).hasNotificationChannelSupport();
        verify(mockedNotificationManager).getNotificationChannelGroups();
        verify(mockedNotificationManager, times(3)).createNotificationChannelGroup(any(NotificationChannelGroup.class));
        verify(mockedNotificationManager).getNotificationChannels();
        verify(notificationChannel1).setGroup(OTHERS_NOTIFICATION_GROUP_ID);
        verify(notificationChannel2).setGroup(CALL_NOTIFICATION_GROUP_ID);
        verify(notificationChannel3).setGroup(MESSAGE_NOTIFICATION_GROUP_ID);
        // TODO how to test what we are updating???
        verify(mockedNotificationManager, times(3)).createNotificationChannel(any(NotificationChannel.class));
    }

    @Test
    // not a possible case TBH
    public void createNotificationGroupsNoGroupsCreated_allNotificationChannelWithGroup() {
        when(mockedNotificationManager.getNotificationChannelGroups()).thenReturn(Collections.EMPTY_LIST);
        List<NotificationChannel> notificationChannels = ImmutableList.of(
                buildNotificationChannel(CALL_NOTIFICATION_GROUP_ID),
                buildNotificationChannel(MESSAGE_NOTIFICATION_GROUP_ID),
                buildNotificationChannel(OTHERS_NOTIFICATION_GROUP_ID)
        );
        when(mockedNotificationManager.getNotificationChannels()).thenReturn(notificationChannels);

        classUnderTest.createNotificationGroups();

        verify(mockedAndroidFeatureProvider).hasNotificationChannelSupport();
        verify(mockedNotificationManager).getNotificationChannelGroups();
        // TODO how to test what we are creating???
        verify(mockedNotificationManager, times(3)).createNotificationChannelGroup(any(NotificationChannelGroup.class));
        verify(mockedNotificationManager).getNotificationChannels();
    }

    @Test
    public void createNotificationGroupsAllGroupsCreated() {
        when(mockedNotificationManager.getNotificationChannelGroups()).thenReturn(
                ImmutableList.of(
                        callNotificationChannelGroup,
                        messageNotificationChannelGroup,
                        otherNotificationChannelGroup
                )
        );
        NotificationChannel notificationChannel1 = buildNotificationChannelWithoutGroup(LineNotificationBuilder.DEFAULT_CHAT_ID);
        NotificationChannel notificationChannel2 = buildNotificationChannelWithoutGroup(LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID);
        NotificationChannel notificationChannel3 = buildNotificationChannelWithoutGroup(SOME_RANDOM_CHAT_ID);
        List<NotificationChannel> notificationChannels = ImmutableList.of(
                notificationChannel1, notificationChannel2, notificationChannel3
        );
        when(mockedNotificationManager.getNotificationChannels()).thenReturn(notificationChannels);

        classUnderTest.createNotificationGroups();

        verify(mockedAndroidFeatureProvider).hasNotificationChannelSupport();
        verify(mockedNotificationManager).getNotificationChannelGroups();
        verify(mockedNotificationManager).getNotificationChannels();
        verify(notificationChannel1).setGroup(OTHERS_NOTIFICATION_GROUP_ID);
        verify(notificationChannel2).setGroup(CALL_NOTIFICATION_GROUP_ID);
        verify(notificationChannel3).setGroup(MESSAGE_NOTIFICATION_GROUP_ID);
        // TODO how to test what we are updating???
        verify(mockedNotificationManager, times(3)).createNotificationChannel(any(NotificationChannel.class));
    }

    @Test
    public void createNotificationGroupsAllGroupsCreated_allNotificationChannelWithGroup() {
        when(mockedNotificationManager.getNotificationChannelGroups()).thenReturn(
                ImmutableList.of(
                        callNotificationChannelGroup,
                        messageNotificationChannelGroup,
                        otherNotificationChannelGroup
                )
        );
        List<NotificationChannel> notificationChannels = ImmutableList.of(
                buildNotificationChannel(CALL_NOTIFICATION_GROUP_ID),
                buildNotificationChannel(MESSAGE_NOTIFICATION_GROUP_ID),
                buildNotificationChannel(OTHERS_NOTIFICATION_GROUP_ID)
        );
        when(mockedNotificationManager.getNotificationChannels()).thenReturn(notificationChannels);
                
        classUnderTest.createNotificationGroups();

        verify(mockedAndroidFeatureProvider).hasNotificationChannelSupport();
        verify(mockedNotificationManager).getNotificationChannelGroups();
        verify(mockedNotificationManager).getNotificationChannels();
    }

    @Test
    @Ignore("TODO figure out a way to test these Android classes where we are getting the not-mocked exceptions")
    public void createSelfResponseNotificationChannel() {
        classUnderTest.createSelfResponseNotificationChannel();

        verify(mockedAndroidFeatureProvider).hasNotificationChannelSupport();
        verify(mockedNotificationManager).createNotificationChannelGroup(notificationChannelGroupCaptor.capture());
        NotificationChannelGroup notificationChannelGroup = notificationChannelGroupCaptor.getValue();
    }

    private NotificationChannel buildNotificationChannelWithoutGroup(String chatId) {
        NotificationChannel notificationChannel = mock(NotificationChannel.class);
        when(notificationChannel.getId()).thenReturn(chatId);
        return notificationChannel;
    }

    private NotificationChannel buildNotificationChannel(String notificationGroupId) {
        NotificationChannel notificationChannel = mock(NotificationChannel.class);
        when(notificationChannel.getGroup()).thenReturn(notificationGroupId);
        return notificationChannel;
    }

    @Test
    public void createNotificationGroupsWithoutNotificationChannelSupport() {
        when(mockedAndroidFeatureProvider.hasNotificationChannelSupport()).thenReturn(false);

        classUnderTest.createNotificationGroups();

        verify(mockedAndroidFeatureProvider).hasNotificationChannelSupport();
    }

}