package com.hsproduce.activity;

import android.annotation.SuppressLint;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputFilter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.VPlanAdapter;
import com.hsproduce.bean.VPlan;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.SoundPlayUtils;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
import com.xuexiang.xui.widget.progress.loading.MiniLoadingView;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;

/**
 * 硫化取消扫描页面
 * 扫描机台号直接进入扫描条码页面，扫描条码，删除此条码的生产实绩
 * 广播监听回调触发事件，成功后此条码写入扫描纪录框中
 * createBy zhangzhr @ 2019-12-30
 */
public class DeleteVulcanizationActivity extends BaseActivity {

    //view 输入机台  记录条码  扫描条码
//    private View ll_Mchid, ll_CodeLog, ll_Code;
    //机台号  轮胎条码 条码计数  条码记录
    private TextView tvBarCode, tvAnum, tvBarCodeLog;
    //    private TextView tvMchid;
    //加载
//    private MiniLoadingView loadingView;
    //轮胎条码
    private String barCode = "", mchId = "";
    //添加条码防止重复扫描
    private List<String> codeList = new ArrayList<>();
    private Boolean isVual = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_delete_vulcanization);
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
        //layout
//        ll_Code = findViewById(R.id.ll_code);
//        ll_Mchid = findViewById(R.id.ll_mchid);
//        ll_CodeLog = findViewById(R.id.ll_codelog);
        //扫描框
//        tvMchid = (TextView) findViewById(R.id.mchid);
        //条码扫描框
        tvBarCode = (TextView) findViewById(R.id.barcode);
        //条码记录
        tvBarCodeLog = (TextView) findViewById(R.id.barcode_log);
        tvBarCodeLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        //不可编辑
        tvBarCodeLog.setFocusable(false);
        tvBarCodeLog.setFocusableInTouchMode(false);
        //扫描条码计数
        tvAnum = (TextView) findViewById(R.id.anum);
        //加载条
//        loadingView = (MiniLoadingView) findViewById(R.id.loading);
        //设置扫描框输入字符数
        tvBarCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
//        tvMchid.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
    }

    //初始化事件
    public void initEvent() {

    }

    //获取轮胎条码  广播监听调用
    public void getBarCode(String barCode) {

        if (StringUtil.isNullOrEmpty(barCode)) {
            Toast.makeText(DeleteVulcanizationActivity.this, "请扫描轮胎条码", Toast.LENGTH_SHORT).show();
        } else {
            //扫描记录中是否已经存在该条码，存在提示已扫描，不存在调用接口记录硫化记录
            if (codeList.contains(barCode)) {
                Toast.makeText(DeleteVulcanizationActivity.this, "此条码已经扫描", Toast.LENGTH_SHORT).show();
                tvBarCode.setText("");
            } else {
                //获取机台号
//                mchId = tvMchid.getText().toString().trim();
                //删除硫化生产记录
                String param = "BARCODE=" + barCode;// + "&MCHID=" + mchId;
                new TypeCodeTask().execute(param);
//                Toast.makeText(DeleteVulcanizationActivity.this, "扫描机台为" + mchId + "扫描条码为：" + barCode, Toast.LENGTH_SHORT).show();
            }
        }

    }

    //广播监听
    private BroadcastReceiver scanDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SCN_CUST_ACTION_SCODE)) {
                try {
                    if (!StringUtil.isNullOrEmpty(mchId)) {
                        mchId = "";
                    }
                    if (!StringUtil.isNullOrEmpty(barCode)) {
                        barCode = "";
                    }
                    String massage = "";
                    massage = intent.getStringExtra(SCN_CUST_EX_SCODE);
                    //判断条码是否为空
                    if (!StringUtil.isNullOrEmpty(massage)) {
//                        massage = massage.toUpperCase();
//                        if (massage.length() == 4 && (massage.endsWith("L") || massage.endsWith("R"))) {
//                            mchId = massage;
////                            tvMchid.setText(mchId);
//                            showVual();
////                            getPlan(massage);
//                        } else
                        if (massage.length() == 12 && isNum(massage) == true) {
                            barCode = massage;
                            getBarCode(massage);
                        }else{
                            Toast.makeText(DeleteVulcanizationActivity.this, "条码不正确，请重新扫描", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        Toast.makeText(DeleteVulcanizationActivity.this, "请重新扫描", Toast.LENGTH_SHORT).show();
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

    //显示硫化生产
//    private void showVual() {
//        //关闭加载
//        loadingView.setVisibility(View.GONE);
//        ll_Mchid.setVisibility(View.GONE);
//        //显示硫化扫描
//        ll_Code.setVisibility(View.VISIBLE);
//        ll_CodeLog.setVisibility(View.VISIBLE);
//        //获得焦点
//        tvBarCode.requestFocus();
//        isVual = true;
//    }

    //显示扫描机台
//    private void showMchid() {
//        ll_Mchid.setVisibility(View.VISIBLE);
//        //显示硫化扫描
//        ll_Code.setVisibility(View.GONE);
//        ll_CodeLog.setVisibility(View.GONE);
//        //获得焦点
//        tvMchid.requestFocus();
//        isVual = false;
//    }

    //新增硫化生产记录
    class TypeCodeTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.OUTVULBARCODE, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(DeleteVulcanizationActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(DeleteVulcanizationActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                    }
                    if (res.get("code").equals("200")) {
                        tvBarCodeLog.append(barCode + "\n");
                        tvBarCode.setText("");
                        codeList.add(barCode);
                        tvAnum.setText(codeList.size() + "");
//                        Toast.makeText(DeleteVulcanizationActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                    }else if(res.get("code").equals("500")){
                        final android.app.AlertDialog.Builder normalDialog = new android.app.AlertDialog.Builder(DeleteVulcanizationActivity.this);
                        normalDialog.setTitle("提示");
                        normalDialog.setMessage(res.get("msg").toString());
                        normalDialog.setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        tvBarCode.requestFocus();
                                        tvBarCode.setText("");
                                    }
                                });
                        // 显示
                        normalDialog.show();
                    } else {
                        Toast.makeText(DeleteVulcanizationActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DeleteVulcanizationActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    //键盘监听
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("key", keyCode + "  ");

        switch (keyCode) {
            case 0:
                tvBarCode.requestFocus();
                tvBarCode.setText("");
//                if (isVual) {
//
//                } else {
//                    tvMchid.requestFocus();
//                    tvMchid.setText("");
//                }
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
            case 22://右方向键
                if (!StringUtil.isNullOrEmpty(barCode)) {
                    barCode = "";
                }
                String sBarCode = tvBarCode.getText().toString().trim();
                if (sBarCode.length() == 12 && isNum(sBarCode) == true) {
                    barCode = sBarCode;
                    getBarCode(barCode);
                }else{
                    Toast.makeText(DeleteVulcanizationActivity.this, "条码不正确，请重新扫描", Toast.LENGTH_SHORT).show();
                }
                break;
            //返回键
            case 4:
                tofunction();
                //返回上级页面
                //先返回扫描机台，再返回功能页
//                if (isVual) {
//                    codeList.clear();
//                    tvBarCodeLog.setText("");
//                    tvBarCode.setText("");
//                    tvMchid.setText("");
//                    tvAnum.setText("0");
//                    showMchid();
//                } else {
//                    tofunction();
//                }
                break;
        }

        return true;
    }


    @SuppressLint("MissingSuperCall")
    @Override
    protected void onPause() {
        unregisterReceiver(scanDataReceiver);
        super.onPause();
    }


}
