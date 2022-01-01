package com.mysticwind.linenotificationsupport.conversationstarter.activity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MutableKeywordEntry {
        private String chatId;
        private String chatName;
        private String keyword;
        private boolean hasReplyAction;
}
