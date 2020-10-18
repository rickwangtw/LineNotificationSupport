package com.mysticwind.linenotificationsupport.model;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.notification.StatusBarNotification;

import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import org.apache.commons.lang3.StringUtils;

public class LineNotificationBuilder {

    private final Context context;

    public LineNotificationBuilder(Context context) {
        this.context = context;
    }

    public LineNotification from(StatusBarNotification statusBarNotification) {
        // individual: android.title is the sender
        // group chat: android.title is "group title：sender", android.conversationTitle is group title
        // chat with multi-folks: android.title is also the sender, no way to differentiate between individual and multi-folks :(

        final String title;
        final String sender;
        if (isChatGroup(statusBarNotification)) {
            title = getGroupChatTitle(statusBarNotification);
            final String androidTitle = getAndroidTitle(statusBarNotification);
            sender = androidTitle.replace(title + "：", "");
        } else {
            title = getAndroidTitle(statusBarNotification);
            sender = getAndroidTitle(statusBarNotification);
        }

        final Icon largeIcon = statusBarNotification.getNotification().getLargeIcon();
        final Bitmap largeIconBitmap = convertDrawableToBitmap(largeIcon.loadDrawable(context));

        final String message = statusBarNotification.getNotification().extras.getString("android.text");
        final String lineStickerUrl = getLineStickerUrl(statusBarNotification);
        return LineNotification.builder()
                .title(title)
                .message(message)
                .sender(new Person.Builder()
                        .setName(sender)
                        .setIcon(IconCompat.createWithBitmap(largeIconBitmap))
                        .build())
                .lineStickerUrl(lineStickerUrl)
                .chatId(getChatId(statusBarNotification))
                .timestamp(statusBarNotification.getPostTime())
                .replyAction(extractReplyAction(statusBarNotification))
                .icon(largeIconBitmap)
                .build();
    }

    private boolean isChatGroup(final StatusBarNotification statusBarNotification) {
        final String title = statusBarNotification.getNotification().extras.getString("android.conversationTitle");
        return StringUtils.isNotBlank(title);
    }

    private String getGroupChatTitle(final StatusBarNotification statusBarNotification) {
        // chat groups will have a conversationTitle (but not groups of people)
        return statusBarNotification.getNotification().extras.getString("android.conversationTitle");
    }

    private String getAndroidTitle(final StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().extras.getString("android.title");
    }

    private String getChatId(final StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().extras.getString("line.chat.id");
    }

    private String getLineStickerUrl(final StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().extras.getString("line.sticker.url");
    }

    private Bitmap convertDrawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        final Bitmap bitmap;
        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private Notification.Action extractReplyAction(StatusBarNotification notificationFromLine) {
        if (notificationFromLine.getNotification().actions == null) {
            return null;
        }
        if (notificationFromLine.getNotification().actions.length < 2) {
            return null;
        }
        Notification.Action secondAction = notificationFromLine.getNotification().actions[1];
        // TODO what about other languages? should extract from Line apk?
        if ("回覆".equals(secondAction.title)) {
            return secondAction;
        }
        return null;
    }

}
