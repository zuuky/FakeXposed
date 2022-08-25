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

package com.sanfengandroid.datafilter.ui.fragments;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.datafilter.BuildConfig;
import com.sanfengandroid.datafilter.R;
import com.sanfengandroid.datafilter.SPProvider;
import com.sanfengandroid.datafilter.XpApplication;
import com.sanfengandroid.datafilter.viewmodel.ApplicationViewModel;
import com.sanfengandroid.fakeinterface.Installer;

public class MainFragment extends Fragment implements View.OnClickListener {

    public static final String VIEW_TAG = "Main";
    private static final boolean SHOW_INSTALL = true;
    private ApplicationViewModel mViewModel;
    private SwitchMaterial installHookConfig;
    private View tipView, updateCard;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(
            @Nullable
                    Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(XpApplication.getInstance()).get(
                ApplicationViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.setDataModelType(DataModelType.NOTHING);
        long time = SPProvider.getHookConfigurationInstallTime(requireContext());
        long time1 = SPProvider.getConfigurationUpdateTime(requireContext());
        updateCard.setVisibility(time != 0 && time != time1 ? View.VISIBLE : View.GONE);
    }

    public static boolean isActive() {
        LogUtil.d("Prevent inlining");
        return TextUtils.equals(XpApplication.getInstance().getString(R.string.set_gid), "123");
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Nullable
    @Override
    public View onCreateView(
            @NonNull
                    LayoutInflater inflater,
            @Nullable
                    ViewGroup container,
            @Nullable
                    Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ImageView icon = view.findViewById(R.id.status_icon);
        boolean active = isActive();
        icon.setImageDrawable(requireActivity().getResources().getDrawable(
                active ? R.drawable.ic_state_normal_24dp : R.drawable.ic_state_warning_24dp, null));
        TextView status = view.findViewById(R.id.status_text);
        String tip = getString(active ? R.string.status_normal : R.string.status_warning) + "("
                + BuildConfig.APP_TYPE + ")";
        status.setText(tip);

        updateCard = view.findViewById(R.id.update_card);
        view.findViewById(R.id.sync_hook_configuration).setOnClickListener(this);
        installHookConfig = view.findViewById(R.id.install_hook_configuration);
        installHookConfig.setOnClickListener(this);
        installHookConfig.setChecked(
                SPProvider.getHookConfigurationInstallTime(this.getContext()) != 0);
        mViewModel.setSnackMessageObserver(this,
                s -> Snackbar.make(requireView(), s, Snackbar.LENGTH_SHORT).show());
        if (!SHOW_INSTALL) {
            installHookConfig.setVisibility(View.GONE);
            updateCard.setVisibility(View.GONE);
        }
        return view;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.install_hook_configuration || id == R.id.sync_hook_configuration) {
            AsyncTask.execute(() -> {
                String tip;
                boolean success;
                boolean uninstall =
                        id != R.id.sync_hook_configuration && !installHookConfig.isChecked();
                try {
                    success = uninstall ? Installer.uninstallHookFile(requireContext())
                                        : Installer.installHookFile(requireContext());
                    tip = uninstall ? getString(R.string.success_uninstall_hook_configuration)
                                    : getString(R.string.success_install_hook_configuration);
                } catch (Exception e) {
                    tip = e.getMessage();
                    success = false;
                }
                mViewModel.setMessage(tip);
                boolean finalSuccess = success;
                installHookConfig.post(() -> {
                    installHookConfig.setChecked(uninstall != finalSuccess);
                    if (uninstall && !finalSuccess) {
                        return;
                    }
                    updateCard.setVisibility(finalSuccess ? View.GONE : View.VISIBLE);
                });
            });
        }
    }
}