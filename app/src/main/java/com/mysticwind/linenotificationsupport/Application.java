package com.mysticwind.linenotificationsupport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import timber.log.Timber;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree() {
                @Override
                protected @Nullable String createStackElementTag(@NotNull StackTraceElement element) {
                    final String tag = super.createStackElementTag(element);
                    if (tag == null) {
                        return null;
                    }
                    return "LNS-" + tag;
                }
            });
        }

    }

}
