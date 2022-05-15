package com.mysticwind.linenotificationsupport.service;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.view.accessibility.AccessibilityEvent;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.call.CallStatusTrackingManager;
import com.mysticwind.linenotificationsupport.call.OnCallEventHandler;
import com.mysticwind.linenotificationsupport.call.model.CallEvent;
import com.mysticwind.linenotificationsupport.event.Intents;
import com.mysticwind.linenotificationsupport.line.Constants;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/*
 * Referenced from the safe-dot-android project
 * https://github.com/kamaravichow/safe-dot-android/blob/862fb3b394255199ca96db4061692eeb55713e0d/app/src/main/java/com/aravi/dotpro/service/DotService.java#L465
 */
@AndroidEntryPoint
public class LineCallStatusDetectionService extends AccessibilityService {

    private static final Set<String> WINDOW_CHANGE_APP_NOISES =
            ImmutableSet.of("com.android.systemui");

    private static final Map<CallEvent, Intents> CALL_EVENT_TO_INTENT_MAP = ImmutableMap.of(
            CallEvent.CALL_START, Intents.LINE_CALL_START_INTENT,
            CallEvent.CALL_END, Intents.LINE_CALL_END_INTENT
    );

    // TODO implement log through Guava
    private boolean recordingInProgress = false;

    // TODO implement log through Guava
    private String currentRunningApp;

    @Inject
    CallStatusTrackingManager callStatusTrackingManager;

    private AudioManager audioManager;
    private AudioManager.AudioRecordingCallback audioRecordingCallback;
    private OnCallEventHandler onCallEventHandler = new OnCallEventHandler() {
        @Override
        public void handle(final CallEvent callEvent) {
            final Intents intent = CALL_EVENT_TO_INTENT_MAP.get(callEvent);
            if (intent == null) {
                return;
            }
            sendBroadcast(intent.getIntent());
        }
    };

    public LineCallStatusDetectionService() {
    }

    @Override
    protected void onServiceConnected() {
        Timber.d("LineCallStatusDetectionService onServiceConnected");

        initializeAudioRecordingCallback();
        callStatusTrackingManager.registerOnCallEventHandler(onCallEventHandler);
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
                    Timber.i("Detected change in recording status: [%s] probably request for app [%s]", newRecordingInProgress, currentRunningApp);
                    recordingInProgress = newRecordingInProgress;
                    if (recordingInProgress && StringUtils.equals(Constants.LINE_PACKAGE_NAME, currentRunningApp)) {
                        callStatusTrackingManager.recordCallStart();
                    } else if (!recordingInProgress) {
                        callStatusTrackingManager.recordCallStop();
                    }
                } else {
                    Timber.d("Detected change in recording, but no status change: %s", newRecordingInProgress);
                }
            }
        };
        return audioRecordingCallback;
    }

    @Override
    public void onDestroy() {
        unregisterAudioRecordingCallback();
        callStatusTrackingManager.unregisterOnCallEventHandler(onCallEventHandler);
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

        if (StringUtils.isBlank(event.getPackageName())) {
            return;
        }
        final ComponentName componentName = new ComponentName(event.getPackageName().toString(), event.getClassName().toString());
        if (WINDOW_CHANGE_APP_NOISES.contains(componentName.getPackageName())) {
            Timber.d("Detected change in package [%s], ignoring", componentName.getPackageName());
            return;
        }
        final String packageName = componentName.getPackageName();
        if (StringUtils.equals(currentRunningApp, packageName)) {
            Timber.d("Package [%s] is the same as current running app, ignoring ...", packageName);
            return;
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentRunningApp = packageName;
            Timber.i("Detected current running app: %s", componentName.getPackageName());
        }
        // be extra more sensitive to LINE windows
        if (StringUtils.equals(Constants.LINE_PACKAGE_NAME, packageName) &&
                event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) > 0)) {
            currentRunningApp = packageName;
            Timber.i("[Extra] Detected current running app: %s", componentName.getPackageName());
        }
    }

    @Override
    public void onInterrupt() {
    }

}