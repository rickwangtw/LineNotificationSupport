package com.mysticwind.linenotificationsupport.module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

public interface HiltQualifiers {

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface MaxNotificationsPerApp {}

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @interface PackageName {}

}
