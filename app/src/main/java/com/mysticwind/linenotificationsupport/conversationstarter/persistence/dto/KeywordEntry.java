package com.mysticwind.linenotificationsupport.conversationstarter.persistence.dto;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(tableName = "chat_id_keywords", indices = {@Index("chat_id")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordEntry {

    @PrimaryKey
    @ColumnInfo(name = "chat_id")
    @NonNull
    public String chatId;

    @ColumnInfo(name = "keyword")
    public String keyword;

    @ColumnInfo(name = "created_at")
    public long createdAtTimestamp;

    @ColumnInfo(name = "updated_at")
    public long updatedAtTimestamp;

}
