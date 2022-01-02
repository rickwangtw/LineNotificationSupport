package com.mysticwind.linenotificationsupport.module;

import com.mysticwind.linenotificationsupport.bluetooth.BluetoothController;
import com.mysticwind.linenotificationsupport.bluetooth.impl.AndroidBluetoothController;
import com.mysticwind.linenotificationsupport.debug.DebugModeProvider;
import com.mysticwind.linenotificationsupport.notification.reactor.CallInProgressTrackingReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.ChatRoomNamePersistenceIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.ConversationStarterNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.DumbNotificationCounterNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.IncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.LineNotificationLoggingIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.LineReplyActionPersistenceIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.ManageLineNotificationIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.SameLineMessageIdFilterIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.SummaryNotificationPublisherNotificationReactor;

import java.util.ArrayList;
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
                                                                                 ConversationStarterNotificationReactor conversationStarterNotificationReactor
                                                                                 ) {
        final List<IncomingNotificationReactor> incomingNotificationReactors = new ArrayList<>();
        if (debugModeProvider.isDebugMode()) {
            incomingNotificationReactors.add(lineNotificationLoggingIncomingNotificationReactor);
        }
        incomingNotificationReactors.add(callInProgressTrackingReactor);
        incomingNotificationReactors.add(chatRoomNamePersistenceIncomingNotificationReactor);
        incomingNotificationReactors.add(lineReplyActionPersistenceIncomingNotificationReactor);
        incomingNotificationReactors.add(dumbNotificationCounterNotificationReactor);
        incomingNotificationReactors.add(summaryNotificationPublisherNotificationReactor);
        incomingNotificationReactors.add(manageLineNotificationIncomingNotificationReactor);
        incomingNotificationReactors.add(sameLineMessageIdFilterIncomingNotificationReactor);
        incomingNotificationReactors.add(conversationStarterNotificationReactor);
        return incomingNotificationReactors;
    }

    @Singleton
    @Binds
    public abstract BluetoothController bindBluetoothController(AndroidBluetoothController androidBluetoothController);


}
