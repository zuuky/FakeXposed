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

package com.sanfengandroid.datafilter;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.sanfengandroid.common.util.Util;
import com.sanfengandroid.datafilter.viewmodel.ApplicationViewModel;
import com.sanfengandroid.xp.ProcessMode;

public class XpApplication extends Application implements ViewModelStoreOwner {
    private static XpApplication xpApplication = null;
    private ViewModelStore mStore;
    private ApplicationViewModel viewModel;

    public static XpApplication getInstance() {
        return xpApplication;
    }

    public static ApplicationViewModel getViewModel() {
        return xpApplication.viewModel;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        xpApplication = this;
        SPProvider.setProcessMode(ProcessMode.SELF);
        if (BuildConfig.APPLICATION_ID.equals(Util.getProcessName(base))) {
            mStore = new ViewModelStore();
            viewModel = new ViewModelProvider(this).get(ApplicationViewModel.class);
        }
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return mStore;
    }
}
