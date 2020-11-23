package com.mysticwind.linenotificationsupport.debug.history.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mysticwind.linenotificationsupport.debug.history.dto.NotificationHistoryEntry;

import java.util.List;

@Dao
public interface LineNotificationHistoryDao {

    @Query("SELECT * FROM notification_history ORDER BY record_date_time DESC")
    LiveData<List<NotificationHistoryEntry>> getAllEntries();

    @Query("SELECT * FROM notification_history WHERE record_date_time BETWEEN :recordDateTimeStart AND :recordDateTimeEnd ORDER BY record_date_time DESC")
    List<NotificationHistoryEntry> getEntriesBetweenRecordDateTimes(String recordDateTimeStart, String recordDateTimeEnd);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(NotificationHistoryEntry entry);

    @Delete
    void delete(NotificationHistoryEntry entry);

    @Query("DELETE FROM notification_history")
    void deleteAllEntries();

}
