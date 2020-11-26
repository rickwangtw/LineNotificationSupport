package com.mysticwind.linenotificationsupport;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.ImageNotificationPublisherAsyncTask;

import java.time.Instant;

public class MainActivity extends AppCompatActivity {

    private static final GroupIdResolver GROUP_ID_RESOLVER = new GroupIdResolver(1);

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
                sendNotification("Message: " + Instant.now().toString(), null);
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
                .build();
        new ImageNotificationPublisherAsyncTask(this, lineNotification, notificationId).execute();
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
}