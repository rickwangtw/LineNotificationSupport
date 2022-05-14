package com.mysticwind.linenotificationsupport.service;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.view.accessibility.AccessibilityEvent;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

import timber.log.Timber;

/*
 * Referenced from the safe-dot-android project
 * https://github.com/kamaravichow/safe-dot-android/blob/862fb3b394255199ca96db4061692eeb55713e0d/app/src/main/java/com/aravi/dotpro/service/DotService.java#L465
 */
public class LineCallStatusDetectionService extends AccessibilityService {

    private static final Set<String> WINDOW_CHANGE_APP_NOISES =
            ImmutableSet.of("com.android.systemui");

    // TODO implement log through Guava
    private boolean recordingInProgress = false;

    // TODO implement log through Guava
    private String currentRunningApp;

    private AudioManager audioManager;
    private AudioManager.AudioRecordingCallback audioRecordingCallback;

    public LineCallStatusDetectionService() {
    }

    @Override
    protected void onServiceConnected() {
        Timber.d("LineCallStatusDetectionService onServiceConnected");

        initializeAudioRecordingCallback();
    }

    private void initializeAudioRecordingCallback() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.registerAudioRecordingCallback(getAudioRecordingCallback(), null);
    }

    private AudioManager.AudioRecordingCallback getAudioRecordingCallback() {
        audioRecordingCallback = new AudioManager.AudioRecordingCallback() {
            @Override
            public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                final boolean newRecordingInProgress;
                // this probably means recording is on
                if (configs.size() > 0) {
                    newRecordingInProgress = true;
                } else {
                    newRecordingInProgress = false;
                }
                if (newRecordingInProgress != recordingInProgress) {
                    Timber.d("Detected change in recording status: [%s] probably request for app [%s]", newRecordingInProgress, currentRunningApp);
                    recordingInProgress = newRecordingInProgress;
                } else {
                    Timber.i("Detected change in recording, but no status change: %s", newRecordingInProgress);
                }
            }
        };
        return audioRecordingCallback;
    }

    @Override
    public void onDestroy() {
        unregisterAudioRecordingCallback();
        super.onDestroy();
    }

    private void unregisterAudioRecordingCallback() {
        if (audioManager != null && audioRecordingCallback != null) {
            audioManager.unregisterAudioRecordingCallback(audioRecordingCallback);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Timber.d("Accessibility event type [%d] action [%d] package [%s] window changes [%d] content change types [%d]",
                event.getEventType(), event.getAction(), event.getPackageName(), event.getWindowChanges(), event.getContentChangeTypes());
        try {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getPackageName() != null) {
                final ComponentName componentName = new ComponentName(event.getPackageName().toString(), event.getClassName().toString());
                if (WINDOW_CHANGE_APP_NOISES.contains(componentName.getPackageName())) {
                    Timber.d("Detected change in package [%s], ignoring", componentName.getPackageName());
                    return;
                }
                if (!StringUtils.equals(currentRunningApp, componentName.getPackageName())) {
                    currentRunningApp = componentName.getPackageName();
                    Timber.d("Detected current running app: %s", componentName.getPackageName());
                }
            }
        } catch (final Exception e) {
            Timber.w(e, "onAccessibilityEvent:" + e.getMessage());
        }
    }

    @Override
    public void onInterrupt() {
    }

}