package com.mysticwind.linenotificationsupport.notification.reactor;

import android.os.Handler;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.conversationstarter.ConversationStarterNotificationManager;
import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.notification.MaxNotificationHandlingNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class ConversationStarterNotificationReactor implements IncomingNotificationReactor, DismissedNotificationReactor {

    private final Set<String> interestedPackages;
    private final String thisPackageName;
    private final Handler handler;

    private final ConversationStarterNotificationManager conversationStarterNotificationManager;
    private final PreferenceProvider preferenceProvider;

    private Set<String> knownAvailableChatIds = new HashSet<>();

    @Inject
    public ConversationStarterNotificationReactor(@HiltQualifiers.PackageName final String thisPackageName,
                                                  final ConversationStarterNotificationManager conversationStarterNotificationManager,
                                                  final PreferenceProvider preferenceProvider,
                                                  final Handler handler) {
        this.thisPackageName = Validate.notBlank(thisPackageName);
        this.conversationStarterNotificationManager = Objects.requireNonNull(conversationStarterNotificationManager);
        this.preferenceProvider = Objects.requireNonNull(preferenceProvider);
        // LINE for incoming and self for dismissing
        this.interestedPackages = ImmutableSet.of(Constants.LINE_PACKAGE_NAME, thisPackageName);
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public Collection<String> interestedPackages() {
        return interestedPackages;
    }

    @Override
    public boolean isInterestInNotificationGroup() {
        return false;
    }

    @Override
    public Reaction reactToIncomingNotification(StatusBarNotification statusBarNotification) {
        Objects.requireNonNull(statusBarNotification);

        if (!preferenceProvider.shouldShowConversationStarterNotification()) {
            return Reaction.NONE;
        }

        if (!Constants.LINE_PACKAGE_NAME.equals(statusBarNotification.getPackageName())) {
            return Reaction.NONE;
        }

        final String chatId = NotificationExtractor.getLineChatId(statusBarNotification.getNotification());
        if (StringUtils.isBlank(chatId)) {
            return Reaction.NONE;
        }
        if (knownAvailableChatIds.contains(chatId)) {
            return Reaction.NONE;
        }
        publishNotification();
        return Reaction.NONE;
    }

    private void publishNotification() {
        final Set<String> chatIds = conversationStarterNotificationManager.publishNotification();
        knownAvailableChatIds.addAll(chatIds);
    }

    @Override
    public Reaction reactToDismissedNotification(StatusBarNotification statusBarNotification) {
        Objects.requireNonNull(statusBarNotification);

        if (!preferenceProvider.shouldShowConversationStarterNotification()) {
            return Reaction.NONE;
        }

        if (!thisPackageName.equals(statusBarNotification.getPackageName())) {
            return Reaction.NONE;
        }
        Optional<String> chatId = NotificationExtractor.getLineNotificationSupportChatId(statusBarNotification.getNotification());
        if (!chatId.isPresent()) {
            return Reaction.NONE;
        }
        if (!ConversationStarterNotificationManager.CONVERSATION_STARTER_CHAT_ID.equals(chatId.get())) {
            return Reaction.NONE;
        }
        Timber.d("Detected dismiss of conversation starter notification");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Timber.d("Republish conversation starter notification");
                publishNotification();
            }
        }, MaxNotificationHandlingNotificationPublisherDecorator.DISMISS_COOL_DOWN_IN_MILLIS);
        return Reaction.NONE;
    }

}
