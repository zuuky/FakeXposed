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

import android.content.Context;

import com.sanfengandroid.common.model.base.ShowDataModel;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.datafilter.SPProvider;

import java.util.Map;
import java.util.Set;

/**
 * Xposed插件调用,其它地方不允许使用
 * 对插件提供统一的api,内部实现中转
 *
 * @author sanfengAndroid
 */
final class XpConfigAgent {
    private static XpDataMode mode;
    private static ProcessMode processMode;
    private static Boolean enable;

    public static void setDataMode(XpDataMode mode) {
        XpConfigAgent.mode = mode;
        SPProvider.setDataMode(mode);
    }

    public static XpDataMode getMode() {
        return mode;
    }

    public static void setProcessMode(ProcessMode mode) {
        processMode = mode;
        SPProvider.setProcessMode(mode);
    }

    public static ProcessMode getProcessMode() {
        return processMode;
    }

    public static boolean getHookAppEnable(Context context, String pkg) {
        if (enable != null) {
            return enable;
        }
        if (processMode == ProcessMode.SELF) {
            enable = SPProvider.getHookAppEnable(context, pkg);
        } else {
            // 需要远程调用
            ContentProviderAgent.RemoteArgsUnpack callback = ContentProviderAgent.getHookAppConfig(
                    context, pkg);
            enable = callback.success() && callback.enable();
        }
        LogUtil.d("getHookAppEnable enable: %s,pkg: %s,processMode: %s,mode: %s", enable, pkg,
                processMode, mode);
        return enable;
    }

    /**
     * 只有Hook开启了才调用,会出现一个进程调用多次,在一个进程内加载了多个包
     */
    public static Map<String, Set<ShowDataModel>> getAppConfig(Context context, String pkg) {
        if (processMode == ProcessMode.SELF) {
            return SPProvider.getOverloadAppAvailable(context, pkg);
        }
        return null;
    }

}
