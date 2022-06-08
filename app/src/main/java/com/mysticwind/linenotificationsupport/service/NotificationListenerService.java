package com.mysticwind.linenotificationsupport.service;

import static com.mysticwind.linenotificationsupport.line.Constants.LINE_PACKAGE_NAME;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.conversationstarter.ConversationStarterNotificationManager;
import com.mysticwind.linenotificationsupport.debug.DebugModeProvider;
import com.mysticwind.linenotificationsupport.identicalmessage.AsIsIdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.identicalmessage.IdenticalMessageEvaluator;
import com.mysticwind.linenotificationsupport.identicalmessage.IdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.identicalmessage.IgnoreIdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.identicalmessage.MergeIdenticalMessageHandler;
import com.mysticwind.linenotificationsupport.model.AutoIncomingCallNotificationState;
import com.mysticwind.linenotificationsupport.model.IdenticalMessageHandlingStrategy;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisherFactory;
import com.mysticwind.linenotificationsupport.notification.impl.DefaultAndroidNotificationManager;
import com.mysticwind.linenotificationsupport.notification.impl.DumbNotificationCounter;
import com.mysticwind.linenotificationsupport.notification.reactor.DismissedNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.IncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.Reaction;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class NotificationListenerService
        extends android.service.notification.NotificationListenerService {

    public static final String DELETE_FRIEND_NAME_CACHE_ACTION = "com.mysticwind.linenotificationsupport.action.deletefriendnamecache";

    private static final long NOTIFICATION_COUNTER_CHECK_PERIOD = 60_000L;

    private static final IdenticalMessageEvaluator IDENTICAL_MESSAGE_EVALUATOR = new IdenticalMessageEvaluator();
    private static final MergeIdenticalMessageHandler MERGE_IDENTICAL_MESSAGE_HANDLER = new MergeIdenticalMessageHandler(IDENTICAL_MESSAGE_EVALUATOR);
    private static final IgnoreIdenticalMessageHandler IGNORE_IDENTICAL_MESSAGE_HANDLER = new IgnoreIdenticalMessageHandler(IDENTICAL_MESSAGE_EVALUATOR);
    private static final AsIsIdenticalMessageHandler AS_IS_IDENTICAL_MESSAGE_HANDLER = new AsIsIdenticalMessageHandler();

    private static final Map<IdenticalMessageHandlingStrategy, IdenticalMessageHandler> STRATEGY_TO_HANDLER_MAP = ImmutableMap.of(
            IdenticalMessageHandlingStrategy.IGNORE, IGNORE_IDENTICAL_MESSAGE_HANDLER,
            IdenticalMessageHandlingStrategy.MERGE, MERGE_IDENTICAL_MESSAGE_HANDLER,
            IdenticalMessageHandlingStrategy.SEND_AS_IS, AS_IS_IDENTICAL_MESSAGE_HANDLER
    );

    private static final Set<String> PREFERENCE_KEYS_THAT_TRIGGER_REBUILDING_NOTIFICATION_PUBLISHER = ImmutableSet.of(
            PreferenceProvider.MAX_NOTIFICATION_WORKAROUND_PREFERENCE_KEY,
            PreferenceProvider.USE_MESSAGE_SPLITTER_PREFERENCE_KEY,
            PreferenceProvider.SINGLE_NOTIFICATION_CONVERSATIONS_KEY
    );

    private AutoIncomingCallNotificationState autoIncomingCallNotificationState;

    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preferenceKey) {
            Timber.d("onSharedPreferenceChangeListener: updated preference [%s]", preferenceKey);
            if (PREFERENCE_KEYS_THAT_TRIGGER_REBUILDING_NOTIFICATION_PUBLISHER.contains(preferenceKey)) {
                notificationPublisherFactory.notifyChange();
            }
            if (PreferenceProvider.CONVERSATION_STARTER_KEY.equals(preferenceKey)) {
                if (preferenceProvider.shouldShowConversationStarterNotification()) {
                    conversationStarterNotificationManager.publishNotification();
                } else {
                    conversationStarterNotificationManager.cancelNotification();
                }
            }
        }
    };

    private boolean isInitialized = false;
    private boolean isListenerConnected = false;

    @Inject
    LineNotificationBuilder lineNotificationBuilder;

    @Inject
    List<IncomingNotificationReactor> incomingNotificationReactors;

    @Inject
    List<DismissedNotificationReactor> dismissedNotificationReactors;

    @Inject
    SharedPreferences sharedPreferences;

    @Inject
    DefaultAndroidNotificationManager defaultAndroidNotificationManager;

    @Inject
    Handler handler;

    @Inject
    NotificationPublisherFactory notificationPublisherFactory;

    @Inject
    PreferenceProvider preferenceProvider;

    @Inject
    NotificationIdGenerator notificationIdGenerator;

    @Inject
    DumbNotificationCounter dumbNotificationCounter;

    @Inject
    ConversationStarterNotificationManager conversationStarterNotificationManager;

    @Inject
    NotificationGroupCreator notificationGroupCreator;

    @Inject
    DebugModeProvider debugModeProvider;

    @Override
    public void onCreate() {
        super.onCreate();

        Timber.d("NotificationListenerService onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("NotificationListenerService onBind");
        return super.onBind(intent);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();

        isListenerConnected = true;
        Timber.w("NotificationListenerService onListenerConnected");

        if (isInitialized) {
            Timber.d("NotificationListenerService has already been initialized");
            return;
        }

        // setup things that are only available through NotificationListenerServices
        this.defaultAndroidNotificationManager.initialize(
                () -> getActiveNotificationsFromAllAppsSafely(),
                key -> cancelNotification(key)
        );

        // getting active notifications to restore previous state
        final List<StatusBarNotification> existingNotifications = getActiveNotificationsFromAllAppsSafely().stream()
                .filter(notification -> notification.getPackageName().equals(getPackageName()))
                .collect(Collectors.toList());

        if (!existingNotifications.isEmpty()) {
            final String keys = existingNotifications.stream()
                    .map(notification -> notification.getKey())
                    .reduce((key1, key2) -> key1 + "," + key2)
                    .orElse("N/A");
            Timber.w("Existing notifications to restore [%s]", keys);
        } else {
            Timber.d("No existing notifications to restore");
        }

        dumbNotificationCounter.updateStateFromExistingNotifications(existingNotifications);

        notificationPublisherFactory.notifyChangeWithExistingNotifications(existingNotifications);

        notificationGroupCreator.createNotificationGroups();

        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        Timber.d("Registered onSharedPreferenceChangeListener");

        scheduleNotificationCounterCheck();

        isInitialized = true;

        if (preferenceProvider.shouldShowConversationStarterNotification()) {
            conversationStarterNotificationManager.publishNotification();
        }

        Timber.d("Service completed initialization");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isInitialized = false;

        Timber.w("NotificationListenerService onDestroy");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Timber.w("NotificationListenerService onUnbind");

        return super.onUnbind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {

        for (final IncomingNotificationReactor reactor : incomingNotificationReactors) {
            try {
                if (!reactor.interestedPackages().contains(statusBarNotification.getPackageName())) {
                    continue;
                }
                if (StatusBarNotificationExtractor.isSummary(statusBarNotification) && !reactor.isInterestInNotificationGroup()) {
                    continue;
                }

                Timber.d("Processing IncomingNotificationReactor [%s] for notification key [%s]",
                        reactor.getClass().getSimpleName(), statusBarNotification.getKey());
                final Reaction reaction = reactor.reactToIncomingNotification(statusBarNotification);

                // TODO how do we deal with legacy actions e.g. auto dismisses?
                if (reaction == Reaction.STOP_FURTHER_PROCESSING) {
                    Timber.d("IncomingNotificationReactor [%s] requested to [%s] after processing notification key [%s]",
                            reactor.getClass().getSimpleName(), reaction, statusBarNotification.getKey());
                    return;
                }
            } catch (Exception e) {
                Timber.e(e, "[ERROR] Failed to process IncomingNotificationReactor [%s]: error [%s] message [%s]",
                        reactor.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
            }
        }

        try {
            onNotificationPostedUnsafe(statusBarNotification);
        } catch (final Exception e) {
            Timber.e(e, "[ERROR] onNotificationPosted failed to handle exception [%s]", e.getMessage());
            if (debugModeProvider.isDebugMode()) {
                Toast.makeText(this, "[ERROR] LNS onNotificationPosted", Toast.LENGTH_SHORT);
            }
        }
    }

    public void onNotificationPostedUnsafe(final StatusBarNotification statusBarNotification) {
        // ignore messages from ourselves
        if (statusBarNotification.getPackageName().startsWith(getPackageName())) {
            return;
        }

        if (shouldIgnoreNotification(statusBarNotification)) {
            return;
        }

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

        return false;
    }

    private void sendNotification(StatusBarNotification notificationFromLine) {
        final LineNotification lineNotification = lineNotificationBuilder.from(notificationFromLine);

        final int notificationId = notificationIdGenerator.getNextNotificationId();

        final Optional<Pair<LineNotification, Integer>> notificationAndId =
                handleDuplicate(lineNotification, notificationId);

        if (!notificationAndId.isPresent()) {
            // skip duplicated message
            return;
        }

        final LineNotification actionAdjustedLineNotification = adjustActionOrder(notificationAndId.get().getLeft());

        sendNotification(actionAdjustedLineNotification, notificationAndId.get().getRight());
    }

    private void sendNotification(final LineNotification lineNotification, final int notificationId) {
        notificationPublisherFactory.get().publishNotification(lineNotification, notificationId);

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
            this.autoIncomingCallNotificationState.notified(notificationId);
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
        if (lineNotification.getActions().size() < 2) {
            return lineNotification;
        }
        final List<Notification.Action> actions =  new ArrayList<>(lineNotification.getActions());
        final Notification.Action firstAction = actions.get(0);
        actions.add(0, actions.get(1));
        actions.add(1, firstAction);
        return lineNotification.toBuilder()
                .clearActions()
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

            final int notificationId;
            if (preferenceProvider.shouldCreateNewContinuousCallNotifications()) {
                notificationId = notificationIdGenerator.getNextNotificationId();
                autoIncomingCallNotificationState.notified(notificationId);
            } else {
                notificationId = autoIncomingCallNotificationState.getIncomingCallNotificationIds().iterator().next();
            }
            notificationPublisherFactory.get().publishNotification(lineNotificationWithUpdatedTimestamp, notificationId);
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
                Timber.w(e, "Failed to cancel notification %d: %s", notificationId, e.getMessage());
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

    private List<StatusBarNotification> getActiveNotificationsFromAllAppsSafely() {
        final Callable<List<StatusBarNotification>> callable = new Callable<List<StatusBarNotification>>() {
            @Override
            public List<StatusBarNotification> call() {
                final StatusBarNotification[] notifications = getActiveNotifications();
                if (ArrayUtils.isEmpty(notifications)) {
                    return Collections.EMPTY_LIST;
                }
                return Arrays.asList(notifications);
            }
        };

        final RetryListener retryListener = new RetryListener() {
            @Override
            public <V> void onRetry(Attempt<V> attempt) {
                if (attempt.hasException()) {
                    Timber.w(attempt.getExceptionCause(),
                            "Failed to fetch active notifications attempt [%d] error [%s]",
                            attempt.getAttemptNumber(), attempt.getExceptionCause().getMessage());
                }
                if (attempt.hasResult() && attempt.getAttemptNumber() > 1) {
                    Timber.w("Finally fetched active notifications after [%d] attempts", attempt.getAttemptNumber());
                }
            }
        };

        final Retryer<List<StatusBarNotification>> retryer = RetryerBuilder.<List<StatusBarNotification>>newBuilder()
                .retryIfException()
                .withWaitStrategy(WaitStrategies.fixedWait(100, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withRetryListener(retryListener)
                .build();

        try {
            return retryer.call(callable);
        } catch (final Exception e) {
            Timber.w(e, "Unable to fetch active notifications after retries ... error message [%s]", e.getMessage());
            return Collections.EMPTY_LIST;
        }
    }

    private void scheduleNotificationCounterCheck() {
        handler.postDelayed(
                () -> checkNotificationCounter(),
                NOTIFICATION_COUNTER_CHECK_PERIOD);
    }

    private void checkNotificationCounter() {
        if (!isListenerConnected) {
            Timber.w("Listener is not connected. Skipping notification counter check");
            scheduleNotificationCounterCheck();
            return;
        }
        final Multimap<String, String> groupToNotificationKeyMultimap = HashMultimap.create();
        getActiveNotificationsFromAllAppsSafely().stream()
                .filter(notification -> notification.getPackageName().equals(getPackageName()))
                .forEach(notification -> groupToNotificationKeyMultimap.put(notification.getNotification().getGroup(), notification.getKey()));
        boolean isValid = dumbNotificationCounter.validateNotifications(groupToNotificationKeyMultimap);

        if (!isValid) {
            // TODO why would this happen outside of service being killed?
        }

        scheduleNotificationCounterCheck();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {

        for (final DismissedNotificationReactor reactor : dismissedNotificationReactors) {
            try {
                if (!reactor.interestedPackages().contains(statusBarNotification.getPackageName())) {
                    continue;
                }
                if (StatusBarNotificationExtractor.isSummary(statusBarNotification) && !reactor.isInterestInNotificationGroup()) {
                    continue;
                }

                Timber.d("Processing DismissedNotificationReactor [%s] for notification key [%s]",
                        reactor.getClass().getSimpleName(), statusBarNotification.getKey());
                final Reaction reaction = reactor.reactToDismissedNotification(statusBarNotification);

                if (reaction == Reaction.STOP_FURTHER_PROCESSING) {
                    Timber.d("DismissedNotificationReactor [%s] requested to [%s] after processing notification key [%s]",
                            reactor.getClass().getSimpleName(), reaction, statusBarNotification.getKey());
                    return;
                }
            } catch (Exception e) {
                Timber.e(e, "[ERROR] Failed to process DismissedNotificationReactor [%s]: error [%s] message [%s]",
                        reactor.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
            }
        }

        try {
            onNotificationRemovedUnsafe(statusBarNotification);
        } catch (final Exception e) {
            Timber.e(e, "[ERROR] onNotificationRemoved failed to handle exception [%s]", e.getMessage());
            if (debugModeProvider.isDebugMode()) {
                Toast.makeText(this, "[ERROR] LNS onNotificationRemoved", Toast.LENGTH_SHORT);
            }
        }
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();

        isListenerConnected = false;
        Timber.w("NotificationListenerService onListenerDisconnected");

        try {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        } catch (final Exception e) {
            Timber.w(e, "Errors thrown when unregistering listener: [%s]", e.getMessage());
        }
        Timber.d("Unregistered onSharedPreferenceChangeListener");

        isInitialized = false;
    }

    public void onNotificationRemovedUnsafe(final StatusBarNotification statusBarNotification) {
        super.onNotificationRemoved(statusBarNotification);

        if (shouldIgnoreNotification(statusBarNotification)) {
            return;
        }

        final LineNotification dismissedLineNotification = lineNotificationBuilder.from(statusBarNotification);

        if (LineNotification.CallState.INCOMING == dismissedLineNotification.getCallState() &&
                this.autoIncomingCallNotificationState != null) {
            this.autoIncomingCallNotificationState.cancel();
        }

        if (preferenceProvider.shouldAutoDismissLineNotificationSupportNotifications()) {
            dismissLineNotificationSupportNotifications(dismissedLineNotification.getChatId());
        }
    }

    private void dismissLineNotificationSupportNotifications(final String chatId) {
        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);

        final Set<Integer> notificationIdsToCancel = Arrays.stream(notificationManager.getActiveNotifications())
                // we're only clearing notifications from our package
                .filter(notification -> notification.getPackageName().equals(this.getPackageName()))
                // LINE only shows the last message for a chat, we'll dismiss all of the messages in the same chat ID
                .filter(notification -> NotificationExtractor.getLineNotificationSupportChatId(notification.getNotification()).isPresent())
                .filter(notification -> chatId.equals(NotificationExtractor.getLineNotificationSupportChatId(notification.getNotification()).get()))
                .map(notification -> notification.getId())
                .collect(Collectors.toSet());

        for (Integer notificationId : notificationIdsToCancel) {
            Timber.d("Cancelling notification: " + notificationId);
            notificationManager.cancel(notificationId.intValue());
        }
    }

}
