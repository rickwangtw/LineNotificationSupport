package com.mysticwind.linenotificationsupport;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.notification.DismissActionInjectorNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.notification.HistoryProvidingNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.notification.LinkActionInjectorNotificationPublisherDecorator;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisherFactory;
import com.mysticwind.linenotificationsupport.notification.NullNotificationPublisher;
import com.mysticwind.linenotificationsupport.notification.SimpleNotificationPublisher;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;

import java.time.Instant;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final GroupIdResolver GROUP_ID_RESOLVER = new GroupIdResolver(1);

    private NotificationPublisher notificationPublisher = NullNotificationPublisher.INSTANCE;

    @Inject
    NotificationPublisherFactory notificationPublisherFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                sendNotification("Message: " + Instant.now().toString() + " https://www.google.com/search?q=" + randomNumber(), null);
            }
        });

        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                sendNotification("Message with big picture: " + Instant.now().toString(),
                        "https://stickershop.line-scdn.net/products/0/0/9/1917/android/stickers/37789.png");
                return true;
            }
        });
    }

    private int randomNumber() {
        return (int) (Math.random() * 100 % 10);
    }

    private PreferenceProvider getPreferenceProvider() {
        return new PreferenceProvider(PreferenceManager.getDefaultSharedPreferences(this));
    }

    private void sendNotification(final String message, final String url) {
        final String groupKey = "message-group";
        int notificationId = (int) (System.currentTimeMillis() / 1000);

        final Person sender = new Person.Builder()
                .setName("sender")
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher_round))
                .build();

        final long timestamp = Instant.now().toEpochMilli();
        LineNotification lineNotification = LineNotification.builder()
                .title("Title")
                .message(message)
                .lineStickerUrl(url)
                .chatId(groupKey)
                .sender(sender)
                .timestamp(timestamp)
                .icon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round))
                .action(buildRemoteInputAction())
                .build();
        notificationPublisherFactory.get().publishNotification(lineNotification, notificationId);
    }

    private Notification.Action buildRemoteInputAction() {
        final int requestCode = 1;
        final Intent messageReplyIntent = new Intent(this, MainActivity.class);
        final PendingIntent actionIntent =
                PendingIntent.getBroadcast(getApplicationContext(),
                        requestCode,
                        messageReplyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        return new Notification.Action.Builder(
                android.R.drawable.btn_default, "Reply", actionIntent)
                .addRemoteInput(
                        new RemoteInput.Builder("quick_reply")
                                .setLabel("Quick reply")
                                .build())
                .build();
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}