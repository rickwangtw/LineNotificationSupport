package com.mysticwind.linenotificationsupport.module;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.bluetooth.BluetoothController;
import com.mysticwind.linenotificationsupport.bluetooth.impl.AndroidBluetoothController;
import com.mysticwind.linenotificationsupport.debug.DebugModeProvider;
import com.mysticwind.linenotificationsupport.notification.reactor.CallInProgressTrackingReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.ChatRoomNamePersistenceIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.ConversationStarterNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.DismissedNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.DumbNotificationCounterNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.IncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.LineNotificationLoggingIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.LineReplyActionPersistenceIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.LoggingDismissedNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.ManageLineNotificationIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.PublishedNotificationTrackerIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.SameLineMessageIdFilterIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.SummaryNotificationPublisherNotificationReactor;

import java.util.List;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class ReactorModule {

    /* Related classes using @Inject
        LineNotificationLoggingIncomingNotificationReactor
        CallInProgressTrackingReactor
        ChatRoomNamePersistenceIncomingNotificationReactor
        LineReplyActionPersistenceIncomingNotificationReactor
        DumbNotificationCounterNotificationReactor
        SummaryNotificationPublisherNotificationReactor
        SummaryNotificationPublisher
        ManageLineNotificationIncomingNotificationReactor
        SameLineMessageIdFilterIncomingNotificationReactor
        ConversationStarterNotificationReactor
        LoggingDismissedNotificationReactor
        PublishedNotificationTrackerIncomingNotificationReactor
     */

    @Singleton
    @Provides
    public static List<IncomingNotificationReactor> incomingNotificationReactors(DebugModeProvider debugModeProvider,
                                                                                 LineNotificationLoggingIncomingNotificationReactor lineNotificationLoggingIncomingNotificationReactor,
                                                                                 CallInProgressTrackingReactor callInProgressTrackingReactor,
                                                                                 ChatRoomNamePersistenceIncomingNotificationReactor chatRoomNamePersistenceIncomingNotificationReactor,
                                                                                 LineReplyActionPersistenceIncomingNotificationReactor  lineReplyActionPersistenceIncomingNotificationReactor,
                                                                                 DumbNotificationCounterNotificationReactor dumbNotificationCounterNotificationReactor,
                                                                                 SummaryNotificationPublisherNotificationReactor  summaryNotificationPublisherNotificationReactor,
                                                                                 ManageLineNotificationIncomingNotificationReactor manageLineNotificationIncomingNotificationReactor,
                                                                                 SameLineMessageIdFilterIncomingNotificationReactor sameLineMessageIdFilterIncomingNotificationReactor,
                                                                                 ConversationStarterNotificationReactor conversationStarterNotificationReactor,
                                                                                 PublishedNotificationTrackerIncomingNotificationReactor publishedNotificationTrackerIncomingNotificationReactor) {
        final ImmutableList.Builder<IncomingNotificationReactor> reactorListBuilder = ImmutableList.builder();
        if (debugModeProvider.isDebugMode()) {
            reactorListBuilder.add(lineNotificationLoggingIncomingNotificationReactor);
        }
        reactorListBuilder.add(callInProgressTrackingReactor);
        reactorListBuilder.add(chatRoomNamePersistenceIncomingNotificationReactor);
        reactorListBuilder.add(lineReplyActionPersistenceIncomingNotificationReactor);
        reactorListBuilder.add(dumbNotificationCounterNotificationReactor);
        reactorListBuilder.add(summaryNotificationPublisherNotificationReactor);
        reactorListBuilder.add(manageLineNotificationIncomingNotificationReactor);
        reactorListBuilder.add(sameLineMessageIdFilterIncomingNotificationReactor);
        reactorListBuilder.add(conversationStarterNotificationReactor);
        reactorListBuilder.add(publishedNotificationTrackerIncomingNotificationReactor);
        return reactorListBuilder.build();
    }

    @Singleton
    @Binds
    public abstract BluetoothController bindBluetoothController(AndroidBluetoothController androidBluetoothController);

    @Singleton
    @Provides
    public static List<DismissedNotificationReactor> dismissedNotificationReactors(DebugModeProvider debugModeProvider,
                                                                                   LoggingDismissedNotificationReactor loggingDismissedNotificationReactor,
                                                                                   CallInProgressTrackingReactor callInProgressTrackingReactor,
                                                                                   DumbNotificationCounterNotificationReactor dumbNotificationCounterNotificationReactor,
                                                                                   SummaryNotificationPublisherNotificationReactor  summaryNotificationPublisherNotificationReactor,
                                                                                   ConversationStarterNotificationReactor conversationStarterNotificationReactor) {
        final ImmutableList.Builder<DismissedNotificationReactor> reactorListBuilder = ImmutableList.builder();
        if (debugModeProvider.isDebugMode()) {
            reactorListBuilder.add(loggingDismissedNotificationReactor);
        }
        reactorListBuilder.add(callInProgressTrackingReactor);
        reactorListBuilder.add(dumbNotificationCounterNotificationReactor);
        reactorListBuilder.add(summaryNotificationPublisherNotificationReactor);
        reactorListBuilder.add(conversationStarterNotificationReactor);
        return reactorListBuilder.build();
    }

}
