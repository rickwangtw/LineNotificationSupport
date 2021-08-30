package com.mysticwind.linenotificationsupport.reply.impl;

import com.google.common.collect.ImmutableMap;
import com.mysticwind.linenotificationsupport.reply.MyPersonLabelProvider;

import org.apache.commons.lang3.Validate;

import java.util.Map;
import java.util.Optional;

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

    private final String locale;

    public LocalizedMyPersonLabelProvider(final String locale) {
        this.locale = Validate.notBlank(locale);
    }

    @Override
    public Optional<String> getMyPersonLabel() {
        for (final Map.Entry<String, String> entry : PREFIX_TO_LABEL_MAP.entrySet()) {
            final String prefix = entry.getKey();
            final String label = entry.getValue();

            if (locale.startsWith(prefix)) {
                return Optional.of(label);
            }
        }
        return Optional.of(DEFAULT_LABEL);
    }

}
