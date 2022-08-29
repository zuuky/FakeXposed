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

package com.sanfengandroid.xp.hooks;

import com.sanfengandroid.common.model.base.DataModelType;
import com.sanfengandroid.fakeinterface.GlobalConfig;

import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 这里应该监听有属性变化然后再次调整
 */
public class HookJavaIoClass implements IHook {

    @Override
    public void hook(ClassLoader loader) throws Throwable {
        Method method = BufferedReader.class.getDeclaredMethod("readLine");
        GlobalConfig.addHookMethodModifierFilter(method);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object r = param.getResult();
                if (r != null && GlobalConfig.getMap(DataModelType.MAPS_HIDE).keySet().stream()
                        .anyMatch(a -> Objects.toString(r).contains(a))) {
                    param.setResult("");
                }
                super.afterHookedMethod(param);
            }
        });
    }
}
