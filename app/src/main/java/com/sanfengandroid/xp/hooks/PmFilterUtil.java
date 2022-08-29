package com.sanfengandroid.xp.hooks;

import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import com.alibaba.fastjson.JSONObject;
import com.sanfengandroid.common.Const;
import com.sanfengandroid.common.model.PackageModel;
import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.common.util.LogUtil;
import com.sanfengandroid.fakeinterface.GlobalConfig;

import java.util.Iterator;
import java.util.List;

public class PmFilterUtil {
    public static void filterQueryIntent(Object result, DataModelType type) {
        LogUtil.d("go into filterQueryIntent result: %s", result);
        Iterator<ResolveInfo> riIt;
        if (result instanceof List) {
            riIt = ((List) result).iterator();
        } else {
            riIt = ((ParceledListSlice<ResolveInfo>) result).getList().iterator();
        }
        while (riIt.hasNext()) {
            ResolveInfo info = riIt.next();
            if (GlobalConfig.stringEqualBlacklist(info.resolvePackageName, type)) {
                LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter ResolveInfo : %s", info);
                riIt.remove();
                continue;
            }
            if (info.activityInfo != null) {
                if (GlobalConfig.stringEqualBlacklist(info.activityInfo.packageName, type)) {
                    LogUtil.w(Const.JAVA_MONITOR_STATE,
                            "hide filter ResolveInfo(ActivityInfo) : %s", info);
                    riIt.remove();
                    continue;
                }
                Bundle metaData = info.activityInfo.applicationInfo.metaData;
                if (metaData != null) {
                    for (final String s : metaData.keySet()) {
                        if (s.contains(PackageModel.XPOSED_PACKAGE_MASK)) {
                            riIt.remove();
                            LogUtil.w(Const.JAVA_MONITOR_STATE,
                                    "info.activityInfo.applicationInfo: %s, metaData: %s, hide: %s",
                                    info.activityInfo.packageName,
                                    JSONObject.toJSONString(metaData.keySet()), info);
                            break;
                        }
                    }
                    continue;
                }
            }
            if (info.serviceInfo != null) {
                if (GlobalConfig.stringEqualBlacklist(info.serviceInfo.packageName, type)) {
                    LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter ResolveInfo(ServiceInfo) : %s",
                            info);
                    riIt.remove();
                    continue;
                }
            }
            if (info.providerInfo != null) {
                if (GlobalConfig.stringEqualBlacklist(info.providerInfo.packageName, type)) {
                    LogUtil.w(Const.JAVA_MONITOR_STATE,
                            "hide filter ResolveInfo(ProviderInfo) : %s", info);
                    riIt.remove();
                }
            }
        }
    }

    public static void filterQueryPackage(Object result, DataModelType type) {
        Iterator<PackageInfo> piIt = ((ParceledListSlice<PackageInfo>) result).getList().iterator();
        while (piIt.hasNext()) {
            PackageInfo info = piIt.next();
            if (GlobalConfig.stringEqualBlacklist(info.packageName, type)) {
                LogUtil.w(Const.JAVA_MONITOR_STATE, "hide filter PackageInfo : %s",
                        info.packageName);
                piIt.remove();
            }
        }
    }
}
