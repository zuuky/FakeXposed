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

import android.util.Pair;

import com.sanfengandroid.common.bean.EnvBean;
import com.sanfengandroid.fakeinterface.MapsMode;

import java.util.Arrays;
import java.util.List;

/**
 * @author sanfengAndroid
 * @date 2020/11/01
 */
public class DefaultLists {

    //COMMON_KEYWORD  ,"su", "ZygoteInit", "system" ,"self","root",
    public static final List<String> COMMON_KEYWORD_LIST = Arrays.asList("supersu", "daemonsu",
            "superuser", "zuperfakefile", "xposed", "lsposed", "magisk", "TracerPid",
            "XposedBridge");

    public static final List<String> DEFAULT_APPS_LIST = Arrays.asList("org.lsposed.manager",
            "org.meowcat.edxposed.manager", "de.robv.android.xposed.installer");

    public static final List<String> DEFAULT_KEYWORD_LIST = Arrays.asList("noshufou", "rootcloak",
            "chainfire", "titanium", "substrate", "greenify", "busybox", "titanium", "tmpsu");

    public static final List<String> DEFAULT_FILES_LIST = COMMON_KEYWORD_LIST;

    public static final List<String> DEFAULT_SYMBOL_LIST = Arrays.asList(
            "riru_is_zygote_methods_replaced", "riru_get_version");

    public static final List<String> DEFAULT_CLASS_LIST = Arrays.asList("de.robv.android.xposed",
            "de.robv.android.xposed.XposedBridge", "de.robv.android.xposed.XposedHelpers",
            "com.android.internal.os.ZygoteInit");

    public static final Pair<String, String>[] DEFAULT_GLOBAL_PROPERTY_LIST = new Pair[]{
            new Pair<>("ro.build.selinux", "1"), new Pair<>("ro.build.tags", "release-keys"),
            new Pair<>("ro.secure", "0"), new Pair<>("ro.debuggable", "1")};

    public static final EnvBean[] DEFAULT_SYSTEM_ENV_LIST;

    public static final Pair<String, String>[] DEFAULT_GLOBAL_VARIABLE_LIST = new Pair[]{
            new Pair<>("adb_enabled", "0"), new Pair<>("development_settings_enabled", "0")};

    public static final Pair<String, String>[] DEFAULT_MAPS_RULE_LIST = new Pair[]{
            new Pair("libmemtrack_real.so", MapsMode.MM_REMOVE.key)};

    static {
        EnvBean bean = new EnvBean("CLASSPATH", "XposedBridge");
        DEFAULT_SYSTEM_ENV_LIST = new EnvBean[]{bean};
    }
}
