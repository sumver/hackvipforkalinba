package com.sumver.kalinba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class hackvip implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE_NAME = "com.yshb.kalinba";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PACKAGE_NAME.equals(lpparam.packageName)) {
            return;
        }

        // 禁止10分钟弹窗 直接mock网络请求返回值，使得永远判断都是会员
        try {
            XposedHelpers.findAndHookMethod(
                    "com.yshb.kalinba.http.MYEnpcryptionRetrofitWrapper",
                    lpparam.classLoader,
                    "getMemberInfo",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);

                            XposedBridge.log("Xposed: Intercepted MYEnpcryptionRetrofitWrapper.getMemberInfo() call.");

                            // 创建一个模拟的 UserMemberInfo 对象
                            Class<?> userMemberInfoClass = XposedHelpers.findClass("com.yshb.kalinba.bean.UserMemberInfo", lpparam.classLoader);

                            // 创建实例
                            Object mockUserMemberInfo = userMemberInfoClass.newInstance();

                            // 设置 memberTag 为非空字符串，模拟会员状态，如果是空会进入好几个条件的判断。
                            XposedHelpers.setObjectField(mockUserMemberInfo, "memberTag", "MockVipTag");

                            // 阻止原始方法执行，并直接返回模拟的对象
                            // 注意：getMemberInfo 返回的是 Observable<UserMemberInfo>
                            // 我们需要返回一个 Observable，它能立即发出我们模拟的数据
                            // 需要找到 RxJava 的 Observable 类
                            Class<?> observableClass = XposedHelpers.findClass("io.reactivex.Observable", lpparam.classLoader);
                            Class<?> observableJustMethod = observableClass; // The 'just' method is static on Observable class

                            // 使用反射调用 Observable.just(mockUserMemberInfo) 来创建一个 Observable
                            Object mockObservable = XposedHelpers.callStaticMethod(observableClass, "just", mockUserMemberInfo);

                            // 将模拟的 Observable 作为结果返回，替换原始的网络请求 Observable
                            param.setResult(mockObservable);

                            XposedBridge.log("Xposed: 模拟数据已插入成功");
                        }
                    }
            );

        } catch (Exception e) {
            XposedBridge.log("Xposed: 错误信息: " + e.getMessage());
            e.printStackTrace();
        }


        // 未登录时的试用时长解除
        try {
            // Hook UserDataCacheManager 的 isReward 方法，确保返回 true
            XposedHelpers.findAndHookMethod(
                    "com.yshb.kalinba.common.UserDataCacheManager",
                    lpparam.classLoader,
                    "isReward",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            XposedBridge.log("Xposed: Hooked UserDataCacheManager成功");
                            param.setResult(true);
                        }
                    }
            );


            // 使其返回值 <= 1800，从而不满足 MainActivity.onResume 中未登录弹窗的条件
            XposedHelpers.findAndHookMethod(
                    "com.yshb.kalinba.common.UserDataCacheManager",
                    lpparam.classLoader,
                    "getTodayPlayTime",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            XposedBridge.log("Xposed: Hooked UserDataCacheManager.getTodayPlayTime成功");
                            param.setResult(1);
                        }
                    }
            );

        } catch (Exception e) {
            XposedBridge.log("Xposed: Error: " + e.getMessage());
            e.printStackTrace();
        }
        // 去广告（这部分没测试）
        try {
            XposedHelpers.findAndHookMethod(
                    "com.yshb.kalinba.act.MainActivity",
                    lpparam.classLoader,
                    "showAd",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            XposedBridge.log("Xposed: 广告已阻止");
                            param.setResult(null);
                        }
                    }
            );
        } catch (Exception e) {
            XposedBridge.log("Xposed: Error: " + e.getMessage());
            e.printStackTrace();
        }

    }
}