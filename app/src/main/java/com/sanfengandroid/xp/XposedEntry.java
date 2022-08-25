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

package com.sanfengandroid.xp;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.Context;
import android.content.ContextParams;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.datafilter.BuildConfig;
import com.sanfengandroid.datafilter.NativeTestActivity;
import com.sanfengandroid.fakeinterface.GlobalConfig;
import com.sanfengandroid.fakeinterface.NativeHook;
import com.sanfengandroid.fakeinterface.NativeInit;
import com.sanfengandroid.xp.hooks.XposedFilter;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author sanfengAndroid
 */
public class XposedEntry implements IXposedHookLoadPackage {
    private static final String TAG = XposedEntry.class.getSimpleName();
    private static boolean hasHook = false;
    public static final String GLOBAL_XP_PREF = "global_xp_pref";

    static {
        LogUtil.addCallback(Log.ERROR, (state, level, tag, msg, throwable) -> {
            XposedBridge.log(tag + ": " + msg);
            if (throwable != null) {
                XposedBridge.log(throwable);
            }
        });
        LogUtil.addCallback(Log.WARN, (state, level, tag, msg, throwable) -> {
            XposedBridge.log(tag + ": " + msg);
            if (throwable != null) {
                XposedBridge.log(throwable);
            }
        });
        //LogUtil.addCallback(Log.INFO, (state, level, tag, msg, throwable) -> {
        //    XposedBridge.log(tag + ": " + msg);
        //    if (throwable != null) {
        //        XposedBridge.log(throwable);
        //    }
        //});
        //LogUtil.addCallback(Log.VERBOSE, (state, level, tag, msg, throwable) -> {
        //    XposedBridge.log(tag + ": " + msg);
        //    if (throwable != null) {
        //        XposedBridge.log(throwable);
        //    }
        //});
        //LogUtil.addCallback(Log.DEBUG, (state, level, tag, msg, throwable) -> {
        //    XposedBridge.log(tag + ": " + msg);
        //    if (throwable != null) {
        //        XposedBridge.log(throwable);
        //    }
        //});
        LogUtil.setLogMode(LogUtil.LogMode.CALLBACK_AND_PRINT);
        LogUtil.minLogLevel = Log.VERBOSE;
    }

    //public static XSharedPreferences getPref(String path) {
    //    XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID, path);
    //    return pref.getFile().canRead() ? pref : null;
    //}

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            if ("android".equals(lpparam.processName)) {
                return;
            }
            LogUtil.v(TAG, "process: %s, package: %s, hasHook: %s", lpparam.processName,
                    lpparam.packageName, hasHook);
            if (hasHook) {
                LogUtil.v(TAG, "current process: %s, package: %s has been hooked",
                        lpparam.processName, lpparam.packageName);
                return;
            }
            if (BuildConfig.APPLICATION_ID.equals(lpparam.packageName)) {
                if (BuildConfig.APPLICATION_ID.equals(lpparam.processName)) {
                    hookSelf(lpparam.classLoader);
                    Context contextImpl = createAppContext(lpparam.appInfo);
                    XpConfigAgent.setDataMode(XpDataMode.X_SP);
                    XpConfigAgent.setProcessMode(ProcessMode.SELF);
                    NativeHook.initLibraryPath(contextImpl, lpparam.appInfo.targetSdkVersion);
                    NativeTestActivity.initTestData(contextImpl);
                    NativeInit.initNative(contextImpl, lpparam.processName);
                    new XposedFilter().hook(lpparam.classLoader);
                    NativeInit.startNative();
                }
                return;
            }
            LogUtil.v(TAG, "targetSDK: %d, current class loader: %s",
                    lpparam.appInfo.targetSdkVersion, XposedEntry.class.getClassLoader());
            Context contextImpl = createAppContext(lpparam.appInfo);
            // 使用自身ContentProvider如果未启动则手动启动,这样会增加很长的启动时间
            XpDataMode mode = XpConfigAgent.xSharedPreferencesAvailable() ? XpDataMode.X_SP
                                                                          : XpDataMode.APP_CALL;
            LogUtil.d(TAG, "current data mode: %s", mode.name());
            XpConfigAgent.setDataMode(mode);
            XpConfigAgent.setProcessMode(ProcessMode.HOOK_APP);
            if (!XpConfigAgent.getHookAppEnable(contextImpl, lpparam.packageName)) {
                return;
            }
            Map<String, Set<ShowDataModel>> map = XpConfigAgent.getAppConfig(contextImpl,
                    lpparam.packageName);
            if (map == null) {
                LogUtil.e(TAG, "get package: " + lpparam.packageName + " configuration failed.");
                return;
            }
            LogUtil.d(TAG, " package " + lpparam.packageName + " map: %s", map);
            GlobalConfig.init(map);
            GlobalConfig.removeThis(lpparam.packageName);
            NativeHook.initLibraryPath(contextImpl, lpparam.appInfo.targetSdkVersion);
            NativeInit.initNative(contextImpl, lpparam.processName);
            new XposedFilter().hook(lpparam.classLoader);
            NativeInit.startNative();
            hasHook = true;
        } catch (Throwable e) {
            LogUtil.e(TAG, "Hook error", e);
        }
    }

    @SuppressLint("PrivateApi")
    public Context createAppContext(ApplicationInfo ai) throws Throwable {
        Constructor<?> ctor = Class.forName("android.app.ContextImpl").getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Context contextImpl;
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        Object loadedApk = activityThread.getPackageInfoNoCheck(ai, null);
        ContextParams params = null;
        if (Build.VERSION.SDK_INT >= 31) {
            params = new ContextParams.Builder().build();
        }
        switch (Build.VERSION.SDK_INT) {
            case 32:
                contextImpl = (Context) ctor.newInstance(null, activityThread, loadedApk, params,
                        null, null, null, null, null, 0, null, null);
                break;
            case 31:
                contextImpl = (Context) ctor.newInstance(null, activityThread, loadedApk, params,
                        null, null, null, null, null, 0, null, null);
                break;
            case 30:
                contextImpl = (Context) ctor.newInstance(null, activityThread, loadedApk, null,
                        null, null, null, 0, null, null);
                break;
            case 29:
                contextImpl = (Context) ctor.newInstance(null, activityThread, loadedApk, null,
                        null, null, 0, null, null);
                break;
            case 28:
                contextImpl = (Context) ctor.newInstance(null, activityThread, loadedApk, null,
                        null, null, 0, null);
                break;
            case 27:
                contextImpl = (Context) ctor.newInstance(null, activityThread, loadedApk, null,
                        null, null, 0, null);
                break;
            case 26:
                contextImpl = (Context) ctor.newInstance(null, activityThread, loadedApk, null,
                        null, null, 0, null);
                break;
            case 25:
                contextImpl = (Context) ctor.newInstance(null, activityThread, loadedApk, null,
                        null, 0, null, null, -1);
                break;
            case 24:
                contextImpl = (Context) ctor.newInstance(null, activityThread, loadedApk, null,
                        null, 0, null, null, -1);
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported version " + Build.VERSION.SDK_INT);

        }
        return contextImpl;
    }

    private void hookSelf(ClassLoader loader) throws ClassNotFoundException {
        XposedHelpers.findAndHookMethod(
                loader.loadClass("com.sanfengandroid.datafilter.ui.fragments.MainFragment"),
                "isActive", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(true);
                    }
                });
        LogUtil.v(TAG, "hooked myself");
    }
}
