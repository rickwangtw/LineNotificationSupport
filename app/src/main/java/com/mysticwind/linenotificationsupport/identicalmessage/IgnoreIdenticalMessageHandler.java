package com.mysticwind.linenotificationsupport.identicalmessage;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

public class IgnoreIdenticalMessageHandler implements IdenticalMessageHandler {

    private IdenticalMessageEvaluator identicalMessageEvaluator;

    public IgnoreIdenticalMessageHandler(IdenticalMessageEvaluator identicalMessageEvaluator) {
        this.identicalMessageEvaluator = identicalMessageEvaluator;
    }

    @Override
    public Optional<Pair<LineNotification, Integer>> handle(final LineNotification lineNotification,
                                                            final int notificationId) {
        // call messaged should always be treated as new
        if (lineNotification.getCallState() == LineNotification.CallState.INCOMING ||
                lineNotification.getCallState() == LineNotification.CallState.MISSED_CALL ||
                // this is de-duped, auto notifications may not stop at all
                lineNotification.getCallState() == LineNotification.CallState.IN_A_CALL) {
            return Optional.of(
                    Pair.of(
                            lineNotification,
                            notificationId
                    )
            );
        }
        final IdenticalMessageEvaluator.EvaluationResult result =
                identicalMessageEvaluator.evaluate(lineNotification, notificationId);
        if (result.isDuplicate()) {
            return Optional.empty();
        }
        return Optional.of(
                Pair.of(
                        lineNotification,
                        notificationId
                )
        );
    }

}
