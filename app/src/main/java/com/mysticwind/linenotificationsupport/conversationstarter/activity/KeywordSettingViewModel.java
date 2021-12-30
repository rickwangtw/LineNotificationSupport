package com.mysticwind.linenotificationsupport.conversationstarter.activity;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.conversationstarter.ChatKeywordManager;

import java.util.List;

import timber.log.Timber;

public class KeywordSettingViewModel extends AndroidViewModel {

    private final List<ChatKeywordManager.KeywordEntry> keywords = ImmutableList.of(
            new ChatKeywordManager.KeywordEntry("MyKeyword", "MyChatId", "MyChatName")
    );

    public KeywordSettingViewModel(final Application application) {
        super(application);
    }


    public LiveData<List<ChatKeywordManager.KeywordEntry>> getAllKeywords() {
        // TODO make this really LiveData
        final MutableLiveData<List<ChatKeywordManager.KeywordEntry>> liveData = new MutableLiveData();
        liveData.setValue(keywords);
        return liveData;
    }

    public void update(String chatId, String keyword) {
        // TODO
        Timber.d("Update keyword [%s] for chat ID [%s]", keyword, chatId);
    }

}
