package com.mysticwind.linenotificationsupport.utils;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

public class MessageDeduper {

    private static final Comparator<LineNotification> LINE_NOTIFICATION_COMPARATOR =
            Comparator
                    .comparing(LineNotification::getMessage)
                    .thenComparing((n1, n2) -> senderName(n1).compareTo(senderName(n2)))
                    .thenComparing(LineNotification::getTitle)
                    .thenComparing(LineNotification::getLineStickerUrl, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(LineNotification::getChatId)
                    .thenComparing(LineNotification::getCallState, Comparator.nullsLast(Comparator.naturalOrder()));

    @Builder
    @Value
    public static class DedupeResult {
        private final int notificationId;
        private final String replacedMessage;
        private final LineNotification lineNotification;
    }

    @Builder
    @Data
    public static class State {
        private final LineNotification lineNotification;
        private final int notificationId;

        private int numberOfDuplicates = 0;

        public void increase() {
            this.numberOfDuplicates += 1;
        }
    }

    private static final Map<String, State> CHAT_ID_TO_STATE_MAP = new HashMap<>();

    public Optional<DedupeResult> evaluate(final LineNotification lineNotification,
                                           final int notificationId) {
        final String chatId = lineNotification.getChatId();
        final State state = CHAT_ID_TO_STATE_MAP.get(chatId);
        if (state == null || !sameMessage(state.getLineNotification(), lineNotification)) {
            CHAT_ID_TO_STATE_MAP.put(
                    chatId,
                    State.builder()
                            .lineNotification(lineNotification)
                            .notificationId(notificationId)
                            .build()
            );
            return Optional.empty();
        }
        // need dedupe
        state.increase();

        return Optional.of(
                DedupeResult.builder()
                        .notificationId(state.notificationId)
                        .replacedMessage(buildNewMessage(state))
                        .lineNotification(state.lineNotification)
                        .build()
        );
    }

    private boolean sameMessage(final LineNotification previousLineNotification,
                                final LineNotification newLineNotification) {
        return LINE_NOTIFICATION_COMPARATOR.compare(previousLineNotification, newLineNotification) == 0;
    }

    private String buildNewMessage(State state) {
        return String.format("%s (%d)", state.getLineNotification().getMessage(), state.numberOfDuplicates + 1);
    }

    private static String senderName(final LineNotification lineNotification) {
        return lineNotification.getSender().getName().toString();
    }

}
