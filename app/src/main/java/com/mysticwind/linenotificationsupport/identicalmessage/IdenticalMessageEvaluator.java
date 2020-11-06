package com.mysticwind.linenotificationsupport.identicalmessage;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

public class IdenticalMessageEvaluator {

    private static final Comparator<LineNotification> LINE_NOTIFICATION_COMPARATOR =
            Comparator
                    .comparing(LineNotification::getMessage)
                    .thenComparing((n1, n2) -> senderName(n1).compareTo(senderName(n2)))
                    .thenComparing(LineNotification::getTitle)
                    .thenComparing(LineNotification::getLineStickerUrl, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(LineNotification::getChatId)
                    .thenComparing(LineNotification::getCallState, Comparator.nullsLast(Comparator.naturalOrder()));


    private static final Map<String, IdenticalMessageEvaluator.State> CHAT_ID_TO_STATE_MAP = new HashMap<>();

    @Builder
    @Data
    public static class State {

        private final LineNotification lineNotification;
        private final int notificationid;
        private int numberOfDuplicates = 0;

        public void increase() {
            this.numberOfDuplicates += 1;
        }
    }

    @Value
    public static class EvaluationResult {
        private final LineNotification lineNotification;
        private final Integer notificationId;
        private final int numberOfDuplicates;

        public static EvaluationResult noDuplicate() {
            return new EvaluationResult(null, null, 0);
        }

        public static EvaluationResult withDuplicate(final LineNotification previousLineNotification,
                                              final int previousNotificationId,
                                              final int numberOfDuplicates) {
            return new EvaluationResult(
                    previousLineNotification, previousNotificationId, numberOfDuplicates);
        }


        public Optional<LineNotification> getPreviousLineNotification() {
            return Optional.ofNullable(lineNotification);
        }

        public Optional<Integer> getNotificationId() {
            return Optional.ofNullable(notificationId);
        }

        public boolean isDuplicate() {
            return numberOfDuplicates > 0;
        }

    }

    public EvaluationResult evaluate(LineNotification lineNotification, int newNotificationId) {
        final String chatId = lineNotification.getChatId();
        final State state = CHAT_ID_TO_STATE_MAP.get(chatId);
        if (state == null || !hasSameMessage(state.getLineNotification(), lineNotification)) {
            CHAT_ID_TO_STATE_MAP.put(
                    chatId,
                    State.builder()
                            .lineNotification(lineNotification)
                            .notificationid(newNotificationId)
                            .build()
            );
            return EvaluationResult.noDuplicate();
        }
        state.increase();

        return EvaluationResult.withDuplicate(
                state.getLineNotification(),
                state.getNotificationid(),
                state.numberOfDuplicates + 1
        );
    }

    private boolean hasSameMessage(final LineNotification previousLineNotification,
                                   final LineNotification newLineNotification) {
        return LINE_NOTIFICATION_COMPARATOR.compare(previousLineNotification, newLineNotification) == 0;
    }

    private static String senderName(final LineNotification lineNotification) {
        return lineNotification.getSender().getName().toString();
    }

}
