package com.can.trafficmonitor;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.can.trafficmonitor.adapter.AppTrafficAdapter;
import com.can.trafficmonitor.bean.AppInfo;
import com.can.trafficmonitor.utils.TextFormat;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.can.tvlib.ui.ToastUtils;
import cn.can.tvlib.ui.focus.FocusMoveUtil;

public class TrafficActivity extends AppCompatActivity implements View.OnFocusChangeListener {

    private static final int GET_APP_DATA = 1;
    private static final int REFRESH_APP_DATA = 2;
    private static final int REFRESH_TIME_DELAYED = 1000;

    private final int FOCUS_CHANGE_FOCUS_DELAY_TIME = 100;
    private final int ITEM_FOCUS_MOVE_DELAY_TIME = 100;
    private final int SHOW_STOP_BTN_DELAY_TIME = 300;
    public static final String ENTRY_KEY_WHITE_APP = "whiteApp";

    private long mLastReceived, mLastTransmitted;
    private BroadcastReceiver mHomeKeyReceiver;

    private TextView mTvReceivedSpeed, mTvTransmittedSpeed, mTvNoData;
    private ImageView mShadow;
    private Button mBtnStopView;
    private RecyclerView mAppRecyclerView;
    private List<AppInfo> mAppList;
    private Handler mHandler = new MyHandler(this);
    private Runnable mFocusMoveRunnable;
    private Runnable mShowStopBtnRunnable;
    private View mFocusedView;
    private AppTrafficAdapter mAdapter;
    private FocusMoveUtil mFocusMoveUtil;
    private Dialog mLoadingDialog;
    private PackageManager pm;
    private ActivityManager mActivityManager;
    private String[] customPkgArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customPkgArray = getIntent().getStringArrayExtra(ENTRY_KEY_WHITE_APP);
        setContentView(R.layout.com_can_trafficmonitor_activity_traffic);
        initView();
        initData();
    }

    /**
     * 启动方法
     * 参数1：上下文
     * 参数2：流量监控要过滤应用的白名单包名数组（没有传null）
     */
    public static void actionStart(Context context, String[] pkgArray) {
        Intent intent = new Intent(context, TrafficActivity.class);
        if (pkgArray != null && pkgArray.length > 0) {
            intent.putExtra(TrafficActivity.ENTRY_KEY_WHITE_APP, pkgArray);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onResume() {
        startHomeKeyListener();
        super.onResume();
    }

    private void initView() {
        mTvReceivedSpeed = (TextView) findViewById(R.id.tv_received);
        mTvTransmittedSpeed = (TextView) findViewById(R.id.tv_transmitted);
        mTvNoData = (TextView) findViewById(R.id.tv_no_data);
        mShadow = (ImageView) findViewById(R.id.iv_bottom_shadow);
        mFocusMoveUtil = new FocusMoveUtil(this, getWindow().getDecorView(), R.mipmap.com_can_trafficmonitor_btn_focus);
    }

    private void initData() {
        if (getTotalReceived() > 0) {
            showLoadingDialog();
            initFocusRunnable();
            initShowStopBtnRunnable();
            getTrafficMonitorAppList();
        } else {
            mTvNoData.setVisibility(View.VISIBLE);
        }
    }

    private void initFocusRunnable() {
        mFocusMoveRunnable = new Runnable() {
            @Override
            public void run() {
                View focusedView = TrafficActivity.this.mFocusedView;
                if (focusedView == null || !focusedView.isFocused()) {
                    return;
                }
                mFocusMoveUtil.startMoveFocus(focusedView);
            }
        };
    }

    private void initShowStopBtnRunnable() {
        mShowStopBtnRunnable = new Runnable() {
            @Override
            public void run() {
                if (mBtnStopView != null) {
                    mBtnStopView.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    private static class MyHandler extends Handler {
        private final WeakReference<TrafficActivity> mActivity;

        private MyHandler(TrafficActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mActivity.get() == null) {
                return;
            }
            TrafficActivity trafficActivity = mActivity.get();
            switch (msg.what) {
                case GET_APP_DATA:
                    trafficActivity.hideLoadingDialog();
                    if (trafficActivity.mAppList.size() == 0) {
                        trafficActivity.mTvNoData.setVisibility(View.VISIBLE);
                        trafficActivity.mTvNoData.setText(trafficActivity.getResources().getString(R.string
                                .no_traffic_app));
                        return;
                    }
                    trafficActivity.refreshData();
                    trafficActivity.initAdapter();
                    trafficActivity.initRecyclerView();
                    break;
                case REFRESH_APP_DATA:
                    trafficActivity.mAdapter.setListData(trafficActivity.mAppList);
                    trafficActivity.refreshData();
                    break;
            }
        }
    }

    private void getTrafficMonitorAppList() {
        pm = getPackageManager();
        new Thread() {
            @Override
            public void run() {
                List<PackageInfo> installedPackages = pm.getInstalledPackages(0);
                List<AppInfo> list = new ArrayList<>();
                for (PackageInfo packageInfo : installedPackages) {
                    ApplicationInfo application = packageInfo.applicationInfo;
                    String[] appWhiteList = AppConstants.APP_WHITE_LIST;
                    if (customPkgArray != null && customPkgArray.length > 0) {
                        appWhiteList = mergeWhiteAppArray(appWhiteList, customPkgArray);
                    }
                    if (!Arrays.asList(appWhiteList).contains(application.packageName) && (application.flags &
                            ApplicationInfo.FLAG_SYSTEM) == 0 && PackageManager.PERMISSION_GRANTED
                            == pm.checkPermission("android" +
                            ".permission.INTERNET", application.packageName)) {
                        AppInfo app = new AppInfo();
                        app.setPackageName(packageInfo.packageName);
                        app.setName(application.loadLabel(pm).toString());
                        app.setDrawable(application.loadIcon(pm));
                        int uid = application.uid;
                        app.setuId(uid);
                        app.setReceived(getAppReceived(uid));
                        app.setTransmitted(getAppTransmitted(uid));
                        list.add(app);
                    }
                }
                Collections.sort(list, new Comparator<AppInfo>() {
                    public int compare(AppInfo arg0, AppInfo arg1) {
                        if (arg0.getReceived() >= arg1.getReceived()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });
                mAppList = new ArrayList<>();
                mAppList = list;
                mLastReceived = getTotalReceived();
                mLastTransmitted = getTotalTransmitted();
                mHandler.sendEmptyMessage(GET_APP_DATA);
            }
        }.start();
    }

    private String[] mergeWhiteAppArray(String[] ary1, String[] ary2) {
        String[] array = new String[ary1.length + ary1.length];
        System.arraycopy(ary1, 0, array, 0, ary1.length);
        System.arraycopy(ary2, 0, array, ary1.length, ary2.length);
        return array;
    }

    private void refreshData() {
        mTvReceivedSpeed.setText(this.getResources().getString(R.string.received) + TextFormat.formatByte(
                (getTotalReceived() - mLastReceived), this) + this.getResources().getString(R.string.unit_second));
        mTvTransmittedSpeed.setText(this.getResources().getString(R.string.transmitted) + TextFormat.formatByte(
                (getTotalTransmitted() - mLastTransmitted), this) + this.getResources().getString(R.string
                .unit_second));
        mLastReceived = getTotalReceived();
        mLastTransmitted = getTotalTransmitted();
        for (int i = 0; i < mAppList.size(); i++) {
            AppInfo appInfo = mAppList.get(i);
            appInfo.setReceived(getAppReceived(appInfo.getuId()));
            mAppList.set(i, appInfo);
        }
        mHandler.sendEmptyMessageDelayed(REFRESH_APP_DATA, REFRESH_TIME_DELAYED);
    }

    private void initAdapter() {
        mAdapter = new AppTrafficAdapter(mAppList, this);
        mAdapter.setHasStableIds(true);
        mAdapter.setFocusListener(this);
        mAdapter.setOnItemFocusChangeListener(new AppTrafficAdapter.OnItemFocusChangeListener() {
            @Override
            public void onItemFocusChange(View view, Button stopBtn, TextView title, int position) {
                if (view.hasFocus() || stopBtn.hasFocus()) {
                    mBtnStopView = stopBtn;
                    view.setSelected(true);
                    title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    if (isHasRootPermission()) {
                        mHandler.removeCallbacks(mShowStopBtnRunnable);
                        mHandler.postDelayed(mShowStopBtnRunnable, SHOW_STOP_BTN_DELAY_TIME);
                    }
                } else {
                    view.setSelected(false);
                    title.setEllipsize(TextUtils.TruncateAt.END);
                    if (isHasRootPermission()) {
                        stopBtn.setVisibility(View.INVISIBLE);
                    }
                }
            }

            @Override
            public void onStopBtnFocusChange(View view, Button stopBtn, TextView title, int position) {
                if (!view.hasFocus()) {
                    view.setVisibility(View.INVISIBLE);
                    title.setSelected(false);
                    title.setEllipsize(TextUtils.TruncateAt.END);
                }
            }
        });
        mAdapter.setOnItemStopButClickListener(new AppTrafficAdapter.OnItemStopButClickListener() {
            @Override
            public void onStopBtnClick(int position) {
                AppInfo app = mAppList.get(position);
                setPackageForceStop(app.getPackageName());
                ToastUtils.showMessage(TrafficActivity.this, app.getName() + getString(R.string.stop_app));
            }
        });
    }

    private void setPackageForceStop(String pkg) {
        try {
            Method method = Class.forName("android.app.ActivityManager").getMethod("forceStopPackage", String.class);
            if (mActivityManager == null) {
                mActivityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            }
            method.invoke(mActivityManager, pkg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isHasRootPermission() {
        return getApplicationInfo().uid == 1000;
    }

    private void initRecyclerView() {
        hideLoadingDialog();
        mAppRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_traffic);
        mAppRecyclerView.setVisibility(View.VISIBLE);
        mShadow.setVisibility(View.VISIBLE);
        mAppRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mAppRecyclerView.getChildAt(0).requestFocus();
                mFocusMoveUtil.setFocusView(mAppRecyclerView.getChildAt(0));
            }
        });
        LinearLayoutManager mllManager = new LinearLayoutManager(this);
        mAppRecyclerView.setLayoutManager(mllManager);
        mAppRecyclerView.setItemAnimator(null);
        mAppRecyclerView.setAdapter(mAdapter);
        mAppRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mHandler.postDelayed(mFocusMoveRunnable, ITEM_FOCUS_MOVE_DELAY_TIME);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mHandler.removeCallbacks(mFocusMoveRunnable);
                if (dy == 0) {
                    mHandler.post(mFocusMoveRunnable);
                }
            }
        });
    }

    private void showLoadingDialog() {
        if (mLoadingDialog == null) {
            mLoadingDialog = cn.can.tvlib.ui.LoadingDialog.showLoadingDialog(this, getResources()
                    .getDimensionPixelSize(R.dimen.px136));
        } else if (!mLoadingDialog.isShowing()) {
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.show();
        }
    }

    private void hideLoadingDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    private long getTotalReceived() {
        return TrafficStats.getTotalRxBytes();
    }

    private long getTotalTransmitted() {
        return TrafficStats.getTotalTxBytes();
    }

    private long getAppReceived(int uid) {
        return TrafficStats.getUidRxBytes(uid);
    }

    private long getAppTransmitted(int uid) {
        return TrafficStats.getUidTxBytes(uid);
    }


    private void startHomeKeyListener() {
        if (mHomeKeyReceiver == null) {
            mHomeKeyReceiver = new HomeKeyReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mHomeKeyReceiver, intentFilter);
    }

    /**
     * Home键监听
     */
    protected void onHomeKeyDown() {
        finish();
    }

    class HomeKeyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                onHomeKeyDown();
            }
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            mFocusedView = v;
            mHandler.removeCallbacks(mFocusMoveRunnable);
            mHandler.postDelayed(mFocusMoveRunnable, FOCUS_CHANGE_FOCUS_DELAY_TIME);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        hideLoadingDialog();
    }

    @Override
    protected void onDestroy() {
        if (mHomeKeyReceiver != null) {
            unregisterReceiver(mHomeKeyReceiver);
        }
        if (mFocusMoveUtil != null) {
            mFocusMoveUtil.release();
            mFocusMoveUtil = null;
        }
        if (mAdapter != null) {
            mAdapter.setOnItemFocusChangeListener(null);
            mAdapter.setFocusListener(null);
            mAdapter = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mAppList != null) {
            mAppList.clear();
            mAppList = null;
        }
        super.onDestroy();
    }
}
