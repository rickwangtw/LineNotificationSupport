package com.mysticwind.linenotificationsupport.call;

import com.mysticwind.linenotificationsupport.call.model.CallHistory;

import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class CallStatusTrackingManager {

    private final Stack<CallHistory> callHistoryStack = new Stack<>();

    @Inject
    public CallStatusTrackingManager() {
    }

    public void recordCallStart() {
        Timber.d("recordCallStart()");

        // self correct invalid data
        while (!callHistoryStack.isEmpty()) {
            if (callHistoryStack.peek().getEnd().isPresent()) {
                break;
            } else {
                final CallHistory callHistoryWithoutEnd = callHistoryStack.pop();
                Timber.w("Call history without end detected with start [%s]", callHistoryWithoutEnd.getStart());
            }
        }

        final CallHistory startedCallHistory = CallHistory.start();
        Timber.i("Call history start [%s]", startedCallHistory);
        callHistoryStack.push(startedCallHistory);
    }

    public void recordCallStop() {
        Timber.d("recordCallStop()");

        // self correct invalid data
        if (callHistoryStack.isEmpty()) {
            Timber.w("No call history, ignoring event");
            return;
        }
        while (!callHistoryStack.isEmpty()) {
            if (callHistoryStack.peek().getEnd().isPresent()) {
                final CallHistory previousFullCallHistory = callHistoryStack.pop();
                Timber.w("Cannot find previous call history, ignoring event and popping previous event [%s]", previousFullCallHistory);
                return;
            } else {
                break;
            }
        }

        if (!callHistoryStack.isEmpty()) {
            final CallHistory lastCallHistory = callHistoryStack.pop();
            lastCallHistory.end();
            Timber.i("Call duration [%d](s) ended [%s]",
                    (lastCallHistory.getEnd().get().getEpochSecond() - lastCallHistory.getStart().getEpochSecond()),
                    lastCallHistory);
        } else {
            Timber.w("Cannot find previous call history, ignoring event");
        }
    }

}
