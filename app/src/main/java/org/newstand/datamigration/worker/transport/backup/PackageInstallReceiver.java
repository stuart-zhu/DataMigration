package org.newstand.datamigration.worker.transport.backup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.newstand.datamigration.provider.SettingsProvider;
import org.newstand.logger.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Nick@NewStand.org on 2017/4/6 11:23
 * E-Mail: NewStand@163.com
 * All right reserved.
 */

class PackageInstallReceiver extends BroadcastReceiver {

    private CountDownLatch latch = new CountDownLatch(1);

    private String packageName;

    public PackageInstallReceiver(String packageName) {
        this.packageName = packageName;
    }

    public void register(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
        context.registerReceiver(this, intentFilter);
    }

    public void unRegister(Context context) {
        context.unregisterReceiver(this);
    }

    public boolean waitUtilInstalled() {
        while (true) {
            try {
                return latch.await(SettingsProvider.getAppInstallerTimeout().timeMills,
                        TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {

            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            // They send us bad action~
            return;
        }

        switch (action) {
            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_REPLACED:
                String packageName = intent.getData().getSchemeSpecificPart();
                Logger.d("Received action %s for %s", action, packageName);
                if (packageName == null) return;
                if (packageName.equals(this.packageName) && latch != null) {
                    latch.countDown();
                }
                break;
        }
    }
}
