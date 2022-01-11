package com.mysticwind.linenotificationsupport.ui.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mysticwind.linenotificationsupport.ui.LocaleDao;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class LocaleChangeBroadcastReceiver extends BroadcastReceiver {

    @Inject
    LocaleDao localeDao;

    @Override
    public void onReceive(Context context, Intent intent) {
        localeDao.notifyLocaleChange();
        final String locale = localeDao.getLocale();
        Timber.i("Locale has been changed to %s", locale);
    }

}