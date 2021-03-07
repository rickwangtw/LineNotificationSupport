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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
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
import com.mysticwind.linenotificationsupport.notification.DismissActionInjectorNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.notification.HistoryProvidingNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.notification.LinkActionInjectorNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.notification.MaxNotificationHandlingNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.notification.NotificationMergingNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.NotificationSentListener;
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
import com.mysticwind.linenotificationsupport.notification.reactor.ManageLineNotificationIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.Reaction;
import com.mysticwind.linenotificationsupport.notification.reactor.SameLineMessageIdFilterIncomingNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.SmartNotificationCounterNotificationReactor;
import com.mysticwind.linenotificationsupport.notification.reactor.SummaryNotificationPublisherNotificationReactor;
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
    private static final long EMPTY_LINE_NOTIFICATION_RETRY_TIMEOUT = 200L;
    private static final int EMPTY_LINE_NOTIFICATION_RETRY_COUNT = 10;
    private static final long VERIFY_NOTIFICATION_SENT_TIMEOUT = 1_000L;
    private static final long NOTIFICATION_COUNTER_CHECK_PERIOD = 60_000L;

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

    private static final Set<String> PREFERENCE_KEYS_THAT_TRIGGER_REBUILDING_NOTIFICATION_PUBLISHER = ImmutableSet.of(
            PreferenceProvider.MAX_NOTIFICATION_WORKAROUND_PREFERENCE_KEY,
            PreferenceProvider.USE_MESSAGE_SPLITTER_PREFERENCE_KEY,
            PreferenceProvider.SINGLE_NOTIFICATION_CONVERSATIONS_KEY
    );

    private final Handler handler = new Handler();
    private final SmartNotificationCounter smartNotificationCounter = new SmartNotificationCounter((int) getMaxNotificationsPerApp());
    private final DumbNotificationCounter dumbNotificationCounter = new DumbNotificationCounter((int) getMaxNotificationsPerApp());
    private final SlotAvailabilityChecker slotAvailabilityChecker = dumbNotificationCounter;

    private AutoIncomingCallNotificationState autoIncomingCallNotificationState;
    private NotificationPublisher notificationPublisher = NullNotificationPublisher.INSTANCE;
    private NotificationHistoryManager notificationHistoryManager = NullNotificationHistoryManager.INSTANCE;

    private ResendUnsentNotificationsNotificationSentListener resendUnsentNotificationsNotificationSentListener;

    private final List<IncomingNotificationReactor> incomingNotificationReactors = new ArrayList<>();
    private final List<DismissedNotificationReactor> dismissedNotificationReactors = new ArrayList<>();

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preferenceKey) {
            if (PREFERENCE_KEYS_THAT_TRIGGER_REBUILDING_NOTIFICATION_PUBLISHER.contains(preferenceKey)) {
                NotificationListenerService.this.notificationPublisher = buildNotificationPublisher();
            }
        }
    };

    private NotificationPublisher buildNotificationPublisher() {
        final boolean shouldExecuteMaxNotificationWorkaround =
                getPreferenceProvider().shouldExecuteMaxNotificationWorkaround();

        final List<NotificationSentListener> notificationSentListeners = new ArrayList<>();
        // don't enable this for single notification conversations just yet because we may still
        // exceed 25 chats
        if (shouldExecuteMaxNotificationWorkaround) {
            resendUnsentNotificationsNotificationSentListener = buildResendUnsentNotificationsNotificationSentListener();
            notificationSentListeners.add(resendUnsentNotificationsNotificationSentListener);
        } else {
            resendUnsentNotificationsNotificationSentListener = null;
        }

        NotificationPublisher notificationPublisher =
                new SimpleNotificationPublisher(this, getPackageName(), GROUP_ID_RESOLVER,
                        getPreferenceProvider(), notificationSentListeners);

        // this should come after HistoryProvidingNotificationPublisherDecorator as it changes the notification ID
        notificationPublisher =
                new DismissActionInjectorNotificationPublisherDecorator(
                        notificationPublisher, this);

        if (getPreferenceProvider().shouldUseSingleNotificationForConversations()) {
            // do this before LinkActionInjectorNotificationPublisherDecorator
            // so that link mutations are also persisted
            notificationPublisher = new HistoryProvidingNotificationPublisherDecorator(notificationPublisher);
        }

        notificationPublisher =
                new LinkActionInjectorNotificationPublisherDecorator(
                        notificationPublisher, this);

        if (shouldExecuteMaxNotificationWorkaround) {
            notificationPublisher = new MaxNotificationHandlingNotificationPublisherDecorator(
                    handler, notificationPublisher, slotAvailabilityChecker);
        }

        if (getPreferenceProvider().shouldUseMessageSplitter()) {
             notificationPublisher = new BigNotificationSplittingNotificationPublisherDecorator(
                    notificationPublisher,
                    getPreferenceProvider());
        }

        notificationPublisher = new NotificationMergingNotificationPublisherDecorator(notificationPublisher);

        return notificationPublisher;
    }

    private ResendUnsentNotificationsNotificationSentListener buildResendUnsentNotificationsNotificationSentListener() {
        return new ResendUnsentNotificationsNotificationSentListener(
                handler,
                new Supplier<NotificationPublisher>() {
                    @Override
                    public NotificationPublisher get() {
                        return notificationPublisher;
                    }
                });
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

        final SummaryNotificationPublisher summaryNotificationPublisher = new SummaryNotificationPublisher(
                this, (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE),
                getPackageName(), GROUP_ID_RESOLVER);
        final SummaryNotificationPublisherNotificationReactor summaryNotificationPublisherNotificationReactor =
                new SummaryNotificationPublisherNotificationReactor(getPackageName(), summaryNotificationPublisher);
        this.incomingNotificationReactors.add(summaryNotificationPublisherNotificationReactor);
        this.dismissedNotificationReactors.add(summaryNotificationPublisherNotificationReactor);

        this.incomingNotificationReactors.add(
                new ManageLineNotificationIncomingNotificationReactor(
                        getPreferenceProvider(),
                        handler,
                        () -> getActiveNotificationsFromAllAppsSafely(),
                        key -> cancelNotification(key)));

        this.incomingNotificationReactors.add(new SameLineMessageIdFilterIncomingNotificationReactor());

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

        scheduleNotificationCounterCheck();

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

        if (isNewMesssageWithoutContent(statusBarNotification)) {
            Timber.d("Detected potential new message without content: key [%s] title [%s] message [%s]",
                    statusBarNotification.getKey(), NotificationExtractor.getTitle(statusBarNotification.getNotification()),
                    statusBarNotification.getNotification().tickerText);
            // we should get a notification update for this message
        }

        sendNotification(statusBarNotification);
    }

    private boolean isNewMesssageWithoutContent(final StatusBarNotification statusBarNotification) {
        // There are notifications that will not have actions and don't need to retry.
        // For example: notifications of someone added to a chat
        if (StringUtils.isBlank(NotificationExtractor.getLineMessageId(statusBarNotification.getNotification()))) {
            return false;
        }
        return StringUtils.equals(LineNotificationBuilder.MESSAGE_CATEGORY, statusBarNotification.getNotification().category) &&
                statusBarNotification.getNotification().actions == null;
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
        final Multimap<String, String> groupToNotificationKeyMultimap = HashMultimap.create();
        getActiveNotificationsFromAllAppsSafely().stream()
                .filter(notification -> notification.getPackageName().equals(getPackageName()))
                .forEach(notification -> groupToNotificationKeyMultimap.put(notification.getNotification().getGroup(), notification.getKey()));
        dumbNotificationCounter.validateNotifications(groupToNotificationKeyMultimap);

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
        notificationPublisher.updateNotificationDismissed(statusBarNotification);
    }

}
