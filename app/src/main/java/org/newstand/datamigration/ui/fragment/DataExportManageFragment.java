package org.newstand.datamigration.ui.fragment;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.widget.TextView;

import org.newstand.datamigration.R;
import org.newstand.datamigration.cache.LoadingCacheManager;
import org.newstand.datamigration.common.AbortSignal;
import org.newstand.datamigration.common.Consumer;
import org.newstand.datamigration.common.StartSignal;
import org.newstand.datamigration.data.model.DataCategory;
import org.newstand.datamigration.data.model.DataRecord;
import org.newstand.datamigration.repo.BKSessionRepoService;
import org.newstand.datamigration.sync.Sleeper;
import org.newstand.datamigration.ui.widget.InputDialogCompat;
import org.newstand.datamigration.utils.Collections;
import org.newstand.datamigration.worker.backup.BackupRestoreListener;
import org.newstand.datamigration.worker.backup.BackupRestoreListenerMainThreadAdapter;
import org.newstand.datamigration.worker.backup.DataBackupManager;
import org.newstand.datamigration.worker.backup.session.Session;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import cn.iwgang.simplifyspan.SimplifySpanBuild;
import cn.iwgang.simplifyspan.other.OnClickableSpanListener;
import cn.iwgang.simplifyspan.unit.SpecialClickableUnit;
import cn.iwgang.simplifyspan.unit.SpecialTextUnit;

/**
 * Created by Nick@NewStand.org on 2017/3/15 16:29
 * E-Mail: NewStand@163.com
 * All right reserved.
 */

public class DataExportManageFragment extends DataTransportManageFragment {

    private CountDownLatch mTaskLatch;

    private BackupRestoreListener mExportListener = new BackupRestoreListenerMainThreadAdapter() {
        @Override
        public void onStartMainThread() {
            super.onStartMainThread();
        }

        @Override
        public void onCompleteMainThread() {
            super.onCompleteMainThread();
            mTaskLatch.countDown();
        }

        @Override
        public void onPieceFailMainThread(DataRecord record, Throwable err) {
            super.onPieceFailMainThread(record, err);
            onProgressUpdate();
        }

        @Override
        public void onPieceSuccessMainThread(DataRecord record) {
            super.onPieceSuccessMainThread(record);
            onProgressUpdate();
        }

        @Override
        public void onPieceStartMainThread(DataRecord record) {
            super.onPieceStartMainThread(record);
            showCurrentPieceInUI(record);
        }
    };

    @Override
    protected void readyToGo() {
        super.readyToGo();

        final LoadingCacheManager cache = LoadingCacheManager.droid();

        final DataBackupManager dataBackupManager = DataBackupManager.from(getContext(), getSession());

        mTaskLatch = Sleeper.waitingFor(DataCategory.values().length, new Runnable() {
            @Override
            public void run() {
                enterState(STATE_TRANSPORT_END);
            }
        });

        DataCategory.consumeAllInWorkerThread(new Consumer<DataCategory>() {
            @Override
            public void consume(@NonNull DataCategory category) {
                Collection<DataRecord> dataRecords = cache.checked(category);
                if (Collections.nullOrEmpty(dataRecords)) {
                    mTaskLatch.countDown();// Release one!!!
                    return;
                }

                StartSignal startSignal = new StartSignal();
                AbortSignal abortSignal = dataBackupManager.performBackupAsync(dataRecords, category, mExportListener, startSignal);

                getStats().merge(mExportListener.getStats());

                getAbortSignals().add(abortSignal);
                getStartSignals().add(startSignal);
            }
        }, new Runnable() {
            @Override
            public void run() {
                Collections.consumeRemaining(getStartSignals(), new Consumer<StartSignal>() {
                    @Override
                    public void consume(@NonNull StartSignal startSignal) {
                        startSignal.start();
                    }
                });
            }
        });
    }

    @Override
    protected Session onCreateSession() {
        return Session.create();
    }

    @Override
    int getStartTitle() {
        return R.string.title_backup_exporting;
    }

    @Override
    int getCompleteTitle() {
        return R.string.title_backup_export_complete;
    }

    @Override
    void onDoneButtonClick() {
        // Save session
        BKSessionRepoService.get().insert(getSession());
        getActivity().finish();
    }

    private void showCurrentPieceInUI(DataRecord record) {
        getConsoleSummaryView().setText(record.getDisplayName());
    }

    @Override
    SimplifySpanBuild onCreateCompleteSummary() {
        SimplifySpanBuild summary = buildTransportReport(getStats());
        summary.append("\n\n");
        summary.append(getStringSafety(R.string.action_remark));
        summary.append(new SpecialTextUnit(getSession().getName())
                .setTextColor(ContextCompat.getColor(getContext(), R.color.accent))
                .showUnderline()
                .useTextBold()
                .showUnderline()
                .setClickableUnit(new SpecialClickableUnit(getConsoleSummaryView(), new OnClickableSpanListener() {
                    @Override
                    public void onClick(TextView tv, String clickText) {
                        showNameSettingsDialog(getSession().getName());
                    }
                })));
        summary.append(getStringSafety(R.string.action_remark_tips));
        return summary;
    }

    protected boolean validateInput(CharSequence in) {
        return !TextUtils.isEmpty(in) && !in.toString().contains("Tmp_")
                && !in.toString().contains(File.separator);
    }

    protected void showNameSettingsDialog(final String currentName) {
        new InputDialogCompat.Builder(getActivity())
                .setTitle(getString(R.string.action_remark))
                .setInputDefaultText(currentName)
                .setInputMaxWords(32)
                .setPositiveButton(getString(android.R.string.ok), new InputDialogCompat.ButtonActionListener() {
                    @Override
                    public void onClick(CharSequence inputText) {
                        DataBackupManager.from(getContext()).renameSessionChecked(getSession(), inputText.toString());
                        updateCompleteSummary();
                    }
                })
                .interceptButtonAction(new InputDialogCompat.ButtonActionIntercepter() {
                    @Override
                    public boolean onInterceptButtonAction(int whichButton, CharSequence inputText) {
                        return !validateInput(inputText);
                    }
                })
                .setNegativeButton(getString(android.R.string.cancel), new InputDialogCompat.ButtonActionListener() {
                    @Override
                    public void onClick(CharSequence inputText) {
                        // Nothing.
                    }
                })
                .show();
    }
}