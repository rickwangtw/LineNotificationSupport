package com.mysticwind.linenotificationsupport.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.model.LineNotification;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

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

        final LineNotification linkActionInjectedLineNotification = lineNotification.toBuilder()
                .action(buildLinkAction(url.get()))
                .build();
        this.notificationPublisher.publishNotification(linkActionInjectedLineNotification, notificationId);
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
