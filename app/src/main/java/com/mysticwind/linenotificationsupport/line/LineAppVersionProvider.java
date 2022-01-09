package com.mysticwind.linenotificationsupport.line;

import static com.mysticwind.linenotificationsupport.line.Constants.LINE_PACKAGE_NAME;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class LineAppVersionProvider {

    private final PackageManager packageManager;

    @Inject
    public LineAppVersionProvider(final PackageManager packageManager) {
        this.packageManager = Objects.requireNonNull(packageManager);
    }

    public Optional<String> getLineAppVersion() {
        // https://stackoverflow.com/questions/50795458/android-how-to-get-any-application-version-by-package-name
        try {
            final PackageInfo packageInfo = packageManager.getPackageInfo(LINE_PACKAGE_NAME, 0);
            return Optional.of(packageInfo.versionName);
        } catch (final PackageManager.NameNotFoundException e) {
            Timber.e(e, "LINE not installed. Package: " + LINE_PACKAGE_NAME);
            return Optional.empty();
        }
    }

}
