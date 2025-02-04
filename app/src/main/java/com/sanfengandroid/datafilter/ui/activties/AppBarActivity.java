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

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.common.util.NetUtil;
import com.sanfengandroid.datafilter.BuildConfig;
import com.sanfengandroid.datafilter.NativeTestActivity;
import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.SPProvider;
import com.sanfengandroid.datafilter.XpApplication;
import com.sanfengandroid.datafilter.listener.AppFilterable;
import com.sanfengandroid.datafilter.ui.DialogBuilder;
import com.sanfengandroid.datafilter.viewmodel.AppBean;
import com.sanfengandroid.datafilter.viewmodel.ApplicationViewModel;

import java.util.List;
import java.util.Objects;

import eu.chainfire.libsuperuser.Debug;
import eu.chainfire.libsuperuser.Shell;


public class AppBarActivity extends AppCompatActivity {
    protected SearchView searchView;
    protected MenuItem mSearchItem, mFilterSystemItem, mFilterXposedItem;
    protected ApplicationViewModel mViewModel;
    protected Toolbar mToolbar;
    private int layoutId;

    @Override
    protected void onCreate(
            @Nullable
                    Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layoutId);
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mViewModel = new ViewModelProvider(XpApplication.getInstance()).get(
                ApplicationViewModel.class);
    }

    public void setViewLayout(int resId) {
        layoutId = resId;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (searchView != null && !searchView.isIconified()) {
                searchView.setIconified(true);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(getMenuInflateId(), menu);
        mSearchItem = menu.findItem(R.id.action_search);
        if (mSearchItem != null) {
            searchView = (SearchView) mSearchItem.getActionView();
        }
        mFilterSystemItem = menu.findItem(R.id.action_filter_system_app);
        mFilterXposedItem = menu.findItem(R.id.action_filter_xposed_app);
        MenuItem item = menu.findItem(R.id.disable_system);
        if (item != null) {
            item.setChecked(true);
        }
        item = menu.findItem(R.id.disable_xposed);
        if (item != null) {
            item.setChecked(true);
        }
        item = menu.findItem(R.id.action_test);
        if (item != null) {
            item.setVisible(true);
        }
        return true;
    }

    protected int getMenuInflateId() {
        return R.menu.menu_main;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_test) {
            Intent intent = new Intent(this, NativeTestActivity.class);
            startActivity(intent);
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.soft_reboot) {
            Debug.setSanityChecksEnabled(false);
            if (!Shell.SU.available()) {
                Snackbar.make(getSnackView(), R.string.error_no_root_permission,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                DialogBuilder.confirmCancelShow(this, R.string.confirm_reboot, (d, w) -> {
                    List<String> list = Shell.SU.run(
                            "setprop ctl.restart surfaceflinger; setprop ctl.restart zygote");
                    if (list == null) {
                        DialogBuilder.confirmShow(this, R.string.soft_reboot_error, 0, null);
                    }
                }, null);

            }
        } else if (id == R.id.disable_system) {
            callbackSystemFilter(AppFilterable.Option.ALL);
            item.setChecked(true);
        } else if (id == R.id.filter_system) {
            callbackSystemFilter(AppFilterable.Option.THIS);
            item.setChecked(true);
        } else if (id == R.id.filter_non_system) {
            callbackSystemFilter(AppFilterable.Option.OTHER);
            item.setChecked(true);
        } else if (id == R.id.disable_xposed) {
            callbackXposedFilter(AppFilterable.Option.ALL);
            item.setChecked(true);
        } else if (id == R.id.filter_xposed) {
            callbackXposedFilter(AppFilterable.Option.THIS);
            item.setChecked(true);
        } else if (id == R.id.filter_non_xposed) {
            callbackXposedFilter(AppFilterable.Option.OTHER);
            item.setChecked(true);
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setOnQueryTextListener(SearchView.OnQueryTextListener listener) {
        searchView.setOnQueryTextListener(listener);
    }

    protected void setSearchViewMenuVisible(boolean visible) {
        mSearchItem.setVisible(visible);
    }

    protected void setFilterSystemMenuVisible(boolean visible) {
        mFilterSystemItem.setVisible(visible);
        mFilterXposedItem.setVisible(visible);
    }

    protected void setAppBarTitle(String title) {
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);
    }

    protected void callbackSystemFilter(AppFilterable.Option option) {
    }

    protected void callbackXposedFilter(AppFilterable.Option option) {
    }

    protected View getSnackView() {
        return null;
    }

    protected void update(boolean force) {
        AsyncTask.execute(() -> {
            String net = NetUtil.requestHttps(AppBean.UPDATE_URL);
            AppBean app = new AppBean(net);
            LogUtil.d("new version " + app);
            if (app.getVersionCode() > BuildConfig.VERSION_CODE && !TextUtils.isEmpty(
                    app.getLink())) {
                if (!force && SPProvider.getIgnoreVersionCode(AppBarActivity.this)
                        == app.getVersionCode()) {
                    return;
                }
                String note = getResources().getConfiguration().locale.getCountry().equals("CN")
                              ? app.getNoteCN() : app.getNoteEN();
                getSnackView().post(
                        () -> DialogBuilder.confirmNeutralShow(AppBarActivity.this, R.string.update,
                                note, R.string.update, R.string.ignore, R.string.cancel,
                                (dialog, which) -> {
                                    Uri uri = Uri.parse(app.getLink());
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    startActivity(intent);
                                }, (dialog, which) -> SPProvider.configureIgnoreVersionCode(
                                        AppBarActivity.this, app.getVersionCode())));
            } else {
                if (force) {
                    Snackbar.make(getSnackView(), R.string.has_latest_version,
                            Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }
}
