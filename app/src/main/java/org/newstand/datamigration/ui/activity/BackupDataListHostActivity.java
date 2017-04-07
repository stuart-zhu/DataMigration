package org.newstand.datamigration.ui.activity;

import android.support.v4.app.Fragment;

import org.newstand.datamigration.data.model.DataCategory;
import org.newstand.datamigration.ui.fragment.AlarmListFragment;
import org.newstand.datamigration.ui.fragment.AppListFragment;
import org.newstand.datamigration.ui.fragment.BackupAppListFragment;
import org.newstand.datamigration.ui.fragment.ContactListFragment;
import org.newstand.datamigration.ui.fragment.CustomFileListFragment;
import org.newstand.datamigration.ui.fragment.MusicListFragment;
import org.newstand.datamigration.ui.fragment.PhotoListFragment;
import org.newstand.datamigration.ui.fragment.SmsListFragment;
import org.newstand.datamigration.ui.fragment.VideoListFragment;

/**
 * Created by Nick@NewStand.org on 2017/4/7 15:20
 * E-Mail: NewStand@163.com
 * All right reserved.
 */

public class BackupDataListHostActivity extends DataListHostActivity {
    @Override
    protected Fragment getFragmentByCategory(DataCategory category) {
        switch (category) {
            case Contact:
                return new ContactListFragment();
            case Music:
                return new MusicListFragment();
            case Photo:
                return new PhotoListFragment();
            case Video:
                return new VideoListFragment();
            case App:
                return new BackupAppListFragment();
            case Sms:
                return new SmsListFragment();
            case CustomFile:
                return new CustomFileListFragment();
            case Alarm:
                return new AlarmListFragment();
            default:
                throw new UnsupportedOperationException("UnSupported category " + category);
        }
    }
}