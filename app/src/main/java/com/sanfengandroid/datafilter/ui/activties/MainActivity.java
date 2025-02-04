/*
 * Copyright (c) 2021 FakeXposed by sanfengAndroid.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.sanfengandroid.datafilter.ui.activties;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;
import com.sanfengandroid.common.model.InstallPackageModel;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.SPProvider;
import com.sanfengandroid.datafilter.listener.AppFilterable;
import com.sanfengandroid.datafilter.ui.fragments.InstalledPackageFragment;
import com.sanfengandroid.datafilter.ui.fragments.ItemFragment;
import com.sanfengandroid.datafilter.ui.fragments.MainFragment;
import com.sanfengandroid.xp.XpDataMode;

public class MainActivity extends AppBarActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private Fragment current;
    private final SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (current instanceof Filterable) {
                Filter filter = ((Filterable) current).getFilter();
                if (filter != null) {
                    filter.filter(newText);
                }
            }
            return true;
        }
    };
    private DrawerLayout mDrawerLayout;
    private ImageView mImageView;
    private final FragmentManager.FragmentLifecycleCallbacks callbacks = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentResumed(
                @NonNull
                        FragmentManager fm,
                @NonNull
                        Fragment f) {
            super.onFragmentResumed(fm, f);
            current = f;
            if (mSearchItem != null) {
                setSearchViewMenuVisible(f instanceof Filterable);
                setFilterSystemMenuVisible(f instanceof AppFilterable.SystemAppFilter);
                mFilterSystemItem.setChecked(false);
            }
            setAppBarTitle(mViewModel.getDataModelType() == DataModelType.NOTHING ? getString(
                    R.string.app_name) : getString(mViewModel.getDataModelType().nameId));
            InstallPackageModel model = mViewModel.getInstallApp(mViewModel.getCurrentPackage());
            if (current instanceof ItemFragment && model != null) {
                mImageView.setImageDrawable(model.icon);
            } else {
                mImageView.setImageDrawable(null);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setViewLayout(R.layout.main_activity);
        super.onCreate(savedInstanceState);
        mImageView = findViewById(R.id.item_package);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance(), MainFragment.VIEW_TAG)
                    .commitNow();
        }
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(callbacks, true);
        for (DataModelType type : DataModelType.values()) {
            if (type != DataModelType.NOTHING) {
                navigationView.getMenu()
                        .add(R.id.config_group, type.ordinal(), type.ordinal(), type.nameId);
            }
        }
        navigationView.getMenu().setGroupCheckable(R.id.config_group, true, true);
        SPProvider.setDataMode(XpDataMode.X_SP);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        setOnQueryTextListener(queryTextListener);
        setSearchViewMenuVisible(current instanceof Filterable);
        setFilterSystemMenuVisible(current instanceof AppFilterable.SystemAppFilter);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void callbackSystemFilter(AppFilterable.Option option) {
        if (current instanceof AppFilterable.SystemAppFilter) {
            ((AppFilterable.SystemAppFilter) current).filterSystemApp(option);
        }
    }

    @Override
    protected void callbackXposedFilter(AppFilterable.Option option) {
        if (current instanceof AppFilterable.XposedAppFilter) {
            ((AppFilterable.XposedAppFilter) current).filterXposedApp(option);
        }
    }

    private void switchDataModelType(int id) {
        mDrawerLayout.postDelayed(() -> {
            DataModelType type = DataModelType.values()[id];
            mViewModel.setDataModelType(type);
            FragmentManager fm = getSupportFragmentManager();
            if (current instanceof ItemFragment) {
                ((ItemFragment) current).reload();
                callbacks.onFragmentResumed(fm, current);
            } else if (current instanceof InstalledPackageFragment) {
                callbacks.onFragmentResumed(fm, current);
            } else {
                InstalledPackageFragment ipf = InstalledPackageFragment.newInstance();
                fm.beginTransaction()
                        .replace(R.id.container, ipf, InstalledPackageFragment.VIEW_TAG)
                        .addToBackStack(null).commit();
            }
        }, 250);
    }

    @Override
    public boolean onNavigationItemSelected(
            @NonNull
                    MenuItem item) {
        item.setChecked(true);
        int id = item.getItemId();
        switchDataModelType(id);
        mDrawerLayout.closeDrawers();
        return true;
    }

    @Override
    public void onClick(View v) {
    }
}