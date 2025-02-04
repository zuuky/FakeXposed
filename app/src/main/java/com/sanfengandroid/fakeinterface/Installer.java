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

package com.sanfengandroid.fakeinterface;

import android.content.Context;

import com.sanfengandroid.datafilter.SPProvider;
import com.sanfengandroid.fakelinker.FileInstaller;

import java.io.File;

public class Installer {
    private static boolean root = true;

    public static boolean installHookFile(Context context) throws Exception {
        setRoot(true);
        FileInstaller.setConfigPath(NativeHook.getConfigPath());
        File[] files = SPProvider.getAllConfigurationFiles(context);
        //if (files == null || files.length <= 0) {
        //    files = SPProvider.initAllConfigurationFiles(context);
        //}
        FileInstaller.installFile(context, files, root);
        return true;
    }

    public static boolean uninstallHookFile(Context context) throws Exception {
        setRoot(true);
        FileInstaller.setConfigPath(NativeHook.getConfigPath());
        File[] files = SPProvider.getAllConfigurationFiles(context);
        if (files == null) {
            return true;
        }
        File[] configs = new File[files.length];
        for (int i = 0; i < configs.length; i++) {
            configs[i] = new File(NativeHook.getConfigPath(), files[i].getName());
        }
        FileInstaller.uninstallFile(context, configs, root);
        return true;
    }

    public static void setRoot(boolean root) {
        Installer.root = root;
        if (root) {
            FileInstaller.setFileOwner(0, 0);
        }
    }
}
