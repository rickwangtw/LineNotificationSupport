package com.mysticwind.linenotificationsupport.debug.history.dto;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(tableName = "notification_history",
        indices = {@Index("record_date_time")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistoryEntry {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "record_date_time")
    public String recordDateTime;

    @ColumnInfo(name = "line_version")
    public String lineVersion;

    @ColumnInfo(name = "notification")
    public String notification;

}
