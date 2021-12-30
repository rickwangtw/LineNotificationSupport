package com.mysticwind.linenotificationsupport.conversationstarter.activity;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.mysticwind.linenotificationsupport.conversationstarter.model.KeywordEntry;

import java.util.Objects;
import java.util.function.BiConsumer;

public class KeywordEntryListAdapter extends ListAdapter<KeywordEntry, KeywordSettingViewHolder> {

    private final BiConsumer<String, String> chatIdAndKeywordUpdater;

    public KeywordEntryListAdapter(@NonNull DiffUtil.ItemCallback<KeywordEntry> diffCallback, BiConsumer<String, String> chatIdAndKeywordUpdater) {
        super(diffCallback);
        this.chatIdAndKeywordUpdater = Objects.requireNonNull(chatIdAndKeywordUpdater);
    }

    @Override
    public KeywordSettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return KeywordSettingViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(KeywordSettingViewHolder holder, int position) {
        KeywordEntry current = getItem(position);
        holder.bind(current, chatIdAndKeywordUpdater);
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