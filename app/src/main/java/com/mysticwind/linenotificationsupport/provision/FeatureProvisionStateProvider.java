package com.mysticwind.linenotificationsupport.provision;

import android.content.SharedPreferences;

public class FeatureProvisionStateProvider {

    public static final String SHOW_DISABLE_POWER_OPTIMIZATION_TIP_KEY = "show_disable_power_optimization_tip";

    private final SharedPreferences sharedPreferences;

    public FeatureProvisionStateProvider(final SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean isDisablePowerOptimizationTipShown() {
        return sharedPreferences.getBoolean(SHOW_DISABLE_POWER_OPTIMIZATION_TIP_KEY, false);
    }

    public void setDisablePowerOptimizationTipShown() {
        sharedPreferences.edit()
                .putBoolean(SHOW_DISABLE_POWER_OPTIMIZATION_TIP_KEY, true)
                .commit();
    }

}
