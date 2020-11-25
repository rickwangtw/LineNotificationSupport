package com.mysticwind.linenotificationsupport.log;

public class TagBuilder {

    private static final String PREFIX = "LNS-";

    public static String build(Class clazz) {
        return PREFIX + clazz.getSimpleName();
    }

}
