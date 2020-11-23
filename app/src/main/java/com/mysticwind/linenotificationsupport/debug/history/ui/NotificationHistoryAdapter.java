package com.mysticwind.linenotificationsupport.debug.history.ui;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.mysticwind.linenotificationsupport.debug.history.dto.NotificationHistoryEntry;

import org.apache.commons.lang3.StringUtils;

public class NotificationHistoryAdapter extends ListAdapter<NotificationHistoryEntry, NotificationHistoryEntryViewHolder> {

    public NotificationHistoryAdapter(@NonNull DiffUtil.ItemCallback<NotificationHistoryEntry> diffCallback) {
        super(diffCallback);
    }

    @NonNull
    @Override
    public NotificationHistoryEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return NotificationHistoryEntryViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationHistoryEntryViewHolder holder, int position) {
        final NotificationHistoryEntry current = getItem(position);
        holder.bind(current, position);
    }


    public static class NotificationHistoryEntryDiff extends DiffUtil.ItemCallback<NotificationHistoryEntry> {

        @Override
        public boolean areItemsTheSame(@NonNull NotificationHistoryEntry oldItem, @NonNull NotificationHistoryEntry newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull NotificationHistoryEntry oldItem, @NonNull NotificationHistoryEntry newItem) {
            return oldItem.getId() == newItem.getId() &&
                    StringUtils.equals(oldItem.getRecordDateTime(), newItem.getRecordDateTime()) &&
                    StringUtils.equals(oldItem.getLineVersion(), newItem.getLineVersion()) &&
                    StringUtils.equals(oldItem.getNotification(), newItem.getNotification());
        }
    }

}
