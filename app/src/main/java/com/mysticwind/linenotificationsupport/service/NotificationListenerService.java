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
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.room.Room;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
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
import com.mysticwind.linenotificationsupport.notification.BigNotificationSplittingNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.notification.MaxNotificationHandlingNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.NullNotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.ResendUnsentNotificationsNotificationSentListener;
import com.mysticwind.linenotificationsupport.notification.SimpleNotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.SlotAvailabilityChecker;
import com.mysticwind.linenotificationsupport.notification.SummaryNotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.impl.DumbNotificationCounter;
import com.mysticwind.linenotificationsupport.notification.impl.SmartNotificationCounter;
import com.mysticwind.linenotificationsupport.notification.reactor.DismissedNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.DumbNotificationCounterNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.IncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.SmartNotificationCounterNotificationReactor;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.persistence.AppDatabase;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.ChatTitleAndSenderResolver;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationPrinter;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import timber.log.Timber;

import static com.mysticwind.linenotificationsupport.line.Constants.LINE_PACKAGE_NAME;

public class NotificationListenerService
        extends android.service.notification.NotificationListenerService {

    private static final String GROUP_MESSAGE_GROUP_KEY = "NOTIFICATION_GROUP_MESSAGE";
    private static final long LINE_NOTIFICATION_DISMISS_RETRY_TIMEOUT = 500L;
    private static final long PRINT_LINE_NOTIFICATION_WAIT_TIME = 200L;
    private static final long EMPTY_LINE_NOTIFICATION_RETRY_TIMEOUT = 200L;
    private static final int EMPTY_LINE_NOTIFICATION_RETRY_COUNT = 10;
    private static final long VERIFY_NOTIFICATION_SENT_TIMEOUT = 1_000L;

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
    private final SmartNotificationCounter smartNotificationCounter = new SmartNotificationCounter((int) getMaxNotificationsPerApp());
    private final DumbNotificationCounter dumbNotificationCounter = new DumbNotificationCounter((int) getMaxNotificationsPerApp());
    private final SlotAvailabilityChecker slotAvailabilityChecker = dumbNotificationCounter;

    private AutoIncomingCallNotificationState autoIncomingCallNotificationState;
    private NotificationPublisher notificationPublisher = NullNotificationPublisher.INSTANCE;
    private NotificationHistoryManager notificationHistoryManager = NullNotificationHistoryManager.INSTANCE;

    private SummaryNotificationPublisher summaryNotificationPublisher;
    private ResendUnsentNotificationsNotificationSentListener resendUnsentNotificationsNotificationSentListener;

    private final List<IncomingNotificationReactor> incomingNotificationReactors = new ArrayList<>();
    private final List<DismissedNotificationReactor> dismissedNotificationReactors = new ArrayList<>();

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preferenceKey) {
            if (PreferenceProvider.MAX_NOTIFICATION_WORKAROUND_PREFERENCE_KEY.equals(preferenceKey)) {
                NotificationListenerService.this.notificationPublisher = buildNotificationPublisher();
            } else if (PreferenceProvider.USE_MESSAGE_SPLITTER_PREFERENCE_KEY.equals(preferenceKey)) {
                NotificationListenerService.this.notificationPublisher = buildNotificationPublisher();
            }
        }
    };

    private NotificationPublisher buildNotificationPublisher() {
        NotificationPublisher notificationPublisher =
                new SimpleNotificationPublisher(this, getPackageName(), GROUP_ID_RESOLVER,
                        getPreferenceProvider(), resendUnsentNotificationsNotificationSentListener);

        if (getPreferenceProvider().shouldExecuteMaxNotificationWorkaround()) {
            notificationPublisher = new MaxNotificationHandlingNotificationPublisherDecorator(
                    handler, notificationPublisher, slotAvailabilityChecker);
        }

        if (getPreferenceProvider().shouldUseMessageSplitter()) {
             notificationPublisher = new BigNotificationSplittingNotificationPublisherDecorator(
                    notificationPublisher,
                    getPreferenceProvider());
        }

        return notificationPublisher;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO remove this after testing the stability of the dumb version
        final SmartNotificationCounterNotificationReactor smartNotificationCounterNotificationReactor =
                new SmartNotificationCounterNotificationReactor(getPackageName(), smartNotificationCounter);
        this.incomingNotificationReactors.add(smartNotificationCounterNotificationReactor);
        this.dismissedNotificationReactors.add(smartNotificationCounterNotificationReactor);

        final DumbNotificationCounterNotificationReactor dumbNotificationCounterNotificationReactor =
                new DumbNotificationCounterNotificationReactor(getPackageName(), dumbNotificationCounter);
        this.incomingNotificationReactors.add(dumbNotificationCounterNotificationReactor);
        this.dismissedNotificationReactors.add(dumbNotificationCounterNotificationReactor);

        this.resendUnsentNotificationsNotificationSentListener = new ResendUnsentNotificationsNotificationSentListener(
                handler,
                new Supplier<NotificationPublisher>() {
                    @Override
                    public NotificationPublisher get() {
                        return notificationPublisher;
                    }
                });

        this.notificationPublisher = buildNotificationPublisher();

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
        this.incomingNotificationReactors.clear();
        this.dismissedNotificationReactors.clear();

        this.notificationPublisher = NullNotificationPublisher.INSTANCE;

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

        this.notificationHistoryManager = NullNotificationHistoryManager.INSTANCE;
        this.summaryNotificationPublisher = null;

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
                reactor.reactToIncomingNotification(statusBarNotification);

            } catch (Exception e) {
                Timber.e(e, "[ERROR] Failed to process IncomingNotificationReactor [%s]: error [%s] message [%s]",
                        reactor.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
            }
        }

        try {
            onNotificationPostedUnsafe(statusBarNotification);
        } catch (final Exception e) {
            Timber.e(e, "[ERROR] onNotificationPosted failed to handle exception [%s]", e.getMessage());
            if (DEBUG_MODE_PROVIDER.isDebugMode()) {
                Toast.makeText(this, "[ERROR] LNS onNotificationPosted", Toast.LENGTH_SHORT);
            }
        }
    }

    public void onNotificationPostedUnsafe(final StatusBarNotification statusBarNotification) {
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

        if (StringUtils.equals(LineNotificationBuilder.MESSAGE_CATEGORY, statusBarNotification.getNotification().category) &&
                statusBarNotification.getNotification().actions == null) {
            Timber.d("Detected potential new message without content: key [%s] title [%s] message [%s]",
                    statusBarNotification.getKey(), NotificationExtractor.getTitle(statusBarNotification.getNotification()),
                    statusBarNotification.getNotification().tickerText);
            scheduleRetryEmptyNotification(statusBarNotification, 0);
            // early return;
            return;
        }

        sendNotification(statusBarNotification);

        if (getPreferenceProvider().shouldManageLineMessageNotifications()) {
            dismissLineNotification(statusBarNotification);

            // there are situations where LINE messages are not dismissed, do this again
            handler.postDelayed(
                    () -> {
                        Timber.d("Retry dismissing LINE notifications again: key [%s] message [%s]",
                                statusBarNotification.getKey(), statusBarNotification.getNotification().tickerText);
                        dismissLineNotification(statusBarNotification);
                    },
                    LINE_NOTIFICATION_DISMISS_RETRY_TIMEOUT);
        }
    }

    private void scheduleRetryEmptyNotification(final StatusBarNotification statusBarNotification, final int retryCount) {
        final int nextRetryCount = retryCount + 1;
        Timber.d("Schedule retrying empty notification [%s] retryCount [%d]",
                statusBarNotification.getKey(), nextRetryCount);
        handler.postDelayed(
                () -> retryEmptyNotification(statusBarNotification, nextRetryCount),
                EMPTY_LINE_NOTIFICATION_RETRY_TIMEOUT);
    }

    private void retryEmptyNotification(final StatusBarNotification previousStatusBarNotification, final int retryCount) {
        // stop condition
        if (retryCount > EMPTY_LINE_NOTIFICATION_RETRY_COUNT) {
            // TODO this is obviously a workaround - we should have extracted the method out instead
            Timber.d("Used up all retries [%d] for key [%s]", retryCount, previousStatusBarNotification.getKey());
            previousStatusBarNotification.getNotification().actions = new Notification.Action[]{};
            onNotificationPosted(previousStatusBarNotification);
            return;
        }
        // check if the content is updated
        final Optional<StatusBarNotification> currentStatusBarNotification =
                getActiveNotificationsFromAllAppsSafely().stream()
                        .filter(notification -> StringUtils.equals(previousStatusBarNotification.getKey(), notification.getKey()))
                        .findFirst();

        // if the notification is already dismissed or replaced
        if (!currentStatusBarNotification.isPresent()) {
            Timber.d("Notification (key [%s]) no longer present", previousStatusBarNotification.getKey());
            // TODO this is obviously a workaround - we should have extracted the method out instead
            previousStatusBarNotification.getNotification().actions = new Notification.Action[]{};
            onNotificationPosted(previousStatusBarNotification);
            return;
        }

        NOTIFICATION_PRINTER.print(
                String.format("Re-fetched status bar notification [%d] [%s]", retryCount, previousStatusBarNotification.getKey()),
                currentStatusBarNotification.get());

        if (currentStatusBarNotification.get().getNotification().actions != null) {
            Timber.d("Notification (key [%s]) identified with update: message [%s]",
                    currentStatusBarNotification.get().getKey(), currentStatusBarNotification.get().getNotification().tickerText);
            // TODO remove this debug information
            if (DEBUG_MODE_PROVIDER.isDebugMode()) {
                Toast.makeText(this, "Successfully re-fetched notification " + currentStatusBarNotification.get().getKey(), Toast.LENGTH_SHORT)
                        .show();
            }

            onNotificationPosted(currentStatusBarNotification.get());
            return;
        }

        // need to retry again
        scheduleRetryEmptyNotification(previousStatusBarNotification /* use the original status bar notification here */, retryCount);
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

        if (actionAdjustedLineNotification.getCallState() == null) {
            return;
        }

        // deal with auto notifications for calls
        if (actionAdjustedLineNotification.getCallState() == LineNotification.CallState.INCOMING) {
            if (this.autoIncomingCallNotificationState != null) {
                this.autoIncomingCallNotificationState.cancel();
            }
            this.autoIncomingCallNotificationState = AutoIncomingCallNotificationState.builder()
                    .lineNotification(actionAdjustedLineNotification)
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

    private void dismissLineNotification(final StatusBarNotification statusBarNotification) {
        // we only dismiss notifications that are in the message category
        if (!LineNotificationBuilder.MESSAGE_CATEGORY.equals(statusBarNotification.getNotification().category)) {
            Timber.d("LINE notification not message category but [%s]: [%s]",
                    statusBarNotification.getNotification().category, statusBarNotification.getNotification().tickerText);
            return;
        }

        final Optional<String> summaryKey = findLineNotificationSummary(statusBarNotification.getNotification().getGroup());
        summaryKey.ifPresent(
                key -> {
                    Timber.d("Cancelling LINE summary: [%s]", key);
                    cancelNotification(key);
                }
        );

        Timber.d("Dismiss LINE notification: key[%s] tag[%s] id[%d]",
                statusBarNotification.getKey(), statusBarNotification.getTag(), statusBarNotification.getId());
        cancelNotification(statusBarNotification.getKey());

        handler.postDelayed(
                () -> printLineNotifications(statusBarNotification.getNotification().getGroup()),
                PRINT_LINE_NOTIFICATION_WAIT_TIME);
    }

    private Optional<String> findLineNotificationSummary(String group) {
        return getActiveNotificationsFromAllAppsSafely().stream()
                .filter(notification -> notification.getPackageName().equals(LINE_PACKAGE_NAME))
                .peek(notification -> Timber.d("LINE notification key [%s] category [%s] group [%s] isSummary [%s] title [%s] message [%s]",
                        notification.getKey(), notification.getNotification().category,
                        notification.getNotification().getGroup(),
                        StatusBarNotificationExtractor.isSummary(notification),
                        NotificationExtractor.getTitle(notification.getNotification()),
                        NotificationExtractor.getMessage(notification.getNotification())))
                .filter(notification -> LineNotificationBuilder.MESSAGE_CATEGORY.equals(notification.getNotification().category))
                .filter(notification -> StatusBarNotificationExtractor.isSummary(notification))
                .filter(notification -> StringUtils.equals(group, notification.getNotification().getGroup()))
                .map(notification -> notification.getKey())
                .findFirst();
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

    private void printLineNotifications(final String groupThatShouldBeDismissed) {
        getActiveNotificationsFromAllAppsSafely().stream()
                .filter(notification -> notification.getPackageName().equals(LINE_PACKAGE_NAME))
                .forEach(notification -> {
                    Timber.w("%sPrint LINE notification that are not dismissed key [%s] category [%s] group [%s] isSummary [%s] isClearable [%s] title [%s] message [%s]",
                            StringUtils.equals(notification.getNotification().getGroup(), groupThatShouldBeDismissed) ? "[SHOULD_DISMISS] " : "",
                            notification.getKey(), notification.getNotification().category,
                            notification.getNotification().getGroup(),
                            StatusBarNotificationExtractor.isSummary(notification),
                            notification.isClearable(),
                            NotificationExtractor.getTitle(notification.getNotification()),
                            NotificationExtractor.getMessage(notification.getNotification()));
                });
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
                reactor.reactToDismissedNotification(statusBarNotification);

            } catch (Exception e) {
                Timber.e(e, "[ERROR] Failed to process DismissedNotificationReactor [%s]: error [%s] message [%s]",
                        reactor.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getMessage());
            }
        }

        try {
            onNotificationRemovedUnsafe(statusBarNotification);
        } catch (final Exception e) {
            Timber.e(e, "[ERROR] onNotificationRemoved failed to handle exception [%s]", e.getMessage());
            if (DEBUG_MODE_PROVIDER.isDebugMode()) {
                Toast.makeText(this, "[ERROR] LNS onNotificationRemoved", Toast.LENGTH_SHORT);
            }
        }
    }

    public void onNotificationRemovedUnsafe(final StatusBarNotification statusBarNotification) {
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

        if (getPreferenceProvider().shouldAutoDismissLineNotificationSupportNotifications()) {
            dismissLineNotificationSupportNotifications(dismissedLineNotification.getChatId());
        }
    }

    private PreferenceProvider getPreferenceProvider() {
        return new PreferenceProvider(PreferenceManager.getDefaultSharedPreferences(this));
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
        if (summaryNotificationPublisher != null) {
            summaryNotificationPublisher.updateSummaryWhenNotificationsPublished(statusBarNotification.getNotification().getGroup());
        }
        if (resendUnsentNotificationsNotificationSentListener != null) {
            resendUnsentNotificationsNotificationSentListener.notificationReceived(statusBarNotification.getId());
        }
    }

    private void handleSelfNotificationDismissed(StatusBarNotification statusBarNotification) {
        if (!StringUtils.equals(statusBarNotification.getPackageName(), getPackageName())) {
            return;
        }
        if (StatusBarNotificationExtractor.isSummary(statusBarNotification)) {
            return;
        }
        if (summaryNotificationPublisher != null) {
            summaryNotificationPublisher.updateSummaryWhenNotificationsDismissed(statusBarNotification.getNotification().getGroup());
        }
        notificationPublisher.updateNotificationDismissed(statusBarNotification);
    }

}
