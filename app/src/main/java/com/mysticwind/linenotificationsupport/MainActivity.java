package com.mysticwind.linenotificationsupport;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.ImmutableMap;
import com.mysticwind.linenotificationsupport.utils.ImageNotificationPublisherAsyncTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static androidx.core.app.NotificationCompat.EXTRA_TEXT;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String CHANNEL_NAME = "LineNotificationSupport";
    private static final String CHANNEL_DESCRIPTION = "Republish Line notifications";
    public static final String CHANNEL_ID = "converted-jp.naver.line.android.notification.NewMessages";
    private static final int GROUP_ID = 0x80;
    private static final Map<String, Integer> groupKeyToGroupIdMap = ImmutableMap.of(
            "MessageGroup",GROUP_ID,
            "MessageGroup2", GROUP_ID + 1
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                sendNotification("MessageGroup", "Message: " + Instant.now().toString());
            }
        });

        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                sendNotification("MessageGroup2", "Group 2 Message: " + Instant.now().toString());
                return true;
            }
        });

        createNotificationChannel();
    }

    private void sendNotification(String groupKey, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        boolean shouldShowGroupNotification = false;
        List<CharSequence> currentNotificationMessages = new ArrayList<>();
        currentNotificationMessages.add(message);
        final int groupId = groupKeyToGroupIdMap.get(groupKey);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (StatusBarNotification sbn : notificationManager.getActiveNotifications()) {
                if (sbn.getId() != groupId && groupKey.equalsIgnoreCase(sbn.getNotification().getGroup())) {
                    CharSequence text = sbn.getNotification().extras.getCharSequence(EXTRA_TEXT);
                    currentNotificationMessages.add(text);
                    shouldShowGroupNotification = true;
                    break;
                }
            }
        }

        showSingleNotification(groupKey, message, shouldShowGroupNotification, currentNotificationMessages);
        if (shouldShowGroupNotification) {
            showGroupNotification(groupKey, currentNotificationMessages);
        }
    }

    private void showSingleNotification(String groupKey,
                                        String message,
                                        boolean shouldShowGroupNotification,
                                        List<CharSequence> currentNotificationMessages) {
        int notificationId = (int) (System.currentTimeMillis() / 1000);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        final Person sender = new Person.Builder().setName("sender").build();

        if (true) {
            final String url = "https://stickershop.line-scdn.net/products/0/0/9/1917/android/stickers/37789.png";
            new ImageNotificationPublisherAsyncTask(this, "Title", message,
                    url, groupKey, notificationId, shouldShowGroupNotification, currentNotificationMessages,
                    groupKeyToGroupIdMap.get(groupKey)).execute();
        } else {
            Notification singleNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Title")
                    .setContentText(message)
                    .setGroup(groupKey)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(notificationId, singleNotification);
        }
    }

    private void showGroupNotification(String groupKey, List<CharSequence> previousNotificationsTexts) {
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (CharSequence text: previousNotificationsTexts) {
            style.addLine(text);
        }
        int groupCount = previousNotificationsTexts.size() + 1;
        style.setSummaryText(groupCount + " new notifications");

        Notification groupNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setStyle(style)
                .setContentTitle("Group Title")
                .setContentText("Group Text")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .build();

        final int groupId = groupKeyToGroupIdMap.get(groupKey);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(groupId, groupNotification);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(CHANNEL_DESCRIPTION);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}