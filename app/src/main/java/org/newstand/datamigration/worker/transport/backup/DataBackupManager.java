package org.newstand.datamigration.worker.transport.backup;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.common.io.Files;
import com.google.gson.Gson;

import org.newstand.datamigration.common.AbortException;
import org.newstand.datamigration.common.AbortSignal;
import org.newstand.datamigration.common.Consumer;
import org.newstand.datamigration.common.ContextWireable;
import org.newstand.datamigration.common.StartSignal;
import org.newstand.datamigration.data.model.AppRecord;
import org.newstand.datamigration.data.model.CallLogRecord;
import org.newstand.datamigration.data.model.ContactRecord;
import org.newstand.datamigration.data.model.DataCategory;
import org.newstand.datamigration.data.model.DataRecord;
import org.newstand.datamigration.data.model.FileBasedRecord;
import org.newstand.datamigration.data.model.SMSRecord;
import org.newstand.datamigration.data.model.SettingsRecord;
import org.newstand.datamigration.data.model.WifiRecord;
import org.newstand.datamigration.loader.LoaderSource;
import org.newstand.datamigration.policy.ExtraDataRule;
import org.newstand.datamigration.provider.SettingsProvider;
import org.newstand.datamigration.repo.ExtraDataRulesRepoService;
import org.newstand.datamigration.sync.SharedExecutor;
import org.newstand.datamigration.utils.Collections;
import org.newstand.datamigration.utils.MediaScannerClient;
import org.newstand.datamigration.worker.transport.Session;
import org.newstand.datamigration.worker.transport.Stats;
import org.newstand.datamigration.worker.transport.TransportListener;
import org.newstand.datamigration.worker.transport.TransportListenerAdapter;
import org.newstand.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by Nick@NewStand.org on 2017/3/8 17:22
 * E-Mail: NewStand@163.com
 * All right reserved.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DataBackupManager {
    @Getter
    private Context context;

    @Getter
    private Session session;

    public static DataBackupManager from(Context context) {
        return new DataBackupManager(context, Session.create());
    }

    public static DataBackupManager from(Context context, Session session) {
        return new DataBackupManager(context, session);
    }

    public boolean renameSessionChecked(LoaderSource source, Session session, String name) {
        String dir = source.getParent() == LoaderSource.Parent.Backup
                ? SettingsProvider.getBackupRootDir()
                : SettingsProvider.getReceivedRootDir();
        File from = new File(dir + File.separator + session.getName());
        File to = new File(dir + File.separator + name);
        try {
            Files.move(from, to);
            session.rename(name);
            return true;
        } catch (IOException e) {
            Logger.e(e, "Fail to rename session");
        }
        return false;
    }

    public void performBackup(final Collection<DataRecord> dataRecords,
                              final DataCategory dataCategory) {
        new BackupWorker(new TransportListenerAdapter(), dataRecords, dataCategory, new AbortSignal()).run();
    }

    public void performBackup(TransportListener listener, final Collection<DataRecord> dataRecords,
                              final DataCategory dataCategory) {
        new BackupWorker(listener, dataRecords, dataCategory, new AbortSignal()).run();
    }

    public AbortSignal performBackupAsync(final Collection<DataRecord> dataRecords,
                                          final DataCategory dataCategory,
                                          final TransportListener listener) {

        return performBackupAsync(dataRecords, dataCategory, listener, null);
    }

    public AbortSignal performBackupAsync(final Collection<DataRecord> dataRecords,
                                          final DataCategory dataCategory,
                                          final TransportListener listener,
                                          final StartSignal startSignal) {

        AbortSignal abortSignal = new AbortSignal();
        final BackupWorker worker = new BackupWorker(listener, dataRecords, dataCategory, abortSignal);
        listener.setStats(worker.status);
        if (startSignal == null) {
            SharedExecutor.execute(worker);
        } else {
            startSignal.addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    SharedExecutor.execute(worker);
                }
            });
        }
        return abortSignal;
    }

    public AbortSignal performRestoreAsync(final Collection<DataRecord> dataRecords,
                                           final DataCategory dataCategory,
                                           final TransportListener listener) {
        return performRestoreAsync(dataRecords, dataCategory, listener, null);
    }

    public AbortSignal performRestoreAsync(final Collection<DataRecord> dataRecords,
                                           final DataCategory dataCategory,
                                           final TransportListener listener,
                                           final StartSignal startSignal) {
        AbortSignal abortSignal = new AbortSignal();
        final RestoreWorker worker = new RestoreWorker(listener, dataRecords, dataCategory, abortSignal);
        listener.setStats(worker.status);
        if (startSignal == null) {
            SharedExecutor.execute(worker);
        } else {
            startSignal.addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    SharedExecutor.execute(worker);
                }
            });
        }
        return abortSignal;
    }

    private BackupSettings getBackupSettingsByCategory(DataCategory dataCategory, DataRecord record) {
        switch (dataCategory) {
            case Music:
            case Photo:
            case Video:
            case CustomFile:
                return getFileBackupSettings(dataCategory, record);
            case App:
            case SystemApp:
                AppBackupSettings appBackupSettings = new AppBackupSettings();
                appBackupSettings.setAppRecord((AppRecord) record);
                appBackupSettings.setDestApkPath(
                        SettingsProvider.getBackupDirByCategory(dataCategory, session)
                                + File.separator + record.getDisplayName()
                                + File.separator + SettingsProvider.getBackupAppApkDirName()
                                + File.separator + record.getDisplayName() + AppRecord.APK_FILE_PREFIX);// DMBK2/APP/Phone/apk/XX.apk
                appBackupSettings.setDestMetaPath(
                        SettingsProvider.getBackupDirByCategory(dataCategory, session)
                                + File.separator + record.getDisplayName()
                                + File.separator + SettingsProvider.getBackupAppApkDirName()
                                + File.separator + record.getDisplayName() + AppRecord.APK_META_PREFIX);// DMBK2/APP/Phone/apk/XX.meta
                appBackupSettings.setDestDataPath(
                        SettingsProvider.getBackupDirByCategory(dataCategory, session)
                                + File.separator + record.getDisplayName()
                                + File.separator + SettingsProvider.getBackupAppDataDirName()
                                + File.separator + "data.tar.gz");// DMBK2/APP/Phone/data/data.tar.gz
                appBackupSettings.setDestExtraDataPath(
                        SettingsProvider.getBackupDirByCategory(dataCategory, session)
                                + File.separator + record.getDisplayName()
                                + File.separator + SettingsProvider.getBackupExtraDataDirName());// DMBK2/APP/Phone/extra_data
                appBackupSettings.setSourceApkPath(((FileBasedRecord) record).getPath());

                appBackupSettings.setBackupApp(((AppRecord) record).isHandleApk());
                appBackupSettings.setBackupData(((AppRecord) record).isHandleData());

                appBackupSettings.setSourceDataPath(SettingsProvider.getAppDataDir() + File.separator + ((AppRecord) record).getPkgName());
                // Query extra rules.
                ExtraDataRule rule = ExtraDataRulesRepoService.get().findByPkg(context, ((AppRecord) record).getPkgName());
                if (rule != null && rule.isEnabled()) {
                    String[] dirs = rule.parseDir();
                    appBackupSettings.setExtraDirs(dirs);
                }
                return appBackupSettings;
            case Contact:
                ContactBackupSettings contactBackupSettings = new ContactBackupSettings();
                contactBackupSettings.setDataRecord(new ContactRecord[]{(ContactRecord) record});
                contactBackupSettings.setDestPath(SettingsProvider.getBackupDirByCategory(dataCategory, session)
                        + File.separator + record.getDisplayName() + "@" + record.getId() + ContactBackupSettings.SUBFIX);
                return contactBackupSettings;
            case Sms:
                SMSBackupSettings smsBackupSettings = new SMSBackupSettings();
                smsBackupSettings.setSmsRecord((SMSRecord) record);
                smsBackupSettings.setDestPath(SettingsProvider.getBackupDirByCategory(dataCategory, session)
                        + File.separator + record.getId());
                return smsBackupSettings;
            case CallLog:
                CallLogBackupSettings callLogBackupSettings = new CallLogBackupSettings();
                callLogBackupSettings.setDataRecord(new CallLogRecord[]{(CallLogRecord) record});
                callLogBackupSettings.setDestPath(SettingsProvider.getBackupDirByCategory(dataCategory, session)
                        + File.separator
                        + mixedName(record.getDisplayName())
                        + "@"
                        + ((CallLogRecord) record).getDate()
                        + CallLogBackupSettings.SUBFIX);
                return callLogBackupSettings;
            case Wifi:
                WifiBackupSettings wifiBackupSettings = new WifiBackupSettings();
                wifiBackupSettings.setRecord((WifiRecord) record);
                wifiBackupSettings.setDestPath(SettingsProvider.getBackupDirByCategory(dataCategory, session)
                        + File.separator
                        + "wpa_supplicant"
                        + "@"
                        + mixedName(record.getDisplayName())
                        + WifiBackupSettings.SUBFIX);
                return wifiBackupSettings;
            case SystemSettings:
                SystemSettingsBackupSettings systemSettingsBackupSettings = new SystemSettingsBackupSettings();
                systemSettingsBackupSettings.setDestPath(SettingsProvider.getBackupDirByCategory(dataCategory, session)
                        + File.separator
                        + record.getDisplayName()
                        + SystemSettingsBackupSettings.SUBFIX);
                systemSettingsBackupSettings.setRecord((SettingsRecord) record);
                return systemSettingsBackupSettings;
        }
        throw new IllegalArgumentException("Unknown for:" + dataCategory.name());
    }

    private String mixedName(String from) {
        return String.valueOf(from.hashCode());
    }

    private FileBackupSettings getFileBackupSettings(DataCategory category, DataRecord record) {
        FileBackupSettings fileBackupSettings = new FileBackupSettings();
        FileBasedRecord fileBasedRecord = (FileBasedRecord) record;
        fileBackupSettings.setSourcePath(fileBasedRecord.getPath());
        fileBackupSettings.setDestPath(SettingsProvider.getBackupDirByCategory(category, session)
                + File.separator + record.getDisplayName());
        return fileBackupSettings;
    }

    private RestoreSettings getRestoreSettingsByCategory(DataCategory dataCategory, DataRecord record) {
        switch (dataCategory) {
            case Music:
            case Photo:
            case Video:
            case CustomFile:
                FileRestoreSettings fileRestoreSettings = new FileRestoreSettings();
                fileRestoreSettings.setSourcePath(((FileBasedRecord) record).getPath());
                fileRestoreSettings.setDestPath(SettingsProvider.getRestoreDirByCategory(dataCategory, session)
                        + File.separator + record.getDisplayName());
                return fileRestoreSettings;
            case App:
            case SystemApp:
                AppRestoreSettings appRestoreSettings = new AppRestoreSettings();
                appRestoreSettings.setSourceApkPath(((FileBasedRecord) record).getPath());
                appRestoreSettings.setSourceDataPath(SettingsProvider.getBackupDirByCategory(dataCategory, session)
                        + File.separator + record.getDisplayName()
                        + File.separator + SettingsProvider.getBackupAppDataDirName()
                        + File.separator + "data.tar.gz");// DMBK2/APP/Phone/data/data.tar.gz
                appRestoreSettings.setInstallApp(((AppRecord) record).isHandleApk());
                appRestoreSettings.setInstallData(((AppRecord) record).isHandleData());
                appRestoreSettings.setDestDataPath(SettingsProvider.getAppDataDir() + File.separator + ((AppRecord) record).getPkgName());
                appRestoreSettings.setAppRecord((AppRecord) record);
                appRestoreSettings.setExtraSourceDataPath(SettingsProvider.getBackupDirByCategory(dataCategory, session)
                        + File.separator + record.getDisplayName()
                        + File.separator + SettingsProvider.getBackupExtraDataDirName());
                return appRestoreSettings;
            case Contact:
                ContactRestoreSettings contactRestoreSettings = new ContactRestoreSettings();
                ContactRecord contactRecord = (ContactRecord) record;
                contactRestoreSettings.setSourcePath(contactRecord.getPath());
                return contactRestoreSettings;
            case Sms:
                SMSRestoreSettings smsRestoreSettings = new SMSRestoreSettings();
                smsRestoreSettings.setSmsRecord((SMSRecord) record);
                return smsRestoreSettings;
            case CallLog:
                CallLogRestoreSettings callLogRestoreSettings = new CallLogRestoreSettings();
                callLogRestoreSettings.setCallLogRecord((CallLogRecord) record);
                return callLogRestoreSettings;
            case Wifi:
                WifiRestoreSettings wifiRestoreSettings = new WifiRestoreSettings();
                wifiRestoreSettings.setRecord((WifiRecord) record);
                return wifiRestoreSettings;
            case SystemSettings:
                SystemSettingsRestoreSettings systemSettingsRestoreSettings = new SystemSettingsRestoreSettings();
                systemSettingsRestoreSettings.setRecord((SettingsRecord) record);
                return systemSettingsRestoreSettings;

        }
        throw new IllegalArgumentException("Unknown for:" + dataCategory.name());
    }

    private BackupAgent getAgentByCategory(DataCategory category) {
        switch (category) {
            case CallLog:
                return new CallLogBackupAgent();
            case Music:
                return new MusicBackupAgent();
            case Photo:
                return new PhotoBackupAgent();
            case Video:
                return new VideoBackupAgent();
            case CustomFile:
                return new FileBackupAgent();
            case Contact:
                return new ContactBackupAgent();
            case App:
            case SystemApp:
                return new AppBackupAgent();
            case Sms:
                return new SMSBackupAgent();
            case Wifi:
                return new WifiBackupAgent();
            case SystemSettings:
                return new SystemSettingsBackupAgent();
        }
        throw new IllegalArgumentException("Unknown for:" + category.name());
    }

    private class BackupWorker implements Runnable {

        TransportListener listener;
        Collection<DataRecord> dataRecords;
        DataCategory dataCategory;

        SimpleStats status;

        boolean canceled;

        BackupWorker(TransportListener listener,
                     Collection<DataRecord> dataRecords,
                     final DataCategory dataCategory,
                     AbortSignal abortSignal) {
            this.listener = listener;
            this.dataRecords = dataRecords;
            this.dataCategory = dataCategory;
            this.status = new SimpleStats();
            this.status.init(dataRecords.size());
            abortSignal.addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    canceled = true;
                    Logger.w("BackupWorker canceled %s", dataCategory.name());
                }
            });
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            final BackupAgent backupAgent = getAgentByCategory(dataCategory);
            if (backupAgent instanceof ContextWireable) {
                ContextWireable contextWireable = (ContextWireable) backupAgent;
                contextWireable.wire(getContext());
            }
            listener.onStart();
            Collections.consumeRemaining(dataRecords, new Consumer<DataRecord>() {
                @Override
                public void accept(@NonNull DataRecord record) {
                    if (canceled) {
                        listener.onPieceFail(record, new AbortException());
                        status.onFail();
                        return;
                    }
                    listener.onPieceStart(record);
                    BackupSettings settings = getBackupSettingsByCategory(dataCategory, record);
                    try {
                        BackupAgent.Res res = backupAgent.backup(settings);
                        if (BackupAgent.Res.isOk(res)) {
                            listener.onPieceSuccess(record);
                            status.onSuccess();
                        } else {
                            listener.onPieceFail(record, res);
                            status.onFail();
                        }
                    } catch (Exception e) {
                        listener.onPieceFail(record, e);
                        status.onFail();
                    }
                }
            });

            // Write the session info.
            String infoFilePath = SettingsProvider.getBackupSessionInfoPath(session);
            Gson gson = new Gson();
            String jsonStr = gson.toJson(session);
            try {
                Files.asCharSink(new File(infoFilePath), Charset.defaultCharset()).write(jsonStr);
                Logger.v("Session info has been written to %s", infoFilePath);
            } catch (IOException e) {
                Logger.e(e, "Fail to write session info, WTF???");
            }

            // Scan.
            try {
                MediaScannerClient.scanSync(context, SettingsProvider.getBackupSessionDir(session));
            } catch (InterruptedException ignored) {

            }
            listener.onComplete();
        }
    }

    private class RestoreWorker implements Runnable {

        TransportListener listener;
        Collection<DataRecord> dataRecords;
        DataCategory dataCategory;

        SimpleStats status;

        boolean canceled;

        RestoreWorker(TransportListener listener,
                      Collection<DataRecord> dataRecords,
                      final DataCategory dataCategory,
                      AbortSignal signal) {
            this.listener = listener;
            this.dataRecords = dataRecords;
            this.dataCategory = dataCategory;
            this.status = new SimpleStats();
            this.status.init(dataRecords.size());

            signal.addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    canceled = true;
                    Logger.w("RestoreWorker canceled %s", dataCategory.name());
                }
            });
        }

        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            final BackupAgent backupAgent = getAgentByCategory(dataCategory);
            if (backupAgent instanceof ContextWireable) {
                ContextWireable contextWireable = (ContextWireable) backupAgent;
                contextWireable.wire(getContext());
            }
            listener.onStart();
            Collections.consumeRemaining(dataRecords, new Consumer<DataRecord>() {
                @Override
                public void accept(@NonNull DataRecord record) {
                    if (canceled) {
                        listener.onPieceFail(record, new AbortException());
                        status.onFail();
                        return;
                    }
                    listener.onPieceStart(record);
                    RestoreSettings settings = getRestoreSettingsByCategory(dataCategory, record);
                    try {
                        BackupAgent.Res res = backupAgent.restore(settings);
                        if (BackupAgent.Res.isOk(res)) {
                            listener.onPieceSuccess(record);
                            status.onSuccess();
                        } else {
                            listener.onPieceFail(record, res);
                            status.onFail();
                        }
                    } catch (Exception e) {
                        listener.onPieceFail(record, e);
                        status.onFail();
                    }
                }
            });
            listener.onComplete();
        }
    }

    @ToString
    private class SimpleStats implements Stats {

        @Setter(AccessLevel.PACKAGE)
        @Getter
        private int total, left, success, fail;

        private void init(int size) {
            total = left = size;
            Logger.d("init status %s", toString());
        }

        private void onPiece() {
            left--;
        }

        @Override
        public void onSuccess() {
            success++;
            onPiece();
        }

        @Override
        public void onFail() {
            fail++;
            onPiece();
        }

        @Override
        public Stats merge(Stats with) {

            total += with.getTotal();
            left += with.getLeft();
            success += with.getSuccess();
            fail += with.getFail();

            return this;
        }
    }
}
