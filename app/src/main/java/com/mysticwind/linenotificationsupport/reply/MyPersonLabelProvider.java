package com.mysticwind.linenotificationsupport.reply;

import androidx.core.app.Person;

import java.util.Optional;

public interface MyPersonLabelProvider {

    Optional<String> getMyPersonLabel();
    Person getMyPerson();

}
