package org.newstand.datamigration.ui.fragment;

import org.newstand.datamigration.R;
import org.newstand.datamigration.data.model.DataCategory;
import org.newstand.datamigration.ui.tiles.LoaderConfigCategoryTile;

import java.util.List;

import dev.nick.tiles.tile.Category;
import dev.nick.tiles.tile.DashboardFragment;

/**
 * Created by Nick on 2017/6/21 17:05
 */

public class LoaderConfigCategorySettingsFragment extends DashboardFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.layout_loader_configure;
    }

    @Override
    protected void onCreateDashCategories(final List<Category> categories) {
        super.onCreateDashCategories(categories);

        Category priv = new Category();
        priv.titleRes = R.string.category_private;
        priv.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.Contact));
        priv.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.CallLog));
        priv.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.Sms));
        priv.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.Alarm));

        Category mm = new Category();
        mm.titleRes = R.string.category_mm;
        mm.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.Music));
        mm.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.Video));
        mm.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.Photo));

        Category conf = new Category();
        conf.titleRes = R.string.category_advanced;
        conf.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.Wifi));
        conf.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.App));
        conf.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.SystemApp));
        conf.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.SystemSettings));
        conf.addTile(new LoaderConfigCategoryTile(getContext(), DataCategory.CustomFile));

        categories.add(priv);
        categories.add(mm);
        categories.add(conf);
    }
}
