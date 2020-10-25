package com.mysticwind.linenotificationsupport.localization;

public class LocalizationHelper {

    public static boolean isCallInProgressText(String text) {
        return LocalizationConstants.CALL_IN_PROGRESS_TEXTS.contains(text);
    }

    public static boolean isReplyActionText(String text) {
        return LocalizationConstants.REPLY_ACTION_TEXTS.contains(text);
    }

}
