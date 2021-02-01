package com.mysticwind.linenotificationsupport.line;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public class LineLauncher {

    private static final Set<String> NOT_CHAT_IDS = ImmutableSet.of(
            LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID,
            LineNotificationBuilder.DEFAULT_CHAT_ID
    );

    public static PendingIntent buildPendingIntent(final Context context, final String chatId) {
        final Intent intent;
        if (isChatId(chatId)) {
            // Credit for launching LINE specific chat: https://www.dcard.tw/f/3c/p/227637855/b/78
            intent = new Intent();
            intent.setComponent(new ComponentName("jp.naver.line.android",
                    "jp.naver.line.android.activity.shortcut.ShortcutLauncherActivity"));
            intent.putExtra("shortcutType", "chatmid");
            intent.putExtra("shortcutTargetId", chatId);
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://line.me/R/nv/chat"));
        }

        return PendingIntent.getActivity(context, 0, intent, 0x10200000 /* what does these flags mean */);
    }

    private static boolean isChatId(String chatId) {
        if (StringUtils.isBlank(chatId)) {
            return false;
        } else if (NOT_CHAT_IDS.contains(chatId)) {
            return false;
        }
        return true;
    }

}
