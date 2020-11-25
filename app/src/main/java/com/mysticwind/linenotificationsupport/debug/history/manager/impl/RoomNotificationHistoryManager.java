package com.mysticwind.linenotificationsupport.debug.history.manager.impl;

import android.os.AsyncTask;
import android.service.notification.StatusBarNotification;

import androidx.lifecycle.LiveData;

import com.mysticwind.linenotificationsupport.debug.history.dto.NotificationHistoryEntry;
import com.mysticwind.linenotificationsupport.debug.history.manager.NotificationHistoryManager;
import com.mysticwind.linenotificationsupport.persistence.AppDatabase;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationPrinter;

import java.time.Instant;
import java.util.List;

import timber.log.Timber;

public class RoomNotificationHistoryManager implements NotificationHistoryManager {

    private final AppDatabase appDatabase;
    private final StatusBarNotificationPrinter statusBarNotificationPrinter;

    public RoomNotificationHistoryManager(final AppDatabase appDatabase,
                                          final StatusBarNotificationPrinter statusBarNotificationPrinter) {
        this.appDatabase = appDatabase;
        this.statusBarNotificationPrinter = statusBarNotificationPrinter;
    }

    @Override
    public void record(final StatusBarNotification statusBarNotification, final String lineVersion) {
        final NotificationHistoryEntry entry = buildEntry(statusBarNotification, lineVersion);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    final long id = appDatabase.lineNotificationHistoryDao().insert(entry);
                    entry.setId(id);
                    Timber.i("Recorded entry with ID: " + entry.getId());
                } catch (Exception e) {
                    Timber.e(e, "Error recording notification: " + e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    private NotificationHistoryEntry buildEntry(final StatusBarNotification notification,
                                                final String lineVersion) {
        return NotificationHistoryEntry.builder()
                .notification(statusBarNotificationPrinter.toString(notification))
                .lineVersion(lineVersion)
                .recordDateTime(Instant.now().toString())
                .build();
    }

    @Override
    public LiveData<List<NotificationHistoryEntry>> getHistory() {
        return appDatabase.lineNotificationHistoryDao().getAllEntries();
    }

}
