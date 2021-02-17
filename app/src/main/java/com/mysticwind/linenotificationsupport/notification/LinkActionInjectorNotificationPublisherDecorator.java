package com.mysticwind.linenotificationsupport.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import timber.log.Timber;

public class LinkActionInjectorNotificationPublisherDecorator implements NotificationPublisher {

    private final NotificationPublisher notificationPublisher;
    private final Context context;

    public LinkActionInjectorNotificationPublisherDecorator(NotificationPublisher notificationPublisher, Context context) {
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher);
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public void publishNotification(LineNotification lineNotification, int notificationId) {
        final Optional<String> url = findUrl(lineNotification.getMessage());
        if (!url.isPresent()) {
            this.notificationPublisher.publishNotification(lineNotification, notificationId);
            return;
        }

        asyncFetchTitleAndPublish(lineNotification, notificationId, url.get());
    }

    private void asyncFetchTitleAndPublish(final LineNotification lineNotification,
                                           final int notificationId,
                                           final String url) {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                Optional<String> title = getTitle(url);
                return title.orElse(null);
            }

            @Override
            protected void onPostExecute(String title) {
                final LineNotification linkActionInjectedLineNotification = lineNotification.toBuilder()
                        .action(buildLinkAction(url))
                        .message(injectUrlTitle(lineNotification.getMessage(), url, title))
                        .build();
                notificationPublisher.publishNotification(linkActionInjectedLineNotification, notificationId);
            }
        }.execute();
    }

    private Optional<String> findUrl(final String message) {
        final Optional<Integer> httpIndex = getHttpIndex(message);
        if (!httpIndex.isPresent()) {
            return Optional.empty();
        }
        final String messageStartingWithHttp = message.substring(httpIndex.get().intValue());

        // separate input by spaces ( URLs don't have spaces )
        final String[] parts = messageStartingWithHttp.split("\\s+");

        // Attempt to convert each item into an URL.
        for (String item : parts) {
            if (isUrl(item)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private Optional<Integer> getHttpIndex(final String message) {
        int httpIndex = message.indexOf("http");
        if (httpIndex < 0) {
            return Optional.empty();
        } else {
            return Optional.of(httpIndex);
        }
    }

    private boolean isUrl(final String string) {
        try {
            new URL(string);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private Notification.Action buildLinkAction(final String url) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));

        final PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final String buttonText = context.getString(R.string.link_button_text);
        return new Notification.Action.Builder(android.R.drawable.btn_default, buttonText, pendingIntent)
                .build();
    }

    // TODO move this to a separate decorator/reactor
    // TODO support injecting when there are multiple URLs
    private String injectUrlTitle(final String message, final String url, final String title) {
        if (title == null) {
            return message;
        }
        final int endIndex = message.indexOf(url) + url.length();
        final String firstPart = message.substring(0, endIndex);
        final String lastPart = message.substring(firstPart.length());
        return String.format("%s (%s) %s", firstPart, title, lastPart);
    }

    private Optional<String> getTitle(String url) {
        try {
            final Document document = Jsoup.connect(url).get();
            return Optional.ofNullable(document.title());
        } catch (Exception e) {
            Timber.e(e, "Failed to extract title from url [%s]: [%s]", url, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void republishNotification(LineNotification lineNotification, int notificationId) {
        // do nothing as the action should have been injected previously through publishNotification()
        this.notificationPublisher.republishNotification(lineNotification, notificationId);
    }

    @Override
    public void updateNotificationDismissed(StatusBarNotification statusBarNotification) {
        // do nothing
        this.notificationPublisher.updateNotificationDismissed(statusBarNotification);
    }
}
