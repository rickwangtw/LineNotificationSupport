package com.mysticwind.linenotificationsupport.debug;

import com.mysticwind.linenotificationsupport.BuildConfig;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DebugModeProvider {

    @Inject
    public DebugModeProvider() {
    }

    public boolean isDebugMode() {
        return StringUtils.equals(BuildConfig.BUILD_TYPE, "debug");
    }

}
