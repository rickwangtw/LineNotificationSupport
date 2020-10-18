package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mysticwind.linenotificationsupport.MainActivity;
import com.mysticwind.linenotificationsupport.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ImageNotificationPublisherAsyncTask extends AsyncTask<String, Void, Bitmap> {

    private static final String TAG = ImageNotificationPublisherAsyncTask.class.getSimpleName();

    private Context mContext;
    private String title, message, imageUrl, groupKey;
    private int notificationId;
    private boolean showGroupNotification;
    private List<CharSequence> currentNotificationMessages;
    private int groupId;

    public ImageNotificationPublisherAsyncTask(Context context, String title, String message, String imageUrl,
                                               String groupKey, int notificationId, boolean showGroupNotification,
                                               List<CharSequence> currentNotificationMessages, int groupId) {
        super();
        this.mContext = context;
        this.title = title;
        this.message = message;
        this.imageUrl = imageUrl;
        this.groupKey = groupKey;
        this.notificationId = notificationId;
        this.showGroupNotification = showGroupNotification;
        this.currentNotificationMessages = currentNotificationMessages;
        this.groupId = groupId;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        InputStream in;
        try {
            URL url = new URL(this.imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            in = connection.getInputStream();
            return BitmapFactory.decodeStream(in);
        } catch (final Exception e) {
            Log.e(TAG, String.format("Failed to download image %s: %s", imageUrl, e.getMessage()), e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(Bitmap downloadedImage) {
        super.onPostExecute(downloadedImage);

        Notification singleNotification = new NotificationCompat.Builder(mContext, MainActivity.CHANNEL_ID)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(downloadedImage))
                .setContentText(message)
                .setGroup(groupKey)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        notificationManager.notify(notificationId, singleNotification);

        if (showGroupNotification) {
            showGroupNotification();
        }
    }

    private void showGroupNotification() {
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (CharSequence text: currentNotificationMessages) {
            style.addLine(text);
        }
        int groupCount = currentNotificationMessages.size() + 1;
        style.setSummaryText(groupCount + " new notifications");

        Notification groupNotification = new NotificationCompat.Builder(mContext, MainActivity.CHANNEL_ID)
                .setStyle(style)
                .setContentTitle("Group Title")
                .setContentText("Group Text")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mContext);
        notificationManager.notify(groupId, groupNotification);
    }

}
