package com.mysticwind.linenotificationsupport.ui.impl;

import android.content.Context;

import com.mysticwind.linenotificationsupport.ui.LocalizationDao;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class AndroidLocalizationDao implements LocalizationDao {

    private final Context context;

    @Inject
    public AndroidLocalizationDao(@ApplicationContext final Context context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public String getLocalizedString(int resourceId, Object... arguments) {
        return context.getString(resourceId, arguments);
    }

}
