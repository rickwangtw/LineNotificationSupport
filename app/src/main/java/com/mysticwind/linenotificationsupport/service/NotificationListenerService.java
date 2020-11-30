package com.mysticwind.linenotificationsupport.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.room.Room;

import com.google.common.collect.ImmutableMap;
import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.debug.DebugModeProvider;
import com.mysticwind.linenotificationsupport.debug.history.manager.NotificationHistoryManager;
import com.mysticwind.linenotificationsupport.debug.history.manager.impl.NullNotificationHistoryManager;
import com.mysticwind.linenotificationsupport.debug.history.manager.impl.RoomNotificationHistoryManager;
import com.mysticwind.linenotificationsupport.identicalmessage.AsIsIdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.identicalmessage.IdenticalMessageEvaluator;
import com.mysticwind.linenotificationsupport.identicalmessage.IdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.identicalmessage.IgnoreIdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.identicalmessage.MergeIdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.model.AutoIncomingCallNotificationState;
import com.mysticwind.linenotificationsupport.model.IdenticalMessageHandlingStrategy;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;
import com.mysticwind.linenotificationsupport.notification.MaxNotificationHandlingNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.notification.NotificationCounter;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.NullNotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.SimpleNotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.SummaryNotificationPublisher;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.persistence.AppDatabase;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.ChatTitleAndSenderResolver;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationPrinter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import timber.log.Timber;

import static com.mysticwind.linenotificationsupport.line.Constants.LINE_PACKAGE_NAME;

