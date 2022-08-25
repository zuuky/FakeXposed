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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.sanfengandroid.common.Const;
import com.sanfengandroid.common.model.PackageModel;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.fakeinterface.NativeHook;
import com.sanfengandroid.xp.ProcessMode;
import com.sanfengandroid.xp.XpDataMode;
import com.sanfengandroid.xp.XposedEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SPProvider {
    private static final String TAG = SPProvider.class.getSimpleName();
    private static final String TEST_SP_ACTIVE = "test_sp_active_";
    private static final String SP_APP_HOOK_VISIBLE = "app_hook_visible";
    private static final String XPOSED_LIST = "xposed_modules";
    private static final String XPOSED_KEYS = "xposeds";
    private static final String SP_APP_PREFIX = "xp_conf_";
    private static final String SP_ALL = SP_APP_PREFIX + Const.GLOBAL_PACKAGE;
    private static final String INIT_KEY = "initialized_";
    private static final String SP_KEY_UPDATE_TIME = "last_update";
    private static final String SP_KEY_IGNORE_VERSION = "ignore_version";
    private static final String LIB_PATH = "lib_path";

    private static final Map<String, SharedPreferences> CACHES = new HashMap<>();
    private static XpDataMode mode;
    private static ProcessMode processMode;

    public static void setDataMode(XpDataMode mode) {
        SPProvider.mode = mode;
    }

    public static XpDataMode getMode() {
        return mode;
    }

    public static void setProcessMode(ProcessMode mode) {
        processMode = mode;
    }

    public static SharedPreferences createSharedPreferenceImpl(File file, int mode) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> clazz = Class.forName("android.app.SharedPreferencesImpl");
            Constructor<?> ctor = clazz.getDeclaredConstructor(File.class, int.class);
            ctor.setAccessible(true);
            return (SharedPreferences) ctor.newInstance(file, mode);
        } catch (Exception e) {
            throw new RuntimeException("create SharedPreferences failed.", e);
        }
    }

    public static long getHookConfigurationInstallTime(Context context) {
        try {
            SharedPreferences sp = getDefaultSharedPreferences(context);
            return sp.getLong(SP_KEY_UPDATE_TIME, 0);
        } catch (Throwable e) {
            return 0;
        }
    }

    private static SharedPreferences getDefaultSharedPreferences(Context context) {
        return getSharedPreferences(context, BuildConfig.APPLICATION_ID + "_preferences");
    }

    public static long getConfigurationUpdateTime(Context context) {
        return getDefaultSharedPreferences(context).getLong(SP_KEY_UPDATE_TIME, 0);
    }

    public static void updateConfigurationTime(Context context) {
        getDefaultSharedPreferences(context).edit()
                .putLong(SP_KEY_UPDATE_TIME, System.currentTimeMillis()).apply();
    }

    public static void configureIgnoreVersionCode(Context context, long code) {
        getDefaultSharedPreferences(context).edit().putLong(SP_KEY_IGNORE_VERSION, code).apply();
    }

    public static long getIgnoreVersionCode(Context context) {
        return getDefaultSharedPreferences(context).getLong(SP_KEY_IGNORE_VERSION, 0);
    }

    public static SharedPreferences getSharedPreferences(Context context, String name) {
        if (CACHES.containsKey(name)) {
            return CACHES.get(name);
        }
        SharedPreferences ret;
        if (processMode == ProcessMode.SELF) {
            ret = context.getSharedPreferences(name, Context.MODE_WORLD_READABLE);
            LogUtil.d(TAG,
                    "XSharedPreferences pref file: %s,context: %s,path: %s, processMode: %s, pref.getAll(): %s",
                    ret, context.toString(), name, processMode, ret.getAll());
        } else {
            ret = XposedEntry.getPref(name);
        }
        if (name.startsWith(SP_APP_HOOK_VISIBLE) || name.startsWith(SP_APP_PREFIX) || name.equals(
                XPOSED_LIST) || name.equals(SP_ALL)) {
            CACHES.put(name, ret);
        }
        return ret;
    }

    public static Map<String, Boolean> getHookApps(Context context) {
        return getHookApps(getSharedPreferences(context, SP_APP_HOOK_VISIBLE));
    }

    public static Map<String, Boolean> getHookApps(SharedPreferences sp) {
        Map<String, Boolean> map = new HashMap<>();
        Map<String, ?> values = sp.getAll();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (entry.getValue() != null && entry.getValue() instanceof Boolean) {
                map.put(entry.getKey(), (Boolean) entry.getValue());
            }
        }
        return map;
    }

    public static void setHookApp(Context context, String pkg, Boolean value) {
        SharedPreferences sp = getSharedPreferences(context, SP_APP_HOOK_VISIBLE);
        sp.edit().putBoolean(pkg, value).apply();
        updateConfigurationTime(context);
    }

    public static boolean getHookAppEnable(Context context) {
        return getHookAppEnable(getSharedPreferences(context, SP_APP_HOOK_VISIBLE),
                Const.GLOBAL_PACKAGE);
    }

    public static boolean getHookAppEnable(Context context, String pkg) {
        return getHookAppEnable(context) || getHookAppEnable(
                getSharedPreferences(context, SP_APP_HOOK_VISIBLE), pkg);
    }

    public static boolean getHookAppEnable(SharedPreferences sp, String pkg) {
        return sp.getBoolean(pkg, false);
    }

    public static SharedPreferences getAppConfig(Context context, String pkg) {
        return getSharedPreferences(context, SP_APP_PREFIX + pkg);
    }

    public static void testSpActive(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context, TEST_SP_ACTIVE);
        if (sharedPreferences == null) {
            throw new RuntimeException("testSpActive failed,can not create SharedPreferences!!!");
        }
        if (!sharedPreferences.contains(TEST_SP_ACTIVE)) {
            sharedPreferences.edit().putString(TEST_SP_ACTIVE, "true").apply();
        }
        LogUtil.d("testSpActive", "result: %s",
                sharedPreferences.getString(TEST_SP_ACTIVE, "false"));
    }

    public static boolean appHasInit(Context context, String pkg, DataModelType type) {
        return appHasInit(getAppConfig(context, pkg), type);
    }

    public static boolean appHasInit(SharedPreferences sp, DataModelType type) {
        return sp.getBoolean(INIT_KEY + type.spKey, false);
    }

    public static String getAppTypeValue(SharedPreferences sp, DataModelType type) {
        return sp.getString(type.spKey, null);
    }

    public static String getAppTypeValue(Context context, String pkg, DataModelType type) {
        return getAppTypeValue(getAppConfig(context, pkg), type);
    }

    public static void putAppStringConfig(Context context, String pkg, DataModelType type,
            JSONArray array) {
        putAppStringConfig(context, pkg, type, array.toString());
    }

    public static void putAppStringConfig(Context context, String pkg, DataModelType type,
            String value) {
        getAppConfig(context, pkg).edit().putString(type.spKey, value)
                .putBoolean(INIT_KEY + type.spKey, true).apply();
        updateConfigurationTime(context);
    }

    public static void setXposedList(Context context, Set<String> xpApks) {
        SharedPreferences sp = getSharedPreferences(context, XPOSED_LIST);
        sp.edit().putStringSet(XPOSED_KEYS, xpApks).apply();
        updateConfigurationTime(context);
    }

    public static Set<String> getXposedList(Context context) {
        SharedPreferences sp = getSharedPreferences(context, XPOSED_LIST);
        return sp.getStringSet(XPOSED_KEYS, new HashSet<>());
    }

    public static Map<String, List<ShowDataModel>> getAppData(Context context, String pkg) {
        return getAppData(context, getAppConfig(context, pkg));
    }

    public static Map<String, List<ShowDataModel>> getAppData(Context context,
            SharedPreferences sp) {
        Map<String, List<ShowDataModel>> data = new HashMap<>();
        for (DataModelType type : DataModelType.values()) {
            if (sp.getBoolean(INIT_KEY + type.spKey, false)) {
                String s = sp.getString(type.spKey, null);
                if (!TextUtils.isEmpty(s)) {
                    try {
                        JSONArray array = new JSONArray(s);
                        int size = array.length();
                        Class<? extends ShowDataModel> clazz = type.valueType;
                        List<ShowDataModel> list = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            JSONObject json = array.getJSONObject(i);
                            ShowDataModel model = clazz.newInstance();
                            model.unSerialization(json);
                        }
                        if (type == DataModelType.PACKAGE_HIDE) {
                            PackageModel xposed = new PackageModel();
                            xposed.setPackageName(PackageModel.XPOSED_PACKAGE_MASK);
                            Set<String> modules = getXposedList(context);
                            for (String m : modules) {
                                PackageModel pm = new PackageModel();
                                pm.setPackageName(m);
                                list.add(pm);
                            }
                        }
                        data.put(type.spKey, list);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return data;
    }

    public static Map<String, Set<ShowDataModel>> getOverloadAppAvailable(Context context,
            SharedPreferences sp) {
        Map<String, List<ShowDataModel>> global =
                getHookAppEnable(context) ? getAppData(context, Const.GLOBAL_PACKAGE)
                                          : new HashMap<>();
        Map<String, List<ShowDataModel>> app = getAppData(context, sp);
        Map<String, Set<ShowDataModel>> res = new HashMap<>();
        for (DataModelType type : DataModelType.values()) {
            List<ShowDataModel> globalArr = global.get(type.spKey);
            List<ShowDataModel> appArr = app.get(type.spKey);
            if (globalArr == null && appArr == null) {
                continue;
            }
            Set<ShowDataModel> set;
            if (globalArr == null) {
                set = new HashSet<>(appArr);
            } else if (appArr == null) {
                set = new HashSet<>(globalArr);
            } else {
                set = new HashSet<>(globalArr);
                set.addAll(appArr);
            }
            set.removeIf(showDataModel -> !showDataModel.isAvailable());
            res.put(type.spKey, set);
        }
        return res;
    }

    public static Map<String, Set<ShowDataModel>> getOverloadAppAvailable(Context context,
            String pkg) {
        return getOverloadAppAvailable(context, getAppConfig(context, pkg));
    }

    /**
     * 供跨进程调用返回方便序列化对象
     */
    public static Map<String, String[]> getOverloadAppJsonAvailable(Context context, String pkg) {
        Map<String, Set<ShowDataModel>> map = getOverloadAppAvailable(context, pkg);
        Map<String, String[]> res = new HashMap<>(map.size());
        for (Map.Entry<String, Set<ShowDataModel>> entry : map.entrySet()) {
            List<String> list = new ArrayList<>();
            for (ShowDataModel model : entry.getValue()) {
                try {
                    list.add(model.serialization().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            res.put(entry.getKey(), list.toArray(new String[0]));
        }
        return res;
    }

    public static File[] getAllConfigurationFiles(Context context) {
        File sharedDir;
        sharedDir = new File(context.getDataDir(), "shared_prefs");
        return sharedDir.listFiles((dir, name) -> {
            if (!(name.startsWith(SP_APP_PREFIX) || name.startsWith(XPOSED_LIST) || name.startsWith(
                    SP_APP_HOOK_VISIBLE) || name.startsWith(BuildConfig.APPLICATION_ID))) {
                return false;
            }
            return name.endsWith(".xml");
        });
    }

    public static File[] initAllConfigurationFiles(Context context) {
        File sharedDir;
        sharedDir = new File(context.getDataDir(), "shared_prefs");
        return sharedDir.listFiles((dir, name) -> {
            if (!(name.startsWith(SP_APP_PREFIX) || name.startsWith(XPOSED_LIST) || name.startsWith(
                    SP_APP_HOOK_VISIBLE) || name.startsWith(BuildConfig.APPLICATION_ID))) {
                return false;
            }
            return name.endsWith(".xml");
        });
    }


    /**
     * 系统进程执行文件同步
     */
    public static boolean writeSyncConfiguration(String name, String data, int block) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        boolean success = true;
        try {
            File file = new File(NativeHook.getConfigPath(), name);
            fw = new FileWriter(file, block >= 0);
            bw = new BufferedWriter(fw);
            bw.write(data);
            bw.flush();
            LogUtil.v(TAG, "system process write configuration file: %s, block index: %d success.",
                    name, block);
        } catch (Exception e) {
            LogUtil.e(TAG, "system process write configuration file: %s, block index: %d error.",
                    name, block, e);
            success = false;
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ignore) {
                }
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ignore) {
                }
            }
        }
        return success;
    }

    public static void setAppLibPath(Context context, final String nativeLibraryDir) {
        getSharedPreferences(context, LIB_PATH).edit().putString(LIB_PATH, nativeLibraryDir)
                .apply();
    }

    public static String getAppLibPath(Context context) {
        return getSharedPreferences(context, LIB_PATH).getString(LIB_PATH, null);
    }
}