package com.mysticwind.linenotificationsupport.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.mysticwind.linenotificationsupport.identicalmessage.AsIsIdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.identicalmessage.IdenticalMessageEvaluator;
import com.mysticwind.linenotificationsupport.identicalmessage.IdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.identicalmessage.IgnoreIdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.identicalmessage.MergeIdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.model.AutoIncomingCallNotificationState;
import com.mysticwind.linenotificationsupport.model.IdenticalMessageHandlingStrategy;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;
import com.mysticwind.linenotificationsupport.notification.DefaultNotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.NullNotificationPublisher;
import com.mysticwind.linenotificationsupport.utils.ChatTitleAndSenderResolver;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class NotificationListenerService
        extends android.service.notification.NotificationListenerService {

    private static final String TAG = "LINE_NOTIFICATION_SUPPORT";

    private static final String LINE_PACKAGE_NAME = "jp.naver.line.android";

    private static final GroupIdResolver GROUP_ID_RESOLVER = new GroupIdResolver();
    private static final NotificationIdGenerator NOTIFICATION_ID_GENERATOR = new NotificationIdGenerator();
    private static final ChatTitleAndSenderResolver CHAT_TITLE_AND_SENDER_RESOLVER = new ChatTitleAndSenderResolver();

    private static final IdenticalMessageEvaluator IDENTICAL_MESSAGE_EVALUATOR = new IdenticalMessageEvaluator();
    private static final MergeIdenticalMessageHandler MERGE_IDENTICAL_MESSAGE_HANDLER = new MergeIdenticalMessageHandler(IDENTICAL_MESSAGE_EVALUATOR);
    private static final IgnoreIdenticalMessageHandler IGNORE_IDENTICAL_MESSAGE_HANDLER = new IgnoreIdenticalMessageHandler(IDENTICAL_MESSAGE_EVALUATOR);
    private static final AsIsIdenticalMessageHandler AS_IS_IDENTICAL_MESSAGE_HANDLER = new AsIsIdenticalMessageHandler();

    private static final Map<IdenticalMessageHandlingStrategy, IdenticalMessageHandler> STRATEGY_TO_HANDLER_MAP = ImmutableMap.of(
            IdenticalMessageHandlingStrategy.IGNORE, IGNORE_IDENTICAL_MESSAGE_HANDLER,
            IdenticalMessageHandlingStrategy.MERGE, MERGE_IDENTICAL_MESSAGE_HANDLER,
            IdenticalMessageHandlingStrategy.SEND_AS_IS, AS_IS_IDENTICAL_MESSAGE_HANDLER
    );

    private final Handler handler = new Handler();

    private AutoIncomingCallNotificationState autoIncomingCallNotificationState;
    private NotificationPublisher notificationPublisher = NullNotificationPublisher.INSTANCE;

    @Override
    public IBinder onBind(Intent intent) {
        this.notificationPublisher = new DefaultNotificationPublisher(this, GROUP_ID_RESOLVER);

        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        this.notificationPublisher = NullNotificationPublisher.INSTANCE;

        return super.onUnbind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        // ignore messages from ourselves
        if (statusBarNotification.getPackageName().startsWith(getPackageName())) {
            return;
        }

        if (shouldIgnoreNotification(statusBarNotification)) {
            return;
        }

        final String stringifiedNotification = MoreObjects.toStringHelper(statusBarNotification)
                .add("packageName", statusBarNotification.getPackageName())
                .add("groupKey", statusBarNotification.getGroupKey())
                .add("key", statusBarNotification.getKey())
                .add("id", statusBarNotification.getId())
                .add("tag", statusBarNotification.getTag())
                .add("user", statusBarNotification.getUser().toString())
                .add("overrideGroupKey", statusBarNotification.getOverrideGroupKey())
                .add("notification", ToStringBuilder.reflectionToString(statusBarNotification.getNotification()))
                .add("actionLabels", extractActionLabels(statusBarNotification))
                .toString();
        Log.i(TAG, String.format("Notification (%s): %s",
                statusBarNotification.getPackageName(),
                stringifiedNotification)
        );

        sendNotification(statusBarNotification);
    }

    private boolean shouldIgnoreNotification(final StatusBarNotification statusBarNotification) {
        final String packageName = statusBarNotification.getPackageName();

        // let's just focus on Line notifications for now
        if (!LINE_PACKAGE_NAME.equals(packageName)) {
            return true;
        }

        // ignore summaries
        if (isSummary(statusBarNotification)) {
            return true;
        }
        return false;
    }

    private boolean isSummary(final StatusBarNotification statusBarNotification) {
        final String summaryText = statusBarNotification.getNotification().extras
                .getString("android.summaryText");
        return StringUtils.isNotBlank(summaryText);
    }

    private String extractActionLabels(StatusBarNotification statusBarNotification) {
        final Notification.Action[] actions = statusBarNotification.getNotification().actions;
        if (ArrayUtils.isEmpty(actions)) {
            return "N/A";
        }
        return Arrays.stream(actions)
                .filter(action -> action.title != null)
                .map(action -> action.title.toString())
                .reduce((title1, title2) -> title1 + "," + title2)
                .orElse("No title");
    }

    private void sendNotification(StatusBarNotification notificationFromLine) {
        final LineNotification lineNotification = new LineNotificationBuilder(this,
                CHAT_TITLE_AND_SENDER_RESOLVER).from(notificationFromLine);

        int notificationId = NOTIFICATION_ID_GENERATOR.getNextNotificationId();

        Optional<Pair<LineNotification, Integer>> notificationAndId =
                handleDuplicate(lineNotification, notificationId);

        if (!notificationAndId.isPresent()) {
            // skip duplicated message
            return;
        }

        notificationPublisher.publishNotification(notificationAndId.get().getLeft(), notificationAndId.get().getRight());

        if (lineNotification.getCallState() == null) {
            return;
        }

        // deal with auto notifications for calls
        if (lineNotification.getCallState() == LineNotification.CallState.INCOMING) {
            if (this.autoIncomingCallNotificationState != null) {
                this.autoIncomingCallNotificationState.cancel();
            }
            this.autoIncomingCallNotificationState = AutoIncomingCallNotificationState.builder()
                    .lineNotification(lineNotification)
                    .waitDurationInSeconds(getWaitDurationInSeconds())
                    .timeoutInSeconds(getAutoSendTimeoutInSecondsFromPreferences())
                    .reverseActionOrder(resolveShouldReverseActionOrder(lineNotification))
                    .build();
            sendIncomingCallNotification(this.autoIncomingCallNotificationState);
        }

        final AutoIncomingCallNotificationState autoIncomingCallNotificationState = this.autoIncomingCallNotificationState;
        if (autoIncomingCallNotificationState == null) {
            return;
        }

        if (lineNotification.getCallState() == LineNotification.CallState.MISSED_CALL) {
            autoIncomingCallNotificationState.setMissedCall();
        } else if (lineNotification.getCallState() == LineNotification.CallState.IN_A_CALL) {
            autoIncomingCallNotificationState.setAccepted();
        }
    }

    private Optional<Pair<LineNotification, Integer>> handleDuplicate(LineNotification lineNotification, int notificationId) {
        final IdenticalMessageHandler handler = selectIdenticalMessageHandler();
        return handler.handle(lineNotification, notificationId);
    }

    private IdenticalMessageHandler selectIdenticalMessageHandler() {
        final IdenticalMessageHandlingStrategy strategy = getIdenticalMessageHandlingStrategyFromPreference();
        return STRATEGY_TO_HANDLER_MAP.getOrDefault(strategy, IGNORE_IDENTICAL_MESSAGE_HANDLER);
    }

    private IdenticalMessageHandlingStrategy getIdenticalMessageHandlingStrategyFromPreference() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String stringStrategy = preferences.getString("identical_message_handling_strategy", "IGNORE");
        return IdenticalMessageHandlingStrategy.valueOf(stringStrategy);
    }

    private double getWaitDurationInSeconds() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean shouldAutoNotify = preferences.getBoolean("auto_call_notifications", true);
        if (!shouldAutoNotify) {
            return 1000; // a random big value
        }
        final String waitTimeString = preferences.getString("auto_notifications_wait", "3.0");
        return Double.parseDouble(waitTimeString);
    }

    private long getAutoSendTimeoutInSecondsFromPreferences() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean shouldAutoNotify = preferences.getBoolean("auto_call_notifications", true);
        if (!shouldAutoNotify) {
            return 0;
        }
        final String timeoutString = preferences.getString("auto_notifications_timeout", "-1");
        final int timeout = parseTimeout(timeoutString);
        if (timeout < 0) {
            // 15 min should be more than enough
            return 15 * 60;
        } else {
            return timeout;
        }
    }

    private int parseTimeout(String timeoutString) {
        try {
            return Integer.parseInt(timeoutString);
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean resolveShouldReverseActionOrder(LineNotification lineNotification) {
        if (LineNotification.CallState.INCOMING != lineNotification.getCallState()) {
            return false;
        }
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean("call_notifications_reverse_action", false);
    }

    private void sendIncomingCallNotification(final AutoIncomingCallNotificationState autoIncomingCallNotificationState) {
        if (!autoIncomingCallNotificationState.shouldNotify()) {
            cancelIncomingCallNotification(autoIncomingCallNotificationState.getIncomingCallNotificationIds());
            return;
        }
        try {
            // resend a new one
            int nextNotificationId = NOTIFICATION_ID_GENERATOR.getNextNotificationId();

            final LineNotification notificationToPublish = buildNotificationWithOrderedAction(autoIncomingCallNotificationState);

            notificationPublisher.publishNotification(notificationToPublish, nextNotificationId);

            autoIncomingCallNotificationState.notified(nextNotificationId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send incoming call notifications: " + e.getMessage(), e);
        }

        scheduleNextIncomingCallNotification(autoIncomingCallNotificationState);
    }

    private LineNotification buildNotificationWithOrderedAction(AutoIncomingCallNotificationState autoIncomingCallNotificationState) {
        if (!autoIncomingCallNotificationState.shouldReverseActionOrder()) {
            return autoIncomingCallNotificationState.getLineNotification();
        }
        final List<Notification.Action> actions = autoIncomingCallNotificationState.getLineNotification().getActions();
        if (actions.size() >= 2) {
            final Notification.Action firstAction = actions.get(0);
            actions.add(0, actions.get(1));
            actions.add(1, firstAction);
        }
        return autoIncomingCallNotificationState.getLineNotification().toBuilder()
                .actions(actions)
                .build();
    }

    private void cancelIncomingCallNotification(final Set<Integer> notificationIdsToCancel) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(NotificationListenerService.this);
        for (final int notificationId : notificationIdsToCancel) {
            try {
                notificationManager.cancel(notificationId);
            } catch (final Exception e) {
                Log.w(TAG, String.format("Failed to cancel notification %d: %s", notificationId, e.getMessage(), e));
            }
        }
    }

    private void scheduleNextIncomingCallNotification(final AutoIncomingCallNotificationState autoIncomingCallNotificationState) {
        final long delayInMillis = (long) (autoIncomingCallNotificationState.getWaitDurationInSeconds() * 1000);

        handler.postDelayed(new Runnable() {
            public void run() {
                sendIncomingCallNotification(autoIncomingCallNotificationState);
            }
        }, delayInMillis);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        super.onNotificationRemoved(statusBarNotification);

        if (shouldIgnoreNotification(statusBarNotification)) {
            return;
        }

        final LineNotification dismissedLineNotification = new LineNotificationBuilder(this,
                CHAT_TITLE_AND_SENDER_RESOLVER).from(statusBarNotification);

        if (LineNotification.CallState.INCOMING == dismissedLineNotification.getCallState() &&
                this.autoIncomingCallNotificationState != null) {
            this.autoIncomingCallNotificationState.cancel();
        }

        if (shouldAutoDismissLineNotificationSupportNotifications()) {
            dismissLineNotificationSupportNotifications(dismissedLineNotification.getChatId());
        }
    }

    private boolean shouldAutoDismissLineNotificationSupportNotifications() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean("auto_dismiss_line_notification_support_messages", true);
    }

    private void dismissLineNotificationSupportNotifications(final String groupId) {
        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);

        final Set<Integer> notificationIdsToCancel = Arrays.stream(notificationManager.getActiveNotifications())
                // we're only clearing notifications from our package
                .filter(notification -> notification.getPackageName().equals(this.getPackageName()))
                // LINE only shows the last message for a chat, we'll dismiss all of the messages in the same chat ID
                .filter(notification -> groupId.equals(notification.getNotification().getGroup()))
                .map(notification -> notification.getId())
                .collect(Collectors.toSet());

        for (Integer notificationId : notificationIdsToCancel) {
            Log.d(TAG, "Cancelling notification: " + notificationId);
            notificationManager.cancel(notificationId.intValue());
        }

    }

}
