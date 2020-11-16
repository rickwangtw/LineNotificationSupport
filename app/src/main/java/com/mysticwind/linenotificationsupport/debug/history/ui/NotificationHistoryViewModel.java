package com.mysticwind.linenotificationsupport.debug.history.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.room.Room;

import com.mysticwind.linenotificationsupport.debug.history.dto.NotificationHistoryEntry;
import com.mysticwind.linenotificationsupport.debug.history.manager.impl.RoomNotificationHistoryManager;
import com.mysticwind.linenotificationsupport.persistence.AppDatabase;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationPrinter;

import java.util.List;

public class NotificationHistoryViewModel extends AndroidViewModel {

    private static final StatusBarNotificationPrinter NOTIFICATION_PRINTER = new StatusBarNotificationPrinter();

    private final RoomNotificationHistoryManager notificationHistoryManager;

    private final LiveData<List<NotificationHistoryEntry>> notificationHistory;

    public NotificationHistoryViewModel(@NonNull Application application) {
        super(application);

        final AppDatabase appDatabase = Room.databaseBuilder(application.getApplicationContext(),
                AppDatabase.class, "database").build();

        this.notificationHistoryManager = new RoomNotificationHistoryManager(appDatabase, NOTIFICATION_PRINTER);
        this.notificationHistory = notificationHistoryManager.getHistory();
    }

    public LiveData<List<NotificationHistoryEntry>> getNotificationHistory() {
        return notificationHistory;
    }

}
