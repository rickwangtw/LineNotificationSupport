package com.mysticwind.linenotificationsupport.conversationstarter.activity;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.mysticwind.linenotificationsupport.conversationstarter.ChatKeywordManager;

public class KeywordEntryListAdapter extends ListAdapter<ChatKeywordManager.KeywordEntry, KeywordSettingViewHolder> {

    public KeywordEntryListAdapter(@NonNull DiffUtil.ItemCallback<ChatKeywordManager.KeywordEntry> diffCallback) {
        super(diffCallback);
    }

    @Override
    public KeywordSettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return KeywordSettingViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(KeywordSettingViewHolder holder, int position) {
        ChatKeywordManager.KeywordEntry current = getItem(position);
        holder.bind(current.getChatId(), current.getKeyword());
    }

    static class KeywordEntryDiff extends DiffUtil.ItemCallback<ChatKeywordManager.KeywordEntry> {

        @Override
        public boolean areItemsTheSame(@NonNull ChatKeywordManager.KeywordEntry oldItem, @NonNull ChatKeywordManager.KeywordEntry newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatKeywordManager.KeywordEntry oldItem, @NonNull ChatKeywordManager.KeywordEntry newItem) {
            return oldItem.getChatId().equals(newItem.getChatId());
        }
    }

}