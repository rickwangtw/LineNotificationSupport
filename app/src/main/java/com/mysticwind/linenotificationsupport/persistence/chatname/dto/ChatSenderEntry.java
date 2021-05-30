package com.mysticwind.linenotificationsupport.persistence.chatname.dto;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(tableName = "chat_senders", indices = {@Index("chat_id")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSenderEntry {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "chat_id")
    public String chatId;

    @ColumnInfo(name = "sender")
    public String sender;

    @ColumnInfo(name = "created_at")
    public long createdAtTimestamp;

    @ColumnInfo(name = "updated_at")
    public long updatedAtTimestamp;

}
