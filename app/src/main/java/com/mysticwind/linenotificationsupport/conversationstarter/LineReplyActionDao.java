package com.mysticwind.linenotificationsupport.conversationstarter;

import android.app.Notification;

import java.util.Optional;

public interface LineReplyActionDao {

    void saveLineReplyAction(String chatId, Notification.Action lineReplyAction);
    Optional<Notification.Action> getLineReplyAction(String chatId);

}