public class NotificationListenerService
        extends android.service.notification.NotificationListenerService {

    private static final String GROUP_MESSAGE_GROUP_KEY = "NOTIFICATION_GROUP_MESSAGE";

    private static final GroupIdResolver GROUP_ID_RESOLVER = new GroupIdResolver();
    private static final NotificationIdGenerator NOTIFICATION_ID_GENERATOR = new NotificationIdGenerator();
    private static final ChatTitleAndSenderResolver CHAT_TITLE_AND_SENDER_RESOLVER = new ChatTitleAndSenderResolver();
    private static final StatusBarNotificationPrinter NOTIFICATION_PRINTER = new StatusBarNotificationPrinter();
    private static final DebugModeProvider DEBUG_MODE_PROVIDER = new DebugModeProvider();

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
    private NotificationHistoryManager notificationHistoryManager = NullNotificationHistoryManager.INSTANCE;
    private NotificationCounter notificationCounter;
    private SummaryNotificationPublisher summaryNotificationPublisher;

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preferenceKey) {
            if (PreferenceProvider.MAX_NOTIFICATION_WORKAROUND_PREFERENCE_KEY.equals(preferenceKey)) {
                NotificationListenerService.this.notificationPublisher = buildNotificationPublisher();
            }
        }
    };

    private NotificationPublisher buildNotificationPublisher() {
        return buildNotificationPublisher(getPreferenceProvider().shouldExecuteMaxNotificationWorkaround());
    }

    private NotificationPublisher buildNotificationPublisher(boolean handleMaxNotificationAndroidLimit) {
        final SimpleNotificationPublisher simpleNotificationPublisher = new SimpleNotificationPublisher(this, getPackageName(), GROUP_ID_RESOLVER);

        if (!handleMaxNotificationAndroidLimit) {
            return simpleNotificationPublisher;
        }

        return new MaxNotificationHandlingNotificationPublisherDecorator(
                handler, simpleNotificationPublisher, notificationCounter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.notificationCounter = new NotificationCounter((int) getMaxNotificationsPerApp());

        this.notificationPublisher = buildNotificationPublisher(
                getPreferenceProvider().shouldExecuteMaxNotificationWorkaround()
        );

        new NotificationGroupCreator(
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE),
                new AndroidFeatureProvider(), getPreferenceProvider())
                .createNotificationGroups();

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

        if (DEBUG_MODE_PROVIDER.isDebugMode()) {
            AppDatabase appDatabase = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "database").build();

            this.notificationHistoryManager = new RoomNotificationHistoryManager(appDatabase, NOTIFICATION_PRINTER);
        }

        this.summaryNotificationPublisher = new SummaryNotificationPublisher(this,
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE), getPackageName(), GROUP_ID_RESOLVER);

        return super.onBind(intent);
    }

    private long getMaxNotificationsPerApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return 25;
        } else {
            return 50;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        this.notificationPublisher = NullNotificationPublisher.INSTANCE;

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

        this.notificationHistoryManager = NullNotificationHistoryManager.INSTANCE;
        this.summaryNotificationPublisher = null;

        return super.onUnbind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {

        handleSelfNotificationPublished(statusBarNotification);

        // ignore messages from ourselves
        if (statusBarNotification.getPackageName().startsWith(getPackageName())) {
            return;
        }

        if (shouldIgnoreNotification(statusBarNotification)) {
            return;
        }

        NOTIFICATION_PRINTER.print("Received", statusBarNotification);
        notificationHistoryManager.record(statusBarNotification, getLineAppVersion());

        sendNotification(statusBarNotification);
    }

    private boolean shouldIgnoreNotification(final StatusBarNotification statusBarNotification) {
        final String packageName = statusBarNotification.getPackageName();

        // let's just focus on Line notifications for now
        if (!LINE_PACKAGE_NAME.equals(packageName)) {
            return true;
        }

        // ignore summaries
        if (StatusBarNotificationExtractor.isSummary(statusBarNotification)) {
            return true;
        }

        // don't know why, but this seems to filter out some duplicated messages
        if (LineNotificationBuilder.GENERAL_NOTIFICATION_CHANNEL.equals(statusBarNotification.getNotification().getChannelId()) &&
                GROUP_MESSAGE_GROUP_KEY.equals(statusBarNotification.getNotification().getGroup())) {
            return true;
        }

        return false;
    }

    // TODO remove one of the duplicates
    private String getLineAppVersion() {
        // https://stackoverflow.com/questions/50795458/android-how-to-get-any-application-version-by-package-name
        final PackageManager packageManager = getPackageManager();
        try {
            final PackageInfo packageInfo = packageManager.getPackageInfo(LINE_PACKAGE_NAME, 0);
            return packageInfo.versionName;
        } catch (final PackageManager.NameNotFoundException e) {
            Timber.e(e, "LINE not installed. Package: " + LINE_PACKAGE_NAME);
            return null;
        }
    }

    private void sendNotification(StatusBarNotification notificationFromLine) {
        final LineNotification lineNotification = new LineNotificationBuilder(this,
                CHAT_TITLE_AND_SENDER_RESOLVER, NOTIFICATION_PRINTER).from(notificationFromLine);

        int notificationId = NOTIFICATION_ID_GENERATOR.getNextNotificationId();

        Optional<Pair<LineNotification, Integer>> notificationAndId =
                handleDuplicate(lineNotification, notificationId);

        if (!notificationAndId.isPresent()) {
            // skip duplicated message
            return;
        }

        LineNotification actionAdjustedLineNotification = adjustActionOrder(notificationAndId.get().getLeft());

        notificationPublisher.publishNotification(actionAdjustedLineNotification, notificationAndId.get().getRight());

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
                    .build();
            this.autoIncomingCallNotificationState.notified(notificationAndId.get().getRight());
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

    private LineNotification adjustActionOrder(LineNotification lineNotification) {
        if (!shouldReverseActionOrder(lineNotification)) {
            return lineNotification;
        }
        final List<Notification.Action> actions = lineNotification.getActions();
        if (actions.size() >= 2) {
            final Notification.Action firstAction = actions.get(0);
            actions.add(0, actions.get(1));
            actions.add(1, firstAction);
        }
        return lineNotification.toBuilder()
                .actions(actions)
                .build();
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

    private boolean shouldReverseActionOrder(LineNotification lineNotification) {
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
            LineNotification lineNotificationWithUpdatedTimestamp =
                    autoIncomingCallNotificationState.getLineNotification().toBuilder()
                            // very interesting that the timestamp needs to be updated for the watch to vibrate
                            .timestamp(Instant.now().toEpochMilli())
                            .build();

            notificationPublisher.publishNotification(
                    lineNotificationWithUpdatedTimestamp,
                    autoIncomingCallNotificationState.getIncomingCallNotificationIds().iterator().next());
        } catch (Exception e) {
            Timber.e(e, "Failed to send incoming call notifications: " + e.getMessage());
        }

        scheduleNextIncomingCallNotification(autoIncomingCallNotificationState);
    }

    private void cancelIncomingCallNotification(final Set<Integer> notificationIdsToCancel) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(NotificationListenerService.this);
        for (final int notificationId : notificationIdsToCancel) {
            try {
                notificationManager.cancel(notificationId);
            } catch (final Exception e) {
                Timber.w(e, String.format("Failed to cancel notification %d: %s", notificationId, e.getMessage()));
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

        handleSelfNotificationDismissed(statusBarNotification);

        if (shouldIgnoreNotification(statusBarNotification)) {
            return;
        }

        final LineNotification dismissedLineNotification = new LineNotificationBuilder(
                this, CHAT_TITLE_AND_SENDER_RESOLVER, NOTIFICATION_PRINTER)
                .from(statusBarNotification);

        if (LineNotification.CallState.INCOMING == dismissedLineNotification.getCallState() &&
                this.autoIncomingCallNotificationState != null) {
            this.autoIncomingCallNotificationState.cancel();
        }

        if (shouldAutoDismissLineNotificationSupportNotifications()) {
            dismissLineNotificationSupportNotifications(dismissedLineNotification.getChatId());
        }
    }

    private PreferenceProvider getPreferenceProvider() {
        return new PreferenceProvider(PreferenceManager.getDefaultSharedPreferences(this));
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
            Timber.d("Cancelling notification: " + notificationId);
            notificationManager.cancel(notificationId.intValue());
        }
    }

    private void handleSelfNotificationPublished(StatusBarNotification statusBarNotification) {
        if (!StringUtils.equals(statusBarNotification.getPackageName(), getPackageName())) {
            return;
        }
        if (StatusBarNotificationExtractor.isSummary(statusBarNotification)) {
            return;
        }
        if (notificationCounter != null) {
            notificationCounter.notified(statusBarNotification.getNotification().getGroup(), statusBarNotification.getId());
        }
        if (summaryNotificationPublisher != null) {
            summaryNotificationPublisher.updateSummary(statusBarNotification.getNotification().getGroup());
        }
    }

    private void handleSelfNotificationDismissed(StatusBarNotification statusBarNotification) {
        if (!StringUtils.equals(statusBarNotification.getPackageName(), getPackageName())) {
            return;
        }
        if (StatusBarNotificationExtractor.isSummary(statusBarNotification)) {
            return;
        }
        if (notificationCounter != null) {
            notificationCounter.dismissed(statusBarNotification.getNotification().getGroup(), statusBarNotification.getId());
        }
        if (summaryNotificationPublisher != null) {
            summaryNotificationPublisher.updateSummary(statusBarNotification.getNotification().getGroup());
        }
        notificationPublisher.updateNotificationDismissed(statusBarNotification);
    }

}
