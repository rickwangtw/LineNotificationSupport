package com.mysticwind.linenotificationsupport.identicalmessage;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class IdenticalMessageHandledResult {
    private final int notificationId;
    private final String replacedMessage;
    private final LineNotification lineNotification;
}
