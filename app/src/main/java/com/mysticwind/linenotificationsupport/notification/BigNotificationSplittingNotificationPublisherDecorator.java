package com.mysticwind.linenotificationsupport.notification;

import android.service.notification.StatusBarNotification;

import com.google.common.base.CharMatcher;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BigNotificationSplittingNotificationPublisherDecorator implements NotificationPublisher {

    private static final String MESSAGE_SEPARATOR = "(...)";
    private static final int MESSAGE_SEPARATOR_LENGTH = MESSAGE_SEPARATOR.length();

    private final NotificationPublisher notificationPublisher;
    private final NotificationIdGenerator notificationIdGenerator;
    private final PreferenceProvider preferenceProvider;

    public BigNotificationSplittingNotificationPublisherDecorator(final NotificationPublisher notificationPublisher,
                                                                  final NotificationIdGenerator notificationIdGenerator,
                                                                  final PreferenceProvider preferenceProvider) {
        this.notificationPublisher = notificationPublisher;
        this.notificationIdGenerator = notificationIdGenerator;
        this.preferenceProvider = preferenceProvider;
    }

    @Override
    public void publishNotification(LineNotification lineNotification, int notificationId) {
        String message = lineNotification.getMessage();
        final int messageSizeLimit = preferenceProvider.getMessageSizeLimit();
        if (message.length() <= messageSizeLimit) {
            // don't do anything else
            notificationPublisher.publishNotification(lineNotification, notificationId);
            return;
        }
        if (hasUrl(message)) {
            // don't do anything else
            notificationPublisher.publishNotification(lineNotification, notificationId);
            return;
        }
        final List<String> splitMessages = splitMessage(message, messageSizeLimit);
        for (int messageIndex = 0 ; messageIndex < splitMessages.size() ; ++messageIndex) {
            final String partialMessage = splitMessages.get(messageIndex);
            final LineNotification partialNotification =
                    lineNotification.toBuilder()
                            .message(partialMessage)
                            .timestamp(lineNotification.getTimestamp() + messageIndex)
                            .build();
            notificationPublisher.publishNotification(partialNotification, notificationIdGenerator.getNextNotificationId());
        }
    }

    // https://stackoverflow.com/questions/285619/how-to-detect-the-presence-of-url-in-a-string
    private boolean hasUrl(final String message) {
        // separate input by spaces ( URLs don't have spaces )
        final String[] parts = message.split("\\s+");

        // Attempt to convert each item into an URL.
        for (String item : parts) {
            try {
                new URL(item);
                return true;
            } catch (MalformedURLException e) {
                // not an URL
            }
        }
        return false;
    }

    private List<String> splitMessage(String originalMessage, int messageSizeLimit) {
        final int maxPageCount = preferenceProvider.getMaxPageCount();
        final List<String> splitMessages = new ArrayList<>();
        // For example, messageSizeLimit = 60
        // Page size: first, N, N+1
        // 60
        // 55, 55
        // 55, 50, 55
        // 55, 50, 50, 55
        String remainingMessage = removeLeadingSpaces(originalMessage);
        int pageCount = 0;
        while (pageCount++ < maxPageCount) {
            remainingMessage = removeLeadingSpaces(remainingMessage);
            if (remainingMessage.length() <= messageSizeLimit) {
                splitMessages.add(remainingMessage);
                break;
            }
            String firstHalfMessage = findNextPage(remainingMessage, messageSizeLimit);
            //String firstHalfMessage = remainingMessage.substring(0, messageSizeLimit - MESSAGE_SEPARATOR_LENGTH);
            splitMessages.add(firstHalfMessage + MESSAGE_SEPARATOR);
            remainingMessage = MESSAGE_SEPARATOR + removeLeadingSpaces(remainingMessage.substring(firstHalfMessage.length()));
        }
        return splitMessages;
    }

    private String removeLeadingSpaces(final String message) {
        for (int characterIndex = 0 ; characterIndex < message.length() ; ++characterIndex) {
            final char character = message.charAt(characterIndex);
            if (!CharMatcher.whitespace().matches(character)) {
                return message.substring(characterIndex);
            }
        }
        return "";
    }

    private String findNextPage(String message, int messageSizeLimit) {
        int lastWhitespaceIndex = 0;
        int characterIndex = 0;
        for (; characterIndex < message.length() && characterIndex < (messageSizeLimit - MESSAGE_SEPARATOR_LENGTH) ;
             ++characterIndex) {
            final char character = message.charAt(characterIndex);
            if (CharMatcher.whitespace().matches(character)) {
                lastWhitespaceIndex = characterIndex;
            }
        }
        if (lastWhitespaceIndex > 0) {
            return message.substring(0, lastWhitespaceIndex);
        }
        return message.substring(0, characterIndex);
    }

    private int findFirstNoneWhitespaceIndex(final String message) {
        for (int characterIndex = 0; characterIndex < message.length() ; ++characterIndex) {
            final char character = message.charAt(characterIndex);
            if (!CharMatcher.whitespace().matches(character)) {
                return characterIndex;
            }
        }
        return message.length();
    }

    @Override
    public void updateNotificationDismissed(StatusBarNotification statusBarNotification) {
        // do nothing
    }

}
