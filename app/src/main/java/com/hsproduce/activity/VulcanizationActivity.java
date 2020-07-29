package com.hsproduce.activity;

import android.annotation.SuppressLint;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputFilter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.VPlanAdapter;
import com.hsproduce.bean.VPlan;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.SoundPlayUtils;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import java.util.*;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;

/**
 * 硫化生产页面
 * 扫描机台号直接进入扫描条码页面，扫描条码，硫化计划更改为生产中，并将扫描的条码添加进生产实绩中
 * 成功添加进生产实绩后此条码写入扫描纪录框中
 * createBy zhangzhr @ 2019-12-21
 */
public class VulcanizationActivity extends BaseActivity {

    //计划展示list
    private ListView listView;
    //机台号  轮胎条码 条码计数  条码记录
    private TextView tvScan, tvAnum, tvBarCodeLog, tvSum, tvMchId;
    //计划展示适配器
    private VPlanAdapter adapter;
    //绑定条码个数
    private int number = 0;
    //轮胎条码
    private String barCode = "", planId = "";
    //添加条码防止重复扫描
    private List<String> codeList = new ArrayList<>();
    //判断弹窗是否已经存在
    private MaterialDialog materialDialog = null;
    //判断是否已经扫描过机台号
    private Boolean isScan = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_vulcanization);
        //加载控件
        initView();
        //设置控件事件
        initEvent();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onResume() {
        //注册广播监听
        IntentFilter intentFilter = new IntentFilter(SCN_CUST_ACTION_SCODE);
        registerReceiver(scanDataReceiver, intentFilter);
        super.onResume();
    }

    //初始化控件
    public void initView() {
        //list列表
        listView = (ListView) findViewById(R.id.lv_plan);
        //条码扫描框
        tvScan = (TextView) findViewById(R.id.barcode);
        //条码记录
        tvBarCodeLog = (TextView) findViewById(R.id.barcode_log);
        tvBarCodeLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        //不可编辑
        tvBarCodeLog.setFocusable(false);
        tvBarCodeLog.setFocusableInTouchMode(false);
        //扫描条码计数
        tvAnum = (TextView) findViewById(R.id.anum);
        //机台本班次累计数量
        tvSum = (TextView) findViewById(R.id.sum);
        //设置扫描框输入字符数
        tvScan.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        //机台号
        tvMchId = (TextView) findViewById(R.id.mchId);
    }


    //初始化事件
    public void initEvent() {

    }

    //获取计划
    public void getPlan(String mchid) {
        if (!StringUtil.isNullOrEmpty(tvMchId.getText().toString().trim())) {
            tvMchId.setText("");
        }
        if (StringUtil.isNullOrEmpty(mchid)) {
            Toast toast = Toast.makeText(VulcanizationActivity.this, "请扫描机台号", Toast.LENGTH_LONG);
            showMyToast(toast, 500);
            tvScan.setText("");
            return;
        } else {
            mchid = mchid.toUpperCase();
            tvMchId.setText(mchid);
            String param = "MCHIDLR=" + mchid + "&SHIFT=" + App.shift + "&USER_NAME=" + App.username;
            new MyTask().execute(param);
        }
    }

    //获取轮胎条码
    public void getBarCode(String barcode) {
        //获取轮胎上barcode
        barCode = barcode;
        if (StringUtil.isNullOrEmpty(barCode)) {
            Toast toast = Toast.makeText(VulcanizationActivity.this, "请扫描轮胎条码", Toast.LENGTH_LONG);
            showMyToast(toast, 500);
            return;
        } else {
            //扫描记录中是否已经存在该条码，存在提示已扫描，不存在调用接口记录硫化记录
            if (codeList.contains(barCode)) {
                Toast toast = Toast.makeText(VulcanizationActivity.this, "此条码已经扫描", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                tvScan.setText("");
                return;
            } else {
                if (isScan) {
                    //记录硫化生产记录
                    String param1 = "PLAN_ID=" + planId + "&barcode=" + barCode + "&User_Name=" + App.username + "&TEAM=" + App.shift + "&doit=0";
                    new TypeCodeTask().execute(param1);
                } else {
                    Toast toast = Toast.makeText(VulcanizationActivity.this, "请先扫描机台", Toast.LENGTH_LONG);
                    showMyToast(toast, 500);
                    return;
                }
            }
        }
    }

    //广播监听
    private BroadcastReceiver scanDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SCN_CUST_ACTION_SCODE)) {
                try {
                    String barCode = "";
                    barCode = intent.getStringExtra(SCN_CUST_EX_SCODE);
                    //判断条码是否为空
                    if (!StringUtil.isNullOrEmpty(barCode)) {
                        //判断位数和是否是纯数字
                        if (barCode.length() == 12 && isNumeric(barCode) == true) {
                            getBarCode(barCode);
                        } else if (barCode.length() == 4 && isNumeric(barCode) == false) {
                            getPlan(barCode);
                        } else {
                            Toast toast = Toast.makeText(VulcanizationActivity.this, "请重新扫描", Toast.LENGTH_LONG);
                            showMyToast(toast, 500);
                            return;
                        }

                    } else {
                        Toast toast = Toast.makeText(VulcanizationActivity.this, "请重新扫描", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ScannerService", e.toString());
                }
            }
        }
    };

    //查询任务
    class MyTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.VUL_GET_PLAN, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast toast = Toast.makeText(VulcanizationActivity.this, "网络连接异常", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VPlan> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VPlan>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast toast = Toast.makeText(VulcanizationActivity.this, "未获取到数据", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //清空数据
                        planId = "";
                        codeList.clear();
                        tvScan.setText("");
                        tvBarCodeLog.setText("");
                        tvAnum.setText("0");
                        tvSum.setText("0");
                        number = 0;
                        //获取计划ID
                        planId = datas.get(0).getId();
                        //展示列表
                        adapter = new VPlanAdapter(VulcanizationActivity.this, datas);
                        listView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        number = Integer.valueOf(datas.get(0).getDnum());
                        tvSum.setText(datas.get(0).getDnum());
                        //已扫描机台号且有计划
                        isScan = true;
                    } else {
                        //清空数据
                        planId = "";
                        number = 0;
                        tvAnum.setText("0");
                        tvSum.setText("0");
                        tvBarCodeLog.setText("");
                        tvScan.setText("");
                        isScan = false;
                        codeList.clear();
                        datas.clear();
                        adapter = new VPlanAdapter(VulcanizationActivity.this, datas);
                        listView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        Toast toast = Toast.makeText(VulcanizationActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(VulcanizationActivity.this, "数据处理异常", Toast.LENGTH_LONG);
                    showMyToast(toast, 500);
                    return;
                }

            }
        }
    }

    //新增硫化生产记录
    class TypeCodeTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.VUL_AddActualAchievement, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast toast = Toast.makeText(VulcanizationActivity.this, "网络连接异常", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast toast = Toast.makeText(VulcanizationActivity.this, "未获取到数据", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        tvBarCodeLog.append(barCode + "\n");
                        tvScan.setText("");
                        codeList.add(barCode);
                        tvAnum.setText(codeList.size() + "");
                        tvSum.setText((number + codeList.size()) + "");
                    } else if (res.get("code").equals("100")) {
                        Toast toast = Toast.makeText(VulcanizationActivity.this, "扫描条码位数不正确", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    } else if (res.get("code").equals("300")) {
                        Toast toast = Toast.makeText(VulcanizationActivity.this, barCode + ":" + res.get("msg") + "", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    } else if (res.get("code").equals("400")) {
                        //提示音
                        SoundPlayUtils.playSoundByMedia(VulcanizationActivity.this, R.raw.raw3);
                        //提示音
                        SoundPlayUtils.startNoti(VulcanizationActivity.this);
                        SoundPlayUtils.stopAlarm();

                        if (materialDialog == null) {
                            materialDialog = new MaterialDialog.Builder(VulcanizationActivity.this)
                                    .title("提示")
                                    .content(res.get("msg") + "")
                                    .positiveText(R.string.vul_confirm)
                                    .negativeText(R.string.vul_cancel)
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            //强制记录硫化生产记录
//                                        iscomplate = false;
                                            String param1 = "PLAN_ID=" + planId + "&barcode=" + barCode + "&User_Name=" + App.username + "&TEAM=" + App.shift + "&doit=1";
                                            new TypeCodeTask().execute(param1);
                                            //提示音
                                            SoundPlayUtils.startNoti(VulcanizationActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    })
                                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            //提示音
                                            SoundPlayUtils.startAlarm(VulcanizationActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    })
                                    .cancelable(false)
                                    .show();
                        }else{
                            materialDialog.setContent(res.get("msg") + "");
                        }
                        materialDialog.show();

                    } else {
                        Toast toast = Toast.makeText(VulcanizationActivity.this, "错误，条码未识别！", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(VulcanizationActivity.this, "数据处理异常", Toast.LENGTH_LONG);
                    showMyToast(toast, 500);
                    return;
                }
            }
        }
    }

    //是否是纯数字
    public static boolean isNumeric(String str) {
        for (int i = str.length(); --i >= 0; ) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }


    //键盘监听
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("key", keyCode + "  ");
        switch (keyCode) {
            case 0:
                tvScan.setText("");
                tvScan.requestFocus();
                break;
        }
        return true;
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //扫描键 弹开时获取计划
        //右方向键
        String msg = "";
        switch (keyCode) {
            //返回键
            case 4:
                //返回功能页
                codeList.clear();
                tvBarCodeLog.setText("");
                tvScan.setText("");
                tvAnum.setText("0");
                tvSum.setText("0");
                isScan = false;
                startActivity(new Intent(VulcanizationActivity.this, FunctionActivity.class));
                this.finish();
                break;
            //右方向键
            case 22:
                operate();
                break;
            default:
                break;

        }
        return true;
    }

    private void operate() {
        String str = tvScan.getText().toString().trim();
        if (!StringUtil.isNullOrEmpty(str)) {
            //判断位数和是否是纯数字
            if (str.length() == 12 && isNumeric(str) == true) {
                getBarCode(str);
            } else if (str.length() == 4 && isNumeric(str) == false) {
                getPlan(str);
            } else {
                Toast toast = Toast.makeText(VulcanizationActivity.this, "请重新扫描", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            }

        } else {
            Toast toast = Toast.makeText(VulcanizationActivity.this, "请重新扫描", Toast.LENGTH_LONG);
            showMyToast(toast, 500);
            return;
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onPause() {
        unregisterReceiver(scanDataReceiver);
        super.onPause();
    }


}
