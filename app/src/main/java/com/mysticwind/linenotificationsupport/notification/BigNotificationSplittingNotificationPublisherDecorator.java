package com.mysticwind.linenotificationsupport.notification;

import android.service.notification.StatusBarNotification;

import com.google.common.base.CharMatcher;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BigNotificationSplittingNotificationPublisherDecorator implements NotificationPublisher {

    private static final String MESSAGE_SEPARATOR = "(...)";
    private static final int MESSAGE_SEPARATOR_LENGTH = MESSAGE_SEPARATOR.length();

    private final NotificationPublisher notificationPublisher;
    private final PreferenceProvider preferenceProvider;

    public BigNotificationSplittingNotificationPublisherDecorator(final NotificationPublisher notificationPublisher,
                                                                  final PreferenceProvider preferenceProvider) {
        this.notificationPublisher = notificationPublisher;
        this.preferenceProvider = preferenceProvider;
    }

    @Override
    public void publishNotification(LineNotification lineNotification, int notificationId) {
        if (StringUtils.isBlank(lineNotification.getMessage())) {
            notificationPublisher.publishNotification(lineNotification, notificationId);
            return;
        }
        String message = lineNotification.getMessage();
        final int messageSizeLimit = preferenceProvider.getMessageSizeLimit();
        if (message.length() <= messageSizeLimit) {
            // don't do anything else
            notificationPublisher.publishNotification(lineNotification, notificationId);
            return;
        }
        final List<String> splitMessages = splitMessage(message, messageSizeLimit);
        final LineNotification notificationWithSplitMessages =
                lineNotification.toBuilder()
                        .messages(splitMessages)
                        .build();
        notificationPublisher.publishNotification(notificationWithSplitMessages, notificationId);
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
            if (firstHalfMessage.length() >= remainingMessage.length()) {
                splitMessages.add(firstHalfMessage);
                break;
            } else {
                splitMessages.add(firstHalfMessage + MESSAGE_SEPARATOR);
                remainingMessage = MESSAGE_SEPARATOR + removeLeadingSpaces(remainingMessage.substring(firstHalfMessage.length()));
            }
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
        final Pair<Integer, String> urlIndexAndUrl = findIndexAndUrl(message);
        if (urlIndexAndUrl.getLeft() >= 0 && urlIndexAndUrl.getLeft() <= messageSizeLimit) {
            // there is url within this page, handle this specially
            final int endIndex = urlIndexAndUrl.getLeft() + urlIndexAndUrl.getRight().length();
            if (message.length() == endIndex) {
                // end of message
                return message.substring(0, endIndex);
            } else {
                // URL will need to end with a space in order to load correctly
                return message.substring(0, endIndex) + " ";
            }
        }

        // no url within this page
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

    private Pair<Integer, String> findIndexAndUrl(final String message) {
        // separate input by spaces ( URLs don't have spaces )
        final String[] parts = message.split("\\s+");

        // Attempt to convert each item into an URL.
        for (String item : parts) {
            if (isUrl(item)) {
                return Pair.of(message.indexOf(item), item);
            }
        }
        return Pair.of(-1, "");
    }

    // https://stackoverflow.com/questions/285619/how-to-detect-the-presence-of-url-in-a-string
    private boolean isUrl(final String string) {
        try {
            new URL(string);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public void republishNotification(LineNotification lineNotification, int notificationId) {
        // do nothing
        notificationPublisher.republishNotification(lineNotification, notificationId);
    }

    @Override
    public void updateNotificationDismissed(StatusBarNotification statusBarNotification) {
        // do nothing
        notificationPublisher.updateNotificationDismissed(statusBarNotification);
    }

}
