package com.mysticwind.linenotificationsupport.reply.impl;

import android.content.Context;

import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.google.common.collect.ImmutableMap;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.reply.MyPersonLabelProvider;
import com.mysticwind.linenotificationsupport.ui.LocaleDao;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class LocalizedMyPersonLabelProvider implements MyPersonLabelProvider {

    private static final String ENGLISH_PREFIX = "en-";
    private static final String ENGLISH_LABEL = "Me";
    private static final String CHINESE_PREFIX = "zh-";
    private static final String CHINESE_LABEL = "我";

    public static final String DEFAULT_LABEL = ENGLISH_LABEL;

    private static final Map<String, String> PREFIX_TO_LABEL_MAP = ImmutableMap.of(
            ENGLISH_PREFIX, ENGLISH_LABEL,
            CHINESE_PREFIX, CHINESE_LABEL
    );

    private final LocaleDao localeDao;
    private final Context context;

    @Inject
    public LocalizedMyPersonLabelProvider(final LocaleDao localeDao, @ApplicationContext final Context context) {
        this.localeDao = Objects.requireNonNull(localeDao);
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public Optional<String> getMyPersonLabel() {
        final String locale = localeDao.getLocale();
        for (final Map.Entry<String, String> entry : PREFIX_TO_LABEL_MAP.entrySet()) {
            final String prefix = entry.getKey();
            final String label = entry.getValue();

            if (locale.startsWith(prefix)) {
                return Optional.of(label);
            }
        }
        return Optional.of(DEFAULT_LABEL);
    }

    @Override
    public Person getMyPerson() {
        return new Person.Builder()
                .setName(getMyPersonLabel().get())
                .setIcon(IconCompat.createWithResource(context, R.drawable.outline_person_24))
                .build();
    }

}
