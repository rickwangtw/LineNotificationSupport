package com.mysticwind.linenotificationsupport.event;

import android.content.Intent;

import org.apache.commons.lang3.StringUtils;

public enum Intents {

    LINE_CALL_START_INTENT("com.mysticwind.linenotificationsupport.line_call_start_intent"),
    LINE_CALL_END_INTENT("com.mysticwind.linenotificationsupport.line_call_end_intent");

    private final String intentAction;

    Intents(String intentAction) {
        StringUtils.isNotBlank(intentAction);
        this.intentAction = intentAction;
    }

    public String getAction() {
        return intentAction;
    }

    public Intent getIntent() {
        return new Intent(intentAction);
    }
}
