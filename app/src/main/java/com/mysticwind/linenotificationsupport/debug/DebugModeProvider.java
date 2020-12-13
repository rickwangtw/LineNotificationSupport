package com.mysticwind.linenotificationsupport.debug;

import com.mysticwind.linenotificationsupport.BuildConfig;

import org.apache.commons.lang3.StringUtils;

public class DebugModeProvider {

    public boolean isDebugMode() {
        return StringUtils.equals(BuildConfig.BUILD_TYPE, "debug");
    }

}
