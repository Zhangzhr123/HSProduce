package com.hsproduce.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;

/**
 * 退厂扫描页面
 * 扫描条码将条码从装车单明细中删除
 * creatBy zhangzhr @ 2020-01-07
 * 1.扫描改为广播监听方式扫描
 */
public class LoadScanningActivity extends BaseActivity {

    //声明控件  扫描条码  记录条码  条码计数
    private TextView tvBarCode, tvBarCodeLog, tvAnum;
    //退厂扫描按钮测试
    private ButtonView btGetCode;
    //声明一个long类型变量：用于存放上一点击“返回键”的时刻
    private long mExitTime = 0;
    //定义变量   退厂条码
    private String scanBarCode = "";
    private List<String> list = new ArrayList<>();
    //条码计数初始值
    private int number = 0;
    //添加条码防止重复扫描
    private List<String> codeList = new ArrayList<>();
    private Boolean isNew = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_loadscanning);
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

    public void initView() {
        //条码扫描框
        tvBarCode = (TextView) findViewById(R.id.scan_barcode);
        //焦点扫描框
        tvBarCode.requestFocus();
        //条码记录
        tvBarCodeLog = (TextView) findViewById(R.id.barcode_log);
        //不可编辑
        tvBarCodeLog.setFocusable(false);
        tvBarCodeLog.setFocusableInTouchMode(false);
        //扫描个数
        tvAnum = (TextView) findViewById(R.id.anum);
        //按钮测试用
        btGetCode = (ButtonView) findViewById(R.id.bt_getCode);
        tvBarCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
    }

    public void initEvent() {
        //测试按钮
        btGetCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!StringUtil.isNullOrEmpty(scanBarCode)) {
                    scanBarCode = "";
                }
                scanBarCode = tvBarCode.getText().toString().trim();
                outVLoad(scanBarCode);
            }
        });
    }

    //退厂扫描
    public void outVLoad(String barCode) {
        //退厂扫描条码
        //判断是否为空
        if (StringUtil.isNullOrEmpty(barCode)) {
            Toast toast = Toast.makeText(LoadScanningActivity.this, "请扫描轮胎条码", Toast.LENGTH_LONG);
            showMyToast(toast, 500);
            return;
        } else {
            if (codeList.contains(barCode)) {
                isNew = false;
            }

            if (isNew) {
                if (barCode.length() == 12 && isNum(barCode) == true) {
                    scanBarCode = barCode;
                    String parm = "TYRE_CODE=" + barCode + "&USER_NAME=" + App.username;
                    new OutsVLoadTask().execute(parm);
                } else {
                    Toast toast = Toast.makeText(LoadScanningActivity.this, "条码不正确，请重新扫描", Toast.LENGTH_LONG);
                    showMyToast(toast, 500);
                    return;
                }
            } else {
                isNew = true;
                Toast toast = Toast.makeText(LoadScanningActivity.this, "此条码已经扫描", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
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
                        if (barCode.length() == 12 && isNum(barCode) == true) {
                            outVLoad(barCode);
                        } else {
                            Toast toast = Toast.makeText(LoadScanningActivity.this, "条码不正确，请重新扫描", Toast.LENGTH_LONG);
                            showMyToast(toast, 500);
                            return;
                        }
                    } else {
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ScannerService", e.toString());
                }
            }
        }
    };

    public Boolean isNum(String s) {
        char[] ch = s.toCharArray();
        for (char c : ch) {
            if (!(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    //退厂扫描
    class OutsVLoadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.OutsVLOAD, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast toast = Toast.makeText(LoadScanningActivity.this, "网络连接异常", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast toast = Toast.makeText(LoadScanningActivity.this, "未获取到数据，数据返回异常", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        codeList.add(scanBarCode);
                        //显示绑定条码数量
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        list.add("[" + date + "]" + scanBarCode);
                        tvBarCodeLog.setText("");
                        for (int i = 0; i < list.size(); i++) {
                            if (i == 0) {
                                tvBarCodeLog.setText(list.get(i));
                            } else {
                                tvBarCodeLog.setText(getlog(list));
                            }
                        }
                        tvAnum.setText("");
                        number++;
                        tvAnum.setText(number + "");
                    } else {
                        Toast toast = Toast.makeText(LoadScanningActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(LoadScanningActivity.this, "数据处理异常", Toast.LENGTH_LONG);
                    showMyToast(toast, 500);
                    return;
                }
            }
        }
    }

    //递归显示
    public String getlog(List<String> list) {
        String logstr = "";
        for (int i = list.size() - 1; i >= 0; i--) {
            logstr += list.get(i) + "\n";
        }
        return logstr;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onPause() {
        unregisterReceiver(scanDataReceiver);
        super.onPause();
    }

    //键盘监听
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("key", keyCode + "  ");
        //扫描键 按下时清除
        if (keyCode == 0) {
            tvBarCode.setText("");
            tvBarCode.requestFocus();
        }
        return true;
    }

    //按键弹开
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //弹开时执行操作
        if (keyCode == 22) {
            if (!StringUtil.isNullOrEmpty(scanBarCode)) {
                scanBarCode = "";
            }
            scanBarCode = tvBarCode.getText().toString().trim();
            outVLoad(scanBarCode);
        }
        //返回键返回功能页面
        if (keyCode == 4) {
            tvAnum.setText("0");
            number = 0;
            tvBarCodeLog.setText("");
            list.clear();
            codeList.clear();
            scanBarCode = "";
            tofunction();
        }
        return true;
    }

}
