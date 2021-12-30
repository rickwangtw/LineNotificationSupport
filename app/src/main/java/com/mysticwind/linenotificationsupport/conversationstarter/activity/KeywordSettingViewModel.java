package com.mysticwind.linenotificationsupport.conversationstarter.activity;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mysticwind.linenotificationsupport.Application;
import com.mysticwind.linenotificationsupport.conversationstarter.ChatKeywordManager;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import timber.log.Timber;

@HiltViewModel
public class KeywordSettingViewModel extends AndroidViewModel {

    private final List<ChatKeywordManager.KeywordEntry> keywords;

    private final ChatKeywordManager chatKeywordManager;

    @Inject
    public KeywordSettingViewModel(final Application application, final ChatKeywordManager chatKeywordManager) {
        super(application);

        this.chatKeywordManager = Objects.requireNonNull(chatKeywordManager);

        keywords = chatKeywordManager.getAvailableKeywordToChatNameMap();
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
