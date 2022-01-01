package com.mysticwind.linenotificationsupport;

import android.util.Log;

import net.yslibrary.historian.Historian;
import net.yslibrary.historian.tree.HistorianTree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class Application extends android.app.Application {

    @Inject
    public Application() {
    }

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
            try {
                historian.initialize();
            } catch (final Exception e) {
                Timber.e(e, "Failed to initialize Historian: [%s]", e.getMessage());
                historian.terminate();
                historian.initialize();
            }
            Timber.plant(HistorianTree.with(historian));
        }

    }

}
