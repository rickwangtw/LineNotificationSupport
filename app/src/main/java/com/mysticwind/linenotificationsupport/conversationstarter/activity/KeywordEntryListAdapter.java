package com.mysticwind.linenotificationsupport.conversationstarter.activity;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.mysticwind.linenotificationsupport.conversationstarter.model.KeywordEntry;

public class KeywordEntryListAdapter extends ListAdapter<KeywordEntry, KeywordSettingViewHolder> {

    public KeywordEntryListAdapter(@NonNull DiffUtil.ItemCallback<KeywordEntry> diffCallback) {
        super(diffCallback);
    }

    @Override
    public KeywordSettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return KeywordSettingViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(KeywordSettingViewHolder holder, int position) {
        KeywordEntry current = getItem(position);
        holder.bind(current.getChatId(), current.getKeyword().orElse("N/A"));
    }

    static class KeywordEntryDiff extends DiffUtil.ItemCallback<KeywordEntry> {

        @Override
        public boolean areItemsTheSame(@NonNull KeywordEntry oldItem, @NonNull KeywordEntry newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull KeywordEntry oldItem, @NonNull KeywordEntry newItem) {
            return oldItem.getChatId().equals(newItem.getChatId());
        }
    }

}