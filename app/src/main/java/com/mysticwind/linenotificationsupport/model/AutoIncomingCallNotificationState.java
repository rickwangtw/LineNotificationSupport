package com.mysticwind.linenotificationsupport.model;

import java.time.Instant;

import lombok.Builder;

public class AutoIncomingCallNotificationState {

    private final LineNotification lineNotification;
    private final double waitDurationInSeconds;

    private long autoNotifyCallUntilTimestampSecond;
    private int incomingCallNotificationId = 0;

    @Builder
    AutoIncomingCallNotificationState(final LineNotification lineNotification,
                                      final double waitDurationInSeconds,
                                      final long timeoutInSeconds) {
        this.lineNotification = lineNotification;
        this.waitDurationInSeconds = waitDurationInSeconds;
        this.autoNotifyCallUntilTimestampSecond = Instant.now().getEpochSecond() + timeoutInSeconds;
    }

    public LineNotification getLineNotification() {
        return this.lineNotification;
    }

    public double getWaitDurationInSeconds() {
        return this.waitDurationInSeconds;
    }

    public boolean shouldNotify() {
        return autoNotifyCallUntilTimestampSecond > Instant.now().getEpochSecond();
    }

    public void notified(int incomingCallNotificationId) {
        this.incomingCallNotificationId = incomingCallNotificationId;
    }

    public int getLastIncomingCallNotificationId() {
        return this.incomingCallNotificationId;
    }

    public void setMissedCall() {
        this.autoNotifyCallUntilTimestampSecond = 0L;
    }

    public void setAccepted() {
        this.autoNotifyCallUntilTimestampSecond = 0L;
    }

    public void cancel() {
        this.autoNotifyCallUntilTimestampSecond = 0L;
    }

}
