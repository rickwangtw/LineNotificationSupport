package com.mysticwind.linenotificationsupport.model;

import com.google.common.collect.ImmutableSet;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import lombok.Builder;

public class AutoIncomingCallNotificationState {

    private final LineNotification lineNotification;
    private final double waitDurationInSeconds;

    private long autoNotifyCallUntilTimestampSecond;
    private Set<Integer> incomingCallNotificationIds = new HashSet<>();

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
        this.incomingCallNotificationIds.add(incomingCallNotificationId);
    }

    public Set<Integer> getIncomingCallNotificationIds() {
        return ImmutableSet.copyOf(this.incomingCallNotificationIds);
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
