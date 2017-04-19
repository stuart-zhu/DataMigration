package org.newstand.datamigration.ui.tiles;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;

import org.newstand.datamigration.R;

import dev.nick.tiles.tile.QuickTileView;

/**
 * Created by Nick@NewStand.org on 2017/4/6 18:26
 * E-Mail: NewStand@163.com
 * All right reserved.
 */

public class ThanksTile extends ThemedTile {

    public ThanksTile(@NonNull Context context) {
        super(context, null);
    }

    @Override
    void onInitView(Context context) {

        this.titleRes = R.string.title_thanks;
        this.summaryRes = R.string.summary_thanks;
        this.iconRes = R.drawable.ic_gift;

        this.tileView = new QuickTileView(getContext(), this) {
            @Override
            public void onClick(View v) {
                super.onClick(v);
                showThanks();
            }
        };
    }

    private String guys() {
        return "学渣匆匆\n" +
                "Dwughjsd";
    }

    private void showThanks() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_thanks)
                .setMessage(guys())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}