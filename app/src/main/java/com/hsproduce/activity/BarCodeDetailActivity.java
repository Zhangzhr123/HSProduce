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
import android.widget.*;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.FormingItemAdapter;
import com.hsproduce.bean.VPlan;
import com.hsproduce.bean.VreCord;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;

/**
 * 生产追溯页面
 * 扫描条码显示硫化条码明细和成型条码明细，使用广播监听扫描方式获取条码
 * createBy zahngzr @ 2019-12-20
 */
public class BarCodeDetailActivity extends BaseActivity {

    //轮胎条码
    private TextView tvBarCode;
    //获取计划按钮
    private ImageButton btGetPlan;
    //成型明细
    private TextView fSpesc, fSpescName, fMchId, fDate, fShift, fMaster, fState;
    //硫化明细
    private TextView vSpesc, vSpescName, vMchId, vLorR, vDate, vShift, vMaster, vState;
    //轮胎条码
    private List<VreCord> data1 = new ArrayList<>();
    private List<VreCord> data2 = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_barcodedetail);
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
        //扫描框
        tvBarCode = (TextView) findViewById(R.id.BarCode);
        //获得焦点
        tvBarCode.requestFocus();
        tvBarCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        //获取计划按钮
        btGetPlan = (ImageButton) findViewById(R.id.getBarCode);
        //成型
        fSpesc = (TextView) findViewById(R.id.fspesc);
        fSpescName = (TextView) findViewById(R.id.fspescname);
        fMchId = (TextView) findViewById(R.id.fmchid);
        fDate = (TextView) findViewById(R.id.fdate);
        fShift = (TextView) findViewById(R.id.fshift);
        fMaster = (TextView) findViewById(R.id.fmaster);
        fState = (TextView) findViewById(R.id.fstate);
        //硫化
        vSpesc = (TextView) findViewById(R.id.vspesc);
        vSpescName = (TextView) findViewById(R.id.vspescname);
        vMchId = (TextView) findViewById(R.id.vmchid);
        vLorR = (TextView) findViewById(R.id.LorR);
        vDate = (TextView) findViewById(R.id.vdate);
        vShift = (TextView) findViewById(R.id.vshift);
        vMaster = (TextView) findViewById(R.id.vmaster);
        vState = (TextView) findViewById(R.id.vstate);

    }

    public void initEvent() {
        //点击查询成型和硫化条码信息
        btGetPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String barCode = tvBarCode.getText().toString().trim();
                getBarCode(barCode);
            }
        });
    }

    //生产追溯 广播监听
    public void getBarCode(String barCode) {
        //获取输入机台上barcode
        if (StringUtil.isNullOrEmpty(barCode)) {
//            Toast.makeText(BarCodeDetailActivity.this, "请扫描轮胎条码", Toast.LENGTH_LONG).show();
            return;
        } else {
            String parm = "SwitchTYRE_CODE=" + barCode;
            new GetFormingDetail().execute(parm);
            new GetVulcanizaDetail().execute(parm);
        }
    }

    //生产追溯 按键监听
    public void getBarCode() {
        //获取输入机台上barcode
        String barCode = tvBarCode.getText().toString().trim();
        if (StringUtil.isNullOrEmpty(barCode)) {
//            Toast.makeText(BarCodeDetailActivity.this, "请扫描轮胎条码", Toast.LENGTH_LONG).show();
            return;
        } else {
            String parm = "SwitchTYRE_CODE=" + barCode;
            new GetFormingDetail().execute(parm);
            new GetVulcanizaDetail().execute(parm);
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
                    //判断条码是否为空 是否为12位 是否纯数字组成
                    if (!StringUtil.isNullOrEmpty(barCode) && barCode.length() == 12 && isNum(barCode) == true) {
                        getBarCode(barCode);
                    } else {
                        Toast.makeText(BarCodeDetailActivity.this, "请重新扫描", Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ScannerService", e.toString());
                }
            }
        }
    };


    //获取成型明细
    class GetFormingDetail extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.FORMINGSECLECTCODE, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            //清空
            fSpesc.setText("");
            fSpescName.setText("");
            fMchId.setText("");
            fDate.setText("");
            fShift.setText("");
            fMaster.setText("");
            fState.setText("");

            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(BarCodeDetailActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VreCord> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VreCord>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
//                        Toast.makeText(BarCodeDetailActivity.this, "未获取到数据", Toast.LENGTH_LONG).show();
                        return;
                    }
                    //赋值判断是否提示
                    data1.addAll(datas);
                    if (res.get("code").equals("200")) {
                        //成型明细
                        fSpesc.setText(datas.get(0).getItnbr());
                        fSpescName.setText(datas.get(0).getItdsc());
                        fMchId.setText(datas.get(0).getMchid());
                        fDate.setText(datas.get(0).getWdate().substring(0, 10));
                        fShift.setText(datas.get(0).getShift());
                        fMaster.setText(datas.get(0).getCreateuser());
                        fState.setText(datas.get(0).getiS_H());
                    } else {
//                        Toast.makeText(BarCodeDetailActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(BarCodeDetailActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }

            }
        }
    }

    //获取硫化明细
    class GetVulcanizaDetail extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.SelDetailed, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            //清空
            vSpesc.setText("");
            vSpescName.setText("");
            vMchId.setText("");
            vLorR.setText("");
            vDate.setText("");
            vShift.setText("");
            vMaster.setText("");
            vState.setText("");

            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(BarCodeDetailActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VreCord> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VreCord>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
//                        Toast.makeText(BarCodeDetailActivity.this, "未获取到数据", Toast.LENGTH_LONG).show();
                        return;
                    }
                    //赋值判断是否提示
                    data2.addAll(datas);

                    if (res.get("code").equals("200")) {
                        //硫化明细
                        vSpesc.setText(datas.get(0).getItnbr());
                        vSpescName.setText(datas.get(0).getItdsc());
                        vMchId.setText(datas.get(0).getMchid());
                        vLorR.setText(datas.get(0).getLr());
                        vDate.setText(datas.get(0).getWdate().substring(0, 10).replaceAll("/", "-"));
                        vShift.setText(datas.get(0).getShift());
                        vMaster.setText(datas.get(0).getCreateuser());
                        vState.setText(datas.get(0).getiS_H());
                    } else {
//                        Toast.makeText(BarCodeDetailActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG).show();
                        if ((data1 == null && data2 == null) || (data1.isEmpty() && data2.isEmpty())) {
                            Toast.makeText(BarCodeDetailActivity.this, "没有条码明细", Toast.LENGTH_SHORT).show();
                        }
                    }
                    data1.clear();
                    data2.clear();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(BarCodeDetailActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }

            }
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onPause() {
        unregisterReceiver(scanDataReceiver);
        super.onPause();
    }

    //是否纯数字
    public Boolean isNum(String s) {
        char[] ch = s.toCharArray();
        for (char c : ch) {
            if (!(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    //键盘监听
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("key", keyCode + "  ");
        //按键按下
        switch (keyCode) {
            case 0://扫描键按下清空
                tvBarCode.setText("");
                break;
        }

        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //按键弹开
        switch (keyCode) {
//            case 66://回车键
//                getBarCode();
//                break;
            case 22://右方向键
                getBarCode();
                break;
            case 4://返回键
                tofunction();//返回功能菜单页面
                break;
            default:
                break;
        }
        return true;
    }

}
