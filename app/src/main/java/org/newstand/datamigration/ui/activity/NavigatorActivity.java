package org.newstand.datamigration.ui.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.tbruyelle.rxpermissions2.RxPermissions;

import org.newstand.datamigration.DataMigrationApp;
import org.newstand.datamigration.R;
import org.newstand.datamigration.repo.BKSessionRepoService;
import org.newstand.datamigration.sync.SharedExecutor;
import org.newstand.datamigration.worker.backup.session.Session;

import java.util.Date;

import io.reactivex.functions.Consumer;
import si.virag.fuzzydateformatter.FuzzyDateTimeFormatter;

public class NavigatorActivity extends TransitionSafeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigator);

        setTitle(getTitle());

        findView(R.id.card_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCard1Pop(findView(v, android.R.id.text2));
            }
        });

        findViewById(R.id.card_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCard2Pop(findView(v, android.R.id.text2));
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onStart() {
        super.onStart();
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) throws Exception {
                        if (granted) {
                            queryShowHistory();
                        } else {
                            onPermissionNotGrant();
                        }
                    }
                });
    }

    private void onPermissionNotGrant() {
        AlertDialog alertDialog = new AlertDialog.Builder(NavigatorActivity.this)
                .setTitle("No permission")
                .setMessage("WTF????")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishWithAfterTransition();
                    }
                }).create();
        alertDialog.show();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void queryShowHistory() {
        final TextView tv1 = findView(findView(R.id.card_1), android.R.id.text2);
        SharedExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Session last = BKSessionRepoService.get().findLast();
                final String intro;
                if (last == null) {
                    intro = getString(R.string.title_backup_history_noop);
                } else {
                    intro = getString(R.string.title_backup_history,
                            FuzzyDateTimeFormatter.getTimeAgo(getApplicationContext(),
                                    new Date(last.getDate())));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv1.setText(intro);
                    }
                });
            }
        });

    }

    private void showCard1Pop(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_backup:
                        transitionTo(new Intent(NavigatorActivity.this, AndroidCategoryViewerActivity.class));
                        break;
                    case R.id.action_restore:
                        transitionTo(new Intent(NavigatorActivity.this, BackupSessionPickerActivity.class));
                        break;
                }
                return true;
            }
        });
        popup.inflate(R.menu.navigator_card_1);
        popup.show();
    }

    private void showCard2Pop(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_send:
                        transitionTo(new Intent(NavigatorActivity.this, WFDDataSenderActivity.class));
                        break;
                    case R.id.action_receive:
                        transitionTo(new Intent(NavigatorActivity.this, WFDDataReceiverActivity.class));
                        break;
                }
                return true;
            }
        });
        popup.inflate(R.menu.navigator_card_2);
        popup.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            transitionTo(new Intent(this, SettingsActivity.class));
        }

        if (id == R.id.action_user_actions) {
            transitionTo(new Intent(this, UserActionViewerActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean isMainActivity() {
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DataMigrationApp app = (DataMigrationApp) getApplication();
        app.cleanup();
    }
}
