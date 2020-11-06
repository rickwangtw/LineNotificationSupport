package com.mysticwind.linenotificationsupport.identicalmessage;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

public class AsIsIdenticalMessageHandler implements IdenticalMessageHandler {

    @Override
    public Optional<Pair<LineNotification, Integer>> handle(LineNotification lineNotification, int notificationId) {
        return Optional.of(Pair.of(lineNotification, notificationId));
    }

}
