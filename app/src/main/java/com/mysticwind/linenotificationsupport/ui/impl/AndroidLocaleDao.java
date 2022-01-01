package com.mysticwind.linenotificationsupport.ui.impl;

import android.content.res.Resources;

import com.mysticwind.linenotificationsupport.ui.LocaleDao;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AndroidLocaleDao implements LocaleDao {

    private final Resources resources;

    private String locale;

    @Inject
    public AndroidLocaleDao(final Resources resources) {
        this.resources = Objects.requireNonNull(resources);
        updateLocale();
    }

    private void updateLocale() {
        locale = resources.getConfiguration().getLocales().get(0).toLanguageTag();
    }

    @Override
    public String getLocale() {
        return locale;
    }

    @Override
    public void notifyLocaleChange() {
        updateLocale();
    }

}
