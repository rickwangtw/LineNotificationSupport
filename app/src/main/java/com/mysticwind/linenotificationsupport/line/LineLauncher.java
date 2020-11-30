package com.mysticwind.linenotificationsupport.line;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class LineLauncher {

    public static PendingIntent buildPendingIntent(final Context context) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://line.me/R/nv/chat"));
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

}
