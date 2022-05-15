package com.mysticwind.linenotificationsupport.call;

import com.mysticwind.linenotificationsupport.call.model.CallEvent;

public interface OnCallEventHandler {

    void handle(CallEvent callEvent);

}
