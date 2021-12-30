package com.mysticwind.linenotificationsupport.conversationstarter.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.conversationstarter.model.KeywordEntry;

class KeywordSettingViewHolder extends RecyclerView.ViewHolder {

    private final ImageView warningIcon;
    private final TextView chatNameTextView;
    private final TextView keywordTextView;

    private KeywordSettingViewHolder(View itemView) {
        super(itemView);
        warningIcon = itemView.findViewById(R.id.warning_icon);
        chatNameTextView = itemView.findViewById(R.id.chat_name_text_view);
        keywordTextView = itemView.findViewById(R.id.keyword_text_view);
    }

    public void bind(KeywordEntry keywordEntry) {
        warningIcon.setVisibility(shouldShowWarning(keywordEntry) ? View.VISIBLE : View.INVISIBLE);
        chatNameTextView.setText(keywordEntry.getChatName());
        keywordTextView.setText(keywordEntry.getKeyword().orElse("N/A"));
    }

    private boolean shouldShowWarning(KeywordEntry keywordEntry) {
        if (keywordEntry.getKeyword().isPresent() && !keywordEntry.isHasReplyAction()) {
            return true;
        }
        return false;
    }

    static KeywordSettingViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.keyword_setting_row_item, parent, false);
        return new KeywordSettingViewHolder(view);
    }

}