package com.mysticwind.linenotificationsupport.persistence.chatname.dto;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(tableName = "group_chat_names", indices = {@Index("chat_id")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChatNameEntry {

    @PrimaryKey
    @ColumnInfo(name = "chat_id")
    @NonNull
    public String chatId;

    @ColumnInfo(name = "chat_group_name")
    public String chatGroupName;

    @ColumnInfo(name = "created_at")
    public long createdAtTimestamp;

    @ColumnInfo(name = "updated_at")
    public long updatedAtTimestamp;

}
