package com.hsproduce.activity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.VPlanAdapter;
import com.hsproduce.bean.VPlan;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;
import com.xuexiang.xui.widget.progress.loading.MiniLoadingView;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;

/**
 * 成型胎胚报废页面
 * 扫描条码，报废条码成功后写入扫描记录中，广播监听响应事件
 * createBy zhangzhr @ 2019-12-21
 */
public class FormingBarCodeActivity extends BaseActivity {

    //轮胎条码 条码计数  条码记录
    private TextView tvBarCode, tvNum, tvBarCodeLog;
    private ButtonView btGetplan;
    //声明一个long类型变量：用于存放上一点击“返回键”的时刻
    private long mExitTime = 0;
    //绑定条码个数
    private int number = 0;
    //轮胎条码
    private String barCode = "", planid = "";
    private List<String> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_formingbarcode);
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
        //扫描框
        tvBarCode = (TextView) findViewById(R.id.barCode);
        //获得焦点
        tvBarCode.requestFocus();
        //条码记录
        tvBarCodeLog = (TextView) findViewById(R.id.barcode_log);
        //不可编辑
        tvBarCodeLog.setFocusable(false);
        tvBarCodeLog.setFocusableInTouchMode(false);
        //扫描条码计数
        tvNum = (TextView) findViewById(R.id.num);
        //获取计划按钮
        btGetplan = (ButtonView) findViewById(R.id.getplan);
    }

    //初始化事件
    public void initEvent() {
        //报废
        btGetplan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取计划
                barCode = tvBarCode.getText().toString().trim();
                getBarCode(barCode);
            }
        });
    }


    //获取轮胎条码  广播监听响应时间
    public void getBarCode(String barCode) {
        //获取轮胎上barcode
        if (StringUtil.isNullOrEmpty(barCode)) {
            return;
        } else {
            String param1 = "ScrapCode=" + barCode + "&FormingID=" + "&User_Name=" + App.username + "&TEAM=" + App.shift;
            new TypeCodeTask().execute(param1);
            tvBarCode.requestFocus();
        }
    }

    //获取轮胎条码  按键监听响应时间
    public void getBarCode() {
        //获取轮胎上barcode
        barCode = tvBarCode.getText().toString().trim();
        if (StringUtil.isNullOrEmpty(barCode)) {
            return;
        } else {
            String param1 = "ScrapCode=" + barCode + "&FormingID=" + "&User_Name=" + App.username + "&TEAM=" + App.shift;
            new TypeCodeTask().execute(param1);
            tvBarCode.requestFocus();
        }
    }

    //广播监听
    private BroadcastReceiver scanDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SCN_CUST_ACTION_SCODE)) {
                try {
                    if (!StringUtil.isNullOrEmpty(barCode)) {
                        barCode = "";
                    }
                    barCode = intent.getStringExtra(SCN_CUST_EX_SCODE);
                    //判断条码是否为空
                    if (!StringUtil.isNullOrEmpty(barCode)) {
                        getBarCode(barCode);
                    } else {
                        Toast.makeText(FormingBarCodeActivity.this, "请重新扫描", Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ScannerService", e.toString());
                }
            }
        }
    };

    //成型胚胎报废
    class TypeCodeTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.FORMINGBARCODE, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingBarCodeActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingBarCodeActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //判断用户是否重复输入
                        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                        list.add("[" + date + "]" + barCode + "报废成功");
                        tvBarCodeLog.setText("");
                        System.out.println(list.size());
                        for (int i = 0; i < list.size(); i++) {
                            if (i == 0) {
                                tvBarCodeLog.setText(list.get(i));
                            } else {
                                tvBarCodeLog.setText(getlog(list));
                            }
                        }
                        tvNum.setText("");
                        number++;//计算成功次数
                        tvNum.setText(number + "");
//                        Toast.makeText(FormingBarCodeActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(FormingBarCodeActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingBarCodeActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //递归显示
    public String getlog(List<String> list) {
        String logstr = "";
        Collections.reverse(list);//倒序
        for (int i = 0; i < list.size(); i++) {
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
        //按键按下
        switch (keyCode) {
            case 0:
                tvBarCode.requestFocus();
                tvBarCode.setText("");
                break;
        }

        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //按键弹开
        switch (keyCode) {
//            case 0://扫描键
//                getBarCode();//获取明细
//                break;
            case 22://右方向键
                getBarCode();//获取明细
                break;
            case 4:
                tofunction();
                tvNum.setText("0");
                break;
        }
        return true;
    }
}
