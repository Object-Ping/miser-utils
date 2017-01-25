package com.mcxtzhang.miserutils.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by Administrator on 2016/2/6.
 */
public class ComeOnMoneyService extends AccessibilityService {
    private static final boolean DBG = true;
    private static final String TAG = "ComeOnMoneyService";

    /**
     * 微信的包名
     */
    private static final String PKG_WECHAT = "com.tencent.mm";
    /**
     * 2016 09 20 add 钉钉包名
     */
    private static final String PKG_DINGDING = "com.alibaba.android.rimet";

    /**
     * QQ的包名
     */
    private static final String PKG_QQ = "com.tencent.mobileqq";
    /**
     * QQ主界面和聊天详情界面的Activity：
     */
    private final String QQ_ACTIVITY_CHAT = "com.tencent.mobileqq.activity.SplashActivity";

    /**
     * QQ拆开红包的Activity：
     */
    private final String QQ_ACTIVITY_OPENED = "cooperation.qwallet.plugin.QWalletPluginProxyActivity";

    /**
     * QQ悬浮窗界面：com.tencent.mobileqq.activity.QQLSActivity
     */
    private final String QQ_ACTIVITY_FLOAT = "com.tencent.mobileqq.activity.QQLSActivity";

    /**
     * QQ红包消息的关键字
     */
    private static final String QQ_ENVELOPE_TEXT_KEY = "[QQ红包]";

    /**
     * QQ打开红包界面的动作关键字
     */
    private static final String QQ_ACTION_OPEN_OTHER = "点击拆开";
    /**
     * QQ口令红包
     */
    private static final String QQ_ACTION_OPEN_KOULING = "口令红包";
    private static final String QQ_ACTION_CLICK_KOULING = "点击输入口令";


    /******************************************************微信红包相关*********************************************************************/
    /**
     * 通知栏 微信红包消息的关键字
     */
    static final String WX_ENVELOPE_TEXT_KEY = "[微信红包]";

    /**
     * 打开红包界面的动作关键字
     */
    private static final String WX_ACTION_OPEN_OTHER = "领取红包";
    //private static final String WX_ACTION_OPEN_SELF = "查看红包";

    /**
     * 一旦有查看领取详情，说明红包已经领完了 应该返回
     */
    private static final String WX_ACTION_BACK_NO_MONEY = "查看领取详情";
    /******************************************************钉钉红包相关*********************************************************************/
    /**
     * 通知栏 钉钉红包消息的关键字
     */
    private static final String DINGDING_ENVELOPE_TEXT_KEY = "[红包]";

    //钉钉主页
    private static final String DINGDING_ACTIVITY_MAIN = "com.alibaba.android.rimet.biz.home.activity.HomeActivity";

    //钉钉聊天页面
    private static final String DINGDING_ACTIVITY_CHAT = "com.alibaba.android.dingtalkim.activities.ChatMsgActivity";
    /**
     * Q钉钉聊天页面红包关键字
     */
    private static final String DINGDING_ACTION_SEE_GIFT = "查看红包";

    //钉钉红包页面
    private static final String DINGDING_ACTIVITY_PICK_GIFT = "com.alibaba.android.dingtalk.redpackets.activities.PickRedPacketsActivity";
    /******************************************************
     * Alipay scan fu
     *********************************************************************/
    private static final String PKG_ALIPAY = "com.eg.android.AlipayGphone";

    private static final String ALI_FU_ACTIVITY_RESULT = " com.alipay.mobile.scan.as.main.MainCaptureActivity";

    @Override
    public void onInterrupt() {
        Toast.makeText(this, "中断抢QQ红包服务", Toast.LENGTH_SHORT).show();
        if (DBG) Log.e(TAG, "中断抢QQ红包服务");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Toast.makeText(this, "连接抢QQ红包服务", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        final CharSequence eventPkg = event.getPackageName();
        if (DBG) Log.d(TAG, "包名---->" + eventPkg);
        if (DBG) Log.d(TAG, "事件---->" + event);
        if (PKG_QQ.equals(eventPkg)) {
            //通知栏事件
            if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence t : texts) {
                        String text = String.valueOf(t);
                        if (text.contains(QQ_ENVELOPE_TEXT_KEY)) {
                            wakeAndUnlock(true);
                            openNotification(event);
                            break;
                        }
                    }
                }
            } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                openEnvelopeQQ(event);
            }

        }
        //微信红包相关
        else if (PKG_WECHAT.equals(eventPkg)) {
            //通知栏事件
            if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence t : texts) {
                        String text = String.valueOf(t);
                        if (text.contains(WX_ENVELOPE_TEXT_KEY)) {
                            wakeAndUnlock(true);
                            openNotification(event);
                            break;
                        }
                    }
                }
            } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                openEnvelopeWx(event);
            }
        }
        // 2016 09 20add
        //DD红包相关
        else if (PKG_DINGDING.equals(eventPkg)) {
            //通知栏事件
            if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence t : texts) {
                        String text = String.valueOf(t);
                        if (text.contains(DINGDING_ENVELOPE_TEXT_KEY)) {
                            wakeAndUnlock(true);
                            openNotification(event);
                            break;
                        }
                    }
                }
            } else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                findDingDingGift(event);
            }
        }
        // alipay scan fu
        else if (PKG_ALIPAY.equals(eventPkg)) {
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                aliFuclickReplayBtn(event);

            }
        }
    }

    //auto click 再来一次 button,named :com.alipay.mobile.scan.arplatform:id/lucky_button,
    //收下福卡 id :com.alipay.android.phone.wallet.roosteryear:id/cr_receive
    //点击重试 id :com.alipay.mobile.scan.arplatform:id/cover_click_button
    private void aliFuclickReplayBtn(AccessibilityEvent event) {
        Log.d(TAG, "aliFuclickReplayBtn() called with: event = [" + event + "]");
        Log.d(TAG, "aliFuclickReplayBtn() called with: event.getClassName() = [" + event.getClassName() + "]");
        if ("android.app.Dialog".equals(event.getClassName())) {

            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            List<AccessibilityNodeInfo> sendButtonInfo = nodeInfo.findAccessibilityNodeInfosByViewId("com.alipay.mobile.scan.arplatform:id/lucky_button");
            if (!sendButtonInfo.isEmpty()) {
                Toast.makeText(this, "自动帮你点击[再来一次]", Toast.LENGTH_SHORT).show();
                sendButtonInfo.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            sendButtonInfo = nodeInfo.findAccessibilityNodeInfosByViewId("com.alipay.android.phone.wallet.roosteryear:id/cr_receive");
            if (!sendButtonInfo.isEmpty()) {
                Toast.makeText(this, "自动帮你点击[收下福卡]", Toast.LENGTH_SHORT).show();
                sendButtonInfo.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            sendButtonInfo = nodeInfo.findAccessibilityNodeInfosByViewId("com.alipay.mobile.scan.arplatform:id/cover_click_button");
            if (!sendButtonInfo.isEmpty()) {
                Toast.makeText(this, "自动帮你点击[点击重试]", Toast.LENGTH_SHORT).show();
                sendButtonInfo.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    /**
     * 打开通知栏消息
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openNotification(AccessibilityEvent event) {
        if (event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
            return;
        }
        //以下是精华，将微信的通知栏消息打开
        Notification notification = (Notification) event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开QQ红包
     *
     * @param event
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openEnvelopeQQ(AccessibilityEvent event) {
        CharSequence className = event.getClassName();
        Log.w(TAG, "curClassName:" + className);
        if (QQ_ACTIVITY_CHAT.equals(className)) {
            //在聊天界面,去点中红包
            ///主界面中,打开对话框
            Log.d(TAG, "聊天界面---->点击红包" + event);
            chaikaihongbao();
        } else if (QQ_ACTIVITY_OPENED.equals(className)) {
            Log.d(TAG, "领取成功/已被领完" + event);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "延迟两秒返回home 上锁");
                    returnHome();
                }
            }, 2000);
        }
    }

    private boolean isAutoOpen = false;
    private Handler mHandler = new Handler();


    private void returnHome() {
        Log.d(TAG, "返回home 上锁 isAutoOpen:" + isAutoOpen + ",  isWaked:" + isWaked);
        if (isAutoOpen) {
            isAutoOpen = false;
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        }

        if (isWaked) {
            isWaked = false;
            wakeAndUnlock(false);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void chaikaihongbao() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
        /**
         * 领取别人发的红包
         * 先搜索当前界面的  点击拆开 字样，然后模拟点击事件打开红包
         * 如果找不到 领取红包 字符串，则可能是在主界面 尝试搜索 [微信红包] 字串
         */
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(QQ_ACTION_OPEN_OTHER);
        if (list.isEmpty()) {
            //如果没有找到点击拆开字样，说明是口令红包，则寻找口令红包字样：
            list = nodeInfo.findAccessibilityNodeInfosByText(QQ_ACTION_OPEN_KOULING);
            for (int i = list.size() - 1; i >= 0; i--) {
                AccessibilityNodeInfo parent = list.get(i).getParent();
                Log.i(TAG, "-->口令红包:" + parent);
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                Log.i(TAG, "-->点击输入口令:");
                List<AccessibilityNodeInfo> koulingList = nodeInfo.findAccessibilityNodeInfosByText(QQ_ACTION_CLICK_KOULING);
                if (!koulingList.isEmpty()) {
                    //找到口令红包弹出框，点击
                    koulingList.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    //点击发送:
                    List<AccessibilityNodeInfo> sendButtonInfo = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mobileqq:id/fun_btn");
                    if (!sendButtonInfo.isEmpty()) {
                        sendButtonInfo.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        isAutoOpen = true;
                    }
                }

                break;
            }
        } else {
            //最新的红包领起
            for (int i = list.size() - 1; i >= 0; i--) {
                AccessibilityNodeInfo parent = list.get(i).getParent();
                Log.i(TAG, "-->领取红包  i:" + i + " ,parent:" + parent);

                if (parent != null) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    isAutoOpen = true;
                }
            }
        }
    }

    /**
     * 解锁亮屏相关
     */
    PowerManager pm;
    PowerManager.WakeLock wl;
    KeyguardManager km;
    KeyguardManager.KeyguardLock kl;
    boolean isWaked;

    private void wakeAndUnlock(boolean b) {
        Log.i(TAG, "解锁亮屏？：" + b);
        if (b) {
            //获取电源管理器对象
            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
            wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
            //点亮屏幕
            wl.acquire();
            //得到键盘锁管理器对象
            km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            kl = km.newKeyguardLock("unLock");
            //解锁
            kl.disableKeyguard();
            isWaked = true;
        } else {
            //锁屏
            kl.reenableKeyguard();
            //释放wakeLock，关灯
            wl.release();
        }
    }
/**
 * 判断一下是否已经被抢完了 抢完了就返回
 */
        /*List<AccessibilityNodeInfo> list7 = nodeInfo.findAccessibilityNodeInfosByText(ACTION_BACK_NO_MONEY);
        if (list7 != null && list7.size() != 0) {
            Log.e(TAG, "[红包被抢完],手动返回");
            performGlobalAction(GLOBAL_ACTION_BACK);
        }*/

    /**************************
     * 微信红包相关
     **************************/

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openEnvelopeWx(AccessibilityEvent event) {
        if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {
            //点中了红包，下一步就是去拆红包
            openEnvelopeWx();
        } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {
            //拆完红包后看详细的纪录界面
            Log.i(TAG, "[拆完红包]/已被领完" + event);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "延迟两秒返回home 上锁");
                    returnHome();
                }
            }, 2000);
        } else if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
            //在聊天界面,去点中红包
            ///主界面中,打开对话框
            clickEnvelopeWx();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openEnvelopeWx() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
        List<AccessibilityNodeInfo> list6 = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/b43");
        Log.w(TAG, "b43list:" + list6);
        for (AccessibilityNodeInfo n : list6) {
            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            isAutoOpen = true;
        }
        /**
         * 判断一下是否已经被抢完了 抢完了就返回
         */
        List<AccessibilityNodeInfo> list7 = nodeInfo.findAccessibilityNodeInfosByText(WX_ACTION_BACK_NO_MONEY);
        if (list7 != null && list7.size() != 0) {
            Log.e(TAG, "[红包被抢完],手动返回");
            performGlobalAction(GLOBAL_ACTION_HOME);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void clickEnvelopeWx() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
        /**
         * 领取别人发的红包
         * 先搜索当前界面的  领取红包 字样，然后模拟点击事件打开红包
         * 如果找不到 领取红包 字符串，则可能是在主界面 尝试搜索 [微信红包] 字串
         */
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(WX_ACTION_OPEN_OTHER);
        if (list.isEmpty()) {
            list = nodeInfo.findAccessibilityNodeInfosByText(WX_ENVELOPE_TEXT_KEY);
            for (AccessibilityNodeInfo n : list) {
                Log.i(TAG, "-->微信红包:" + n);
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        } else {
            boolean isShouldExit = false;
            //最新的红包领起
            for (int i = list.size() - 1; i >= 0; i--) {
                AccessibilityNodeInfo parent = list.get(i).getParent();
                Log.i(TAG, "-->领取红包  i:" + i + " ,parent:" + parent);
                if (parent != null) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    //点击一个红包后就跳出循环
                    break;
                }
            }
        }
    }

    /**
     * 寻找钉钉的红包
     *
     * @param event
     */
    private void findDingDingGift(AccessibilityEvent event) {
        if (DINGDING_ACTIVITY_MAIN.equals(event.getClassName())) {//点击进入聊天页面
            enterChatHasGift();
        } else if (DINGDING_ACTIVITY_CHAT.equals(event.getClassName())) {//聊天页面寻找红包点开
            findGiftInChat();

        } else if (DINGDING_ACTIVITY_PICK_GIFT.equals(event.getClassName())) {//红包页面 寻找拆红包按钮
            openGift();
        }
    }

    /**
     * 递归遍历点击ImageButton
     *
     * @param nodeInfo
     */
    private void recursiveNodeInfo(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            Log.d(TAG, "recursiveNodeInfo() called with: nodeInfo = [" + nodeInfo + "]");
            if ("android.widget.ImageButton".equals(nodeInfo.getClassName())) {//点击ImageButton
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            for (int i = 0; i < nodeInfo.getChildCount(); i++) {//遍历子View
                AccessibilityNodeInfo child = nodeInfo.getChild(i);
                recursiveNodeInfo(child);
            }
        }

    }

    private void openGift() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
        //recursiveNodeInfo(nodeInfo);
        Log.d(TAG, "openGift() called 开始模拟cmd adb");
        execCmd("adb shell input swipe 360 990 360 991");
    }

    /**
     * Shell命令封装类
     *
     * @param cmd Shell命令
     */
    public void execCmd(String cmd) {
        System.out.println("ExecCmd:" + cmd);
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            // 执行成功返回流
            InputStream input = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    input, "GBK"));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            // 执行失败返回流
            InputStream errorInput = p.getErrorStream();
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(errorInput, "GBK"));
            String eline;
            while ((eline = errorReader.readLine()) != null) {
                System.out.println(eline);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void findGiftInChat() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(DINGDING_ACTION_SEE_GIFT);
        if (list != null) {
            for (AccessibilityNodeInfo n : list) {
                //点击父控件 ListView的Item
                AccessibilityNodeInfo parent = n.getParent();
                Log.i(TAG, "-->聊天界面寻找钉钉红包:" + n);
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        }
    }

    private void enterChatHasGift() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(DINGDING_ENVELOPE_TEXT_KEY);
        if (list != null) {
            for (AccessibilityNodeInfo n : list) {
                //点击父控件 ListView的Item
                AccessibilityNodeInfo parent = n.getParent();
                Log.i(TAG, "-->主界面寻找钉钉红包:" + n);
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        }
    }

}