package moe.zapic.xposedqqnotify

import android.app.*
import android.content.Context
import android.content.res.XModuleResources
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import cc.chenhe.qqnotifyevo.core.NevoNotificationProcessor
import cc.chenhe.qqnotifyevo.core.NotificationProcessor
import cc.chenhe.qqnotifyevo.utils.getNotificationChannels
import de.robv.android.xposed.*
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Field
import java.lang.reflect.Method

class XposedInit : IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {
    private var MODULE_PATH: String? = null;
    var ic_qq: Int? = null;
    var ic_qzone: Int? = null;

    @Throws(Throwable::class)
    override fun initZygote(startupParam: StartupParam) {
        MODULE_PATH = startupParam.modulePath
    }
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!lpparam.packageName.equals("com.tencent.mobileqq")) return;
        var ctx: Context? = null;
        var processor: NevoNotificationProcessor? = null;

        val startup: XC_MethodHook = object : XC_MethodHook(51) {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val clz = param.thisObject.javaClass.classLoader
                        ?.loadClass("com.tencent.common.app.BaseApplicationImpl")
                    var fsApp: Field? = null
                    if (clz != null) {
                        for (f in clz.declaredFields) {
                            if (f.type == clz) {
                                fsApp = f
                                break
                            }
                        }
                    }
                    if (fsApp == null) {
                        throw NoSuchFieldException("field BaseApplicationImpl.sApplication not found")
                    }
                    val _ctx = fsApp[null] as Context
                    val _processor = NevoNotificationProcessor(_ctx);
                    if (ic_qq != null && ic_qzone != null) {
                        _processor.injectIcons(
                            IconCompat.createWithBitmap((_ctx.getDrawable(ic_qq!!))!!.toBitmap()),
                            IconCompat.createWithBitmap((_ctx.getDrawable(ic_qzone!!))!!.toBitmap())
                        );
                    } else {
                        throw Error("Icon cannot be null");
                    }
                    createNotificationChannels(_ctx);
                    processor = _processor;
                    ctx = _ctx;
                } catch (e: Throwable) {
                    throw e
                }
            }
        }
        val loadDex: Class<*> =
            lpparam.classLoader.loadClass("com.tencent.mobileqq.startup.step.LoadDex")
        val ms = loadDex.declaredMethods
        var m: Method? = null
        for (method in ms) {
            if (method.returnType == Boolean::class.javaPrimitiveType && method.parameterTypes.size == 0) {
                m = method
                break
            }
        }
        XposedBridge.hookMethod(m, startup)
        XposedBridge.hookAllMethods(
            NotificationManager::class.java,
            "notify",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        if (ctx == null || processor == null) return;
                        val n: Notification = param.args[param.args.size - 1] as Notification;
                        if (n.tickerText == null) return
                        val processed = processor!!.resolveNotification(ctx!!, lpparam.packageName, n);
                        if (processed != null) {
                            param.args[param.args.size - 1] = processed
                        }
                    } catch (e: Exception) {
                        XposedBridge.log(e);
                    }
                }
            }
        )
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        if (!resparam.packageName.equals("com.tencent.mobileqq")) return;
        val res = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        ic_qq = resparam.res.addResource(res, R.drawable.ic_notify_qq);
        ic_qzone = resparam.res.addResource(res, R.drawable.ic_notify_qzone);
    }
    private fun createNotificationChannels(ctx: Context) {
        val notificationChannels: List<NotificationChannel> = getNotificationChannels()
        val notificationChannelGroup = NotificationChannelGroup("qq_evolution", "QQ通知进化")
        val notificationManager: NotificationManager = ctx.getSystemService(NotificationManager::class.java)
        if (notificationChannels.any {
                    notificationChannel -> notificationManager.getNotificationChannel(notificationChannel.id) == null
            }) {
            XposedBridge.log("Creating channels...")
            notificationManager.createNotificationChannelGroup(notificationChannelGroup)
            notificationManager.createNotificationChannels(notificationChannels)
        }
    }
}