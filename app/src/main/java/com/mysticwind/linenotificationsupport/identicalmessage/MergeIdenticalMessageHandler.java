package com.mysticwind.linenotificationsupport.identicalmessage;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

public class MergeIdenticalMessageHandler implements IdenticalMessageHandler {

    private IdenticalMessageEvaluator identicalMessageEvaluator;

    public MergeIdenticalMessageHandler(IdenticalMessageEvaluator identicalMessageEvaluator) {
        this.identicalMessageEvaluator = identicalMessageEvaluator;
    }

    @Override
    public Optional<Pair<LineNotification, Integer>> handle(final LineNotification lineNotification,
                                                            final int notificationId) {
        final IdenticalMessageEvaluator.EvaluationResult result =
                identicalMessageEvaluator.evaluate(lineNotification, notificationId);

        if (!result.isDuplicate()) {
            return Optional.of(
                    Pair.of(
                            lineNotification,
                            notificationId
                    )
            );
        }
        return Optional.of(
                Pair.of(
                        buildMergedNotification(result, lineNotification),
                        result.getNotificationId().get()
                )
        );
    }

    private LineNotification buildMergedNotification(IdenticalMessageEvaluator.EvaluationResult result,
                                                     LineNotification lineNotification) {
        return result.getPreviousLineNotification().get().toBuilder()
                .message(buildNewMessage(result))
                .build();
    }

    private String buildNewMessage(IdenticalMessageEvaluator.EvaluationResult result) {
        return String.format("%s (%d)",
                result.getPreviousLineNotification().get().getMessage(),
                result.getNumberOfDuplicates());
    }

}
