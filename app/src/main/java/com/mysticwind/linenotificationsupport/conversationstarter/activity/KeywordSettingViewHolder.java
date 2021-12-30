package com.mysticwind.linenotificationsupport.conversationstarter.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.conversationstarter.model.KeywordEntry;

import org.apache.commons.lang3.StringUtils;

import java.util.function.BiConsumer;

class KeywordSettingViewHolder extends RecyclerView.ViewHolder {

    private final ImageView warningIcon;
    private final TextView chatNameTextView;
    private final EditText keywordEditText;

    private KeywordSettingViewHolder(View itemView) {
        super(itemView);
        warningIcon = itemView.findViewById(R.id.warning_icon);
        chatNameTextView = itemView.findViewById(R.id.chat_name_text_view);
        keywordEditText = itemView.findViewById(R.id.keyword_edit_text);
    }

    public void bind(final KeywordEntry keywordEntry, BiConsumer<String, String> chatIdAndKeywordUpdater) {
        warningIcon.setVisibility(shouldShowWarning(keywordEntry) ? View.VISIBLE : View.INVISIBLE);
        chatNameTextView.setText(keywordEntry.getChatName());
        keywordEditText.setText(keywordEntry.getKeyword().orElse(""));
        keywordEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                final EditText editText = (EditText) v;
                editText.setText(editText.getText().toString().trim());
                final String newKeyword = editText.getText().toString().trim();
                if (StringUtils.isNotBlank(newKeyword) && !newKeyword.equals(keywordEntry.getKeyword())) {
                    chatIdAndKeywordUpdater.accept(keywordEntry.getChatId(), newKeyword);
                }
            }
        });
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