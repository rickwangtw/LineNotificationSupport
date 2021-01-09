package com.mysticwind.linenotificationsupport;

import android.util.Log;

import net.yslibrary.historian.Historian;
import net.yslibrary.historian.tree.HistorianTree;

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
            Historian historian = Historian.builder(this)
                    .size(100_000)
                    .logLevel(Log.DEBUG)
                    .debug(true)
                    .build();
            historian.initialize();
            Timber.plant(HistorianTree.with(historian));
        }

    }

}
