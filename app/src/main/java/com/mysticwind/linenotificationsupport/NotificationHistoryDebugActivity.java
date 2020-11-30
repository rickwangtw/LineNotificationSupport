package com.mysticwind.linenotificationsupport;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mysticwind.linenotificationsupport.debug.history.ui.NotificationHistoryAdapter;
import com.mysticwind.linenotificationsupport.debug.history.ui.NotificationHistoryViewModel;

public class NotificationHistoryDebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_notification_history_view);

        final RecyclerView recyclerView = findViewById(R.id.notification_history_recyclerview);
        final NotificationHistoryAdapter adapter = new NotificationHistoryAdapter(new NotificationHistoryAdapter.NotificationHistoryEntryDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        final NotificationHistoryViewModel notificationHistoryViewModel =
                new ViewModelProvider(this).get(NotificationHistoryViewModel.class);

        notificationHistoryViewModel.getNotificationHistory().observe(this,
                notificationHistory -> adapter.submitList(notificationHistory)
        );

    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}