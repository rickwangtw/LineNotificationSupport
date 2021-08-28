package com.mysticwind.linenotificationsupport.reply;

import android.app.Notification;

public interface ReplyActionBuilder {

    Notification.Action buildReplyAction(final String chatId, final Notification.Action originalLineReplyAction);

}
