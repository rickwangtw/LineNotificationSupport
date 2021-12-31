package com.mysticwind.linenotificationsupport.ui.impl;

import android.content.Context;
import android.widget.Toast;

import com.mysticwind.linenotificationsupport.ui.UserAlertDao;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ToastUserAlertDao implements UserAlertDao {

    private final Context context;

    @Inject
    public ToastUserAlertDao(@ApplicationContext final Context context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public void notify(final String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

}
