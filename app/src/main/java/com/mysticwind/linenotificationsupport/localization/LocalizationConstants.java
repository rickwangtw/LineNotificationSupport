package com.mysticwind.linenotificationsupport.localization;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class LocalizationConstants {

    public static final Set<String> CALL_IN_PROGRESS_TEXTS = ImmutableSet.of(
            "LINE call in progress", // en-US
            "LINE通話中", // zh-TW
            "正在进行LINE通话", // zh-CN
            "LINE通話中…" // jp-JP
    );

    public static final Set<String> REPLY_ACTION_TEXTS = ImmutableSet.of(
            "Reply", // en-US
            "回覆", // zh-TW
            "回复", // zh-CN
            "返信" // jp-JP
    );

}
