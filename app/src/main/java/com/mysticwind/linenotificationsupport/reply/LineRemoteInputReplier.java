package com.mysticwind.linenotificationsupport.reply;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.apache.commons.lang3.Validate;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

/**
 * https://medium.com/@polidea/how-to-respond-to-any-messaging-notification-on-android-7befa483e2d7
 * https://stackoverflow.com/questions/59251922/how-to-send-a-reply-from-a-notification
 */
@Singleton
public class LineRemoteInputReplier {

    private static final String LINE_TEXT_REMOTE_INPUT_KEY = "line.text";

    private final PendingIntent.OnFinished onFinished = new PendingIntent.OnFinished() {
        @Override
        public void onSendFinished(final PendingIntent pendingIntent, final Intent intent,
                                   final int resultCode, final String resultData, final Bundle resultExtras) {
            Timber.i("Completed sending pending intent action [%s], code [%d], data [%s]",
                    intent.getAction(), resultCode, resultData);
        }
    };

    private final Context context;

    @Inject
    public LineRemoteInputReplier(@ApplicationContext final Context context) {
        this.context = Objects.requireNonNull(context);
    }

    public void sendReply(final Notification.Action replyAction, final String responseText) {
        Objects.requireNonNull(replyAction);
        Objects.requireNonNull(responseText);
        Validate.isTrue(replyAction.getRemoteInputs() != null || replyAction.getRemoteInputs().length > 0,
                "Invalid remote input from reply action: " + replyAction.title);

        final Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        final Bundle bundle = new Bundle();
        final String remoteInputKey = resolveRemoteInputKey(replyAction.getRemoteInputs());
        bundle.putString(remoteInputKey, responseText);

        RemoteInput.addResultsToIntent(replyAction.getRemoteInputs(), intent, bundle);
        try {
            final int code = (int) Instant.now().toEpochMilli();
            replyAction.actionIntent.send(context, code, intent, onFinished, null);
            Timber.d("Sent intent with bundle [%s]", bundle.toString());
        } catch (final PendingIntent.CanceledException e) {
            Timber.e(e, "Failed to send message to LINE: %s", e.getMessage());
        }
    }

    private String resolveRemoteInputKey(final RemoteInput[] remoteInputs) {
        if (remoteInputs.length == 1) {
            return remoteInputs[0].getResultKey();
        }
        final String remoteInputKeys = Arrays.stream(remoteInputs)
                .map(remoteInput -> remoteInput.getResultKey())
                .reduce((remoteInput1, remoteInput2) -> String.format("%1,%2", remoteInput1, remoteInput2))
                .orElse("N/A");
        Timber.w("More than one remoteInput: %s", remoteInputKeys);
        return LINE_TEXT_REMOTE_INPUT_KEY;
    }

}
