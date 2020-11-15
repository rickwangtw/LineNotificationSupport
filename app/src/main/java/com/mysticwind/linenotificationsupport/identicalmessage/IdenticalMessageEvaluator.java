package com.mysticwind.linenotificationsupport.identicalmessage;

import androidx.annotation.VisibleForTesting;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

public class IdenticalMessageEvaluator {

    @VisibleForTesting
    protected static final Comparator<LineNotification> LINE_NOTIFICATION_COMPARATOR =
            Comparator
                    .comparing(LineNotification::getMessage, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing((n1, n2) -> compareSender(n1, n2))
                    .thenComparing(LineNotification::getTitle, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(LineNotification::getLineStickerUrl, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(LineNotification::getChatId, Comparator.nullsLast(Comparator.naturalOrder()))
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

    private static int compareSender(LineNotification lineNotification1, LineNotification lineNotification2) {
        final String senderName1 = senderName(lineNotification1);
        final String senderName2 = senderName(lineNotification2);
        return StringUtils.compare(senderName1, senderName2);
    }

    private static String senderName(final LineNotification lineNotification) {
        if (lineNotification.getSender() == null) {
            return null;
        }
        if (StringUtils.isBlank(lineNotification.getSender().getName())) {
            return null;
        }
        return lineNotification.getSender().getName().toString();
    }

}
