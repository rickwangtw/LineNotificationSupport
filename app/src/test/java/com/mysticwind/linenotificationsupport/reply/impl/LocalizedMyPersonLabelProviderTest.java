package com.mysticwind.linenotificationsupport.reply.impl;

import static org.junit.Assert.*;

import org.junit.Test;

public class LocalizedMyPersonLabelProviderTest {

    @Test
    public void getMyPersonLabel_americanEnglishLabel() {
        LocalizedMyPersonLabelProvider classUnderTest = buildWithLocale("en-US");

        assertEquals("Me", classUnderTest.getMyPersonLabel().get());
    }

    @Test
    public void getMyPersonLabel_taiwanChineseLabel() {
        LocalizedMyPersonLabelProvider classUnderTest = buildWithLocale("zh-rTW");

        assertEquals("我", classUnderTest.getMyPersonLabel().get());
    }

    @Test
    public void getMyPersonLabel_defaultLabel() {
        LocalizedMyPersonLabelProvider classUnderTest = buildWithLocale("xxx");

        assertEquals("Me", classUnderTest.getMyPersonLabel().get());
    }

    private LocalizedMyPersonLabelProvider buildWithLocale(String locale) {
        return new LocalizedMyPersonLabelProvider(locale);
    }

}