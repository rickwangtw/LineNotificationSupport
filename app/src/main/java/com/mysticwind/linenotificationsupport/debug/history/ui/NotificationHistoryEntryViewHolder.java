package com.mysticwind.linenotificationsupport.debug.history.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.debug.history.dto.NotificationHistoryEntry;

public class NotificationHistoryEntryViewHolder extends RecyclerView.ViewHolder {

    private final TextView timestampTextView;
    private final TextView lineVersionTextView;
    private final TextView notificationEntryTextView;

    public NotificationHistoryEntryViewHolder(@NonNull View itemView) {
        super(itemView);
        timestampTextView = itemView.findViewById(R.id.timestamp_text_view);
        lineVersionTextView = itemView.findViewById(R.id.line_version_text_view);
        notificationEntryTextView = itemView.findViewById(R.id.notification_entry_text_view);
    }

    public void bind(NotificationHistoryEntry entry, int position) {
        timestampTextView.setText(entry.getRecordDateTime());
        lineVersionTextView.setText(entry.getLineVersion());
        notificationEntryTextView.setText(entry.getNotification());

        if (position % 2 == 1) {
            itemView.setBackgroundColor(Color.GRAY);
        } else {
            itemView.setBackgroundColor(Color.WHITE);
        }
    }

    static NotificationHistoryEntryViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_history_entry, parent, false);
        return new NotificationHistoryEntryViewHolder(view);
    }

}
