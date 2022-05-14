package com.mysticwind.linenotificationsupport.call.model;

import java.time.Instant;
import java.util.Optional;

import lombok.Data;

@Data
public class CallHistory {
    private final Instant start;
    private Instant end;

    public static CallHistory start() {
        final CallHistory callHistory = new CallHistory(Instant.now());
        return callHistory;
    }

    public void end() {
        if (this.end != null) {
            throw new IllegalStateException(String.format("End time [%s] already exists!", end));
        }
        this.end = Instant.now();
    }

    public Optional<Instant> getEnd() {
        return Optional.ofNullable(end);
    }

    @Override
    public String toString() {
        return "CallHistory{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }

}
