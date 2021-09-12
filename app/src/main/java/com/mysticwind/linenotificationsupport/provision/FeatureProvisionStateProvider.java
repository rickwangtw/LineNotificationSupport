package com.mysticwind.linenotificationsupport.provision;

import android.content.Context;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import java.util.Optional;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public class FeatureProvisionStateProvider {

    private static final String FILE_NAME = "provision";
    public static final String SHOW_DISABLE_POWER_OPTIMIZATION_TIP_KEY = "show_disable_power_optimization_tip";
    private final Preferences.Key<Boolean> SHOW_DISABLE_POWER_OPTIMIZATION_TIP_PREFERENCE_KEY =
            PreferencesKeys.booleanKey(SHOW_DISABLE_POWER_OPTIMIZATION_TIP_KEY);

    private final RxDataStore<Preferences> dataStore;

    public FeatureProvisionStateProvider(final Context context) {
        dataStore = new RxPreferenceDataStoreBuilder(context, FILE_NAME).build();
    }

    // TODO how can we default to false?
    public @NonNull Flowable<Optional<Boolean>> isDisablePowerOptimizationTipShown() {
        return dataStore.data()
                .map(prefs -> Optional.ofNullable(prefs.get(SHOW_DISABLE_POWER_OPTIMIZATION_TIP_PREFERENCE_KEY)));
    }

    public void setDisablePowerOptimizationTipShown() {
        dataStore.updateDataAsync(preferences -> {
            final MutablePreferences mutablePreferences = preferences.toMutablePreferences();
            mutablePreferences.set(SHOW_DISABLE_POWER_OPTIMIZATION_TIP_PREFERENCE_KEY, true);
            return Single.just(mutablePreferences);
        });
    }

}
