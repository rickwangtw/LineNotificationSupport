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

import com.mysticwind.linenotificationsupport.localization.LocalizationHelper;
import com.mysticwind.linenotificationsupport.utils.ChatTitleAndSenderResolver;

import org.apache.commons.lang3.tuple.Pair;

public class LineNotificationBuilder {

    public static final String CALL_VIRTUAL_CHAT_ID = "call_virtual_chat_id";

    private static final String CALL_CATEGORY = "call";
    private static final String MISSED_CALL_TAG = "NOTIFICATION_TAG_MISSED_CALL";

    private final Context context;
    private final ChatTitleAndSenderResolver chatTitleAndSenderResolver;

    public LineNotificationBuilder(Context context, ChatTitleAndSenderResolver chatTitleAndSenderResolver) {
        this.context = context;
        this.chatTitleAndSenderResolver = chatTitleAndSenderResolver;
    }

    public LineNotification from(StatusBarNotification statusBarNotification) {
        final Pair<String, String> titleAndSender = chatTitleAndSenderResolver.resolveTitleAndSender(statusBarNotification);
        final String title = titleAndSender.getLeft();
        final String sender = titleAndSender.getRight();
        final Bitmap largeIconBitmap = getLargeIconBitmap(statusBarNotification);
        final Person senderPerson = buildPerson(sender, largeIconBitmap);
        final LineNotification.CallState callState = resolveCallState(statusBarNotification);

        final String message = getMessage(statusBarNotification);
        final String lineStickerUrl = getLineStickerUrl(statusBarNotification);
        return LineNotification.builder()
                .title(title)
                .message(message)
                .sender(senderPerson)
                .lineStickerUrl(lineStickerUrl)
                .chatId(resolveChatId(statusBarNotification, callState))
                .timestamp(statusBarNotification.getPostTime())
                .replyAction(extractReplyAction(statusBarNotification))
                .icon(largeIconBitmap)
                .callState(callState)
                .build();
    }

    private LineNotification.CallState resolveCallState(final StatusBarNotification statusBarNotification) {
        if (CALL_CATEGORY.equals(statusBarNotification.getNotification().category)) {
            return LineNotification.CallState.INCOMING;
        } else if (MISSED_CALL_TAG.equals(statusBarNotification.getTag())) {
            return LineNotification.CallState.MISSED_CALL;
        } else if (LocalizationHelper.isCallInProgressText(getMessage(statusBarNotification))) {
            return LineNotification.CallState.IN_A_CALL;
        }
        return null;
    }

    private String resolveChatId(final StatusBarNotification statusBarNotification, LineNotification.CallState callState) {
        if (callState != null) {
            return CALL_VIRTUAL_CHAT_ID;
        }
        return getChatId(statusBarNotification);
    }

    private String getChatId(final StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().extras.getString("line.chat.id");
    }

    private String getLineStickerUrl(final StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().extras.getString("line.sticker.url");
    }

    private Bitmap getLargeIconBitmap(StatusBarNotification statusBarNotification) {
        final Icon largeIcon = statusBarNotification.getNotification().getLargeIcon();
        if (largeIcon == null) {
            return null;
        }
        return convertDrawableToBitmap(largeIcon.loadDrawable(context));
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

    private Person buildPerson(String sender, Bitmap iconBitmap) {
        final IconCompat icon;
        if (iconBitmap == null) {
            icon = null;
        } else {
            icon = IconCompat.createWithBitmap(iconBitmap);
        }
        return new Person.Builder()
                .setName(sender)
                .setIcon(icon)
                .build();
    }

    private String getMessage(StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().extras.getString("android.text");
    }

    private Notification.Action extractReplyAction(StatusBarNotification notificationFromLine) {
        if (notificationFromLine.getNotification().actions == null) {
            return null;
        }
        if (notificationFromLine.getNotification().actions.length < 2) {
            return null;
        }
        Notification.Action secondAction = notificationFromLine.getNotification().actions[1];
        if (LocalizationHelper.isReplyActionText(secondAction.title.toString())) {
            return secondAction;
        }
        return null;
    }

}
