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
import com.hsproduce.bean.VreCord;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;

import java.util.List;
import java.util.Map;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;

/**
 * 条码更换页面
 * 扫描旧条码，查询条码硫化明细，扫描新条码更换加到硫化生产实绩中
 * createBy zhangzr @ 2019-12-20
 * 1.扫描回调改为广播监听方式
 */
public class BarcodeReplaceActivity extends BaseActivity {

    //声明控件   获取条码明细按钮   btn_repl
    private ButtonView btGetCode, btReplCode;
    //旧条码  新条码 规格编码 规格名称 日期 LR 班次 班组 主手
    private TextView tvBarCode, tvNewBarCode, tvSpesc, tvSpescName, tvDate, tvLorR, tvShift, tvTeam, tvCreatUser;
    //声明一个long类型变量：用于存放上一点击“返回键”的时刻
    private long mExitTime = 0;
    //原条码ID
    public String currentId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_barcodereplace);
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
        tvBarCode = (TextView) findViewById(R.id.barcode);
        tvNewBarCode = (TextView) findViewById(R.id.new_barcode);
        tvBarCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        tvNewBarCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        //显示
        tvSpesc = (TextView) findViewById(R.id.spesc);
        tvSpescName = (TextView) findViewById(R.id.spescname);
        tvDate = (TextView) findViewById(R.id.product_date);
        tvLorR = (TextView) findViewById(R.id.LorR);
        tvShift = (TextView) findViewById(R.id.shift);
        tvTeam = (TextView) findViewById(R.id.team);
        tvCreatUser = (TextView) findViewById(R.id.creatuser);
        //条码按钮
        btGetCode = (ButtonView) findViewById(R.id.bt_getCode);
        btReplCode = (ButtonView) findViewById(R.id.barcode_replace);
    }

    public void initEvent() {
        //获取条码明细
        btGetCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selCode();
            }
        });
        //更换条码
        btReplCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNewCode();
            }
        });
    }

    //获取条码明细操作
    public void selCode() {
        String lvcode = tvBarCode.getText().toString().trim();
        if (StringUtil.isNullOrEmpty(lvcode)) {
            Toast.makeText(BarcodeReplaceActivity.this, "请扫描轮胎条码", Toast.LENGTH_SHORT).show();
        } else {
            //?TYRE_CODE=111600000375
            String parm = "TYRE_CODE=" + lvcode;
            new SelCodeTask().execute(parm);
        }
//        barcode.setText("");
    }

    //更换条码操作
    public void getNewCode() {
        String replcode = tvNewBarCode.getText().toString().trim();
        String s_barcode = tvBarCode.getText().toString().trim();
        if (StringUtil.isNullOrEmpty(s_barcode)) {
            Toast.makeText(BarcodeReplaceActivity.this, "请扫描原轮胎条码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (StringUtil.isNullOrEmpty(replcode)) {
            Toast.makeText(BarcodeReplaceActivity.this, "请扫描新轮胎条码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (s_barcode.equals(replcode)) {
            Toast.makeText(BarcodeReplaceActivity.this, "新条码与原条码一致，请重新扫描条码", Toast.LENGTH_SHORT).show();
            return;
        }
        String parm = "currentId=" + currentId + "&SwitchTYRE_CODE=" + replcode + "&USER_NAME=" + App.username;
        new ReplCodeTask().execute(parm);
    }

    //广播监听
    private BroadcastReceiver scanDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SCN_CUST_ACTION_SCODE)) {
                try {
                    String barCode = "";
                    barCode = intent.getStringExtra(SCN_CUST_EX_SCODE);
                    //判断条码是否为空  是否12位 是否纯数字
                    if (!StringUtil.isNullOrEmpty(barCode) && barCode.length() == 12 && isNum(barCode) == true) {
                        //判断填入那个扫描框
                        if (StringUtil.isNullOrEmpty(tvBarCode.getText().toString().trim()) && StringUtil.isNullOrEmpty(tvNewBarCode.getText().toString().trim())) {
                            tvBarCode.setText(barCode);
                            selCode();
                        } else if (!StringUtil.isNullOrEmpty(tvBarCode.getText().toString().trim()) && StringUtil.isNullOrEmpty(tvNewBarCode.getText().toString().trim())) {
                            tvNewBarCode.setText(barCode);
                        } else if (StringUtil.isNullOrEmpty(tvBarCode.getText().toString().trim()) && !StringUtil.isNullOrEmpty(tvNewBarCode.getText().toString().trim())) {
                            tvBarCode.setText(barCode);
                            selCode();
                        } else {
                            Toast.makeText(BarcodeReplaceActivity.this, "请删除原有条码再扫描", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        Toast.makeText(BarcodeReplaceActivity.this, "请重新扫描", Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ScannerService", e.toString());
                }
            }
        }
    };

    //扫描条码查询轮胎规格
    class SelCodeTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.SelTYRE_CODE, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
//            //获得焦点
//            tvNewBarCode.requestFocus();

            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(BarcodeReplaceActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VreCord> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VreCord>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(BarcodeReplaceActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                    }
                    if (res.get("code").equals("200") && datas != null && datas.size() > 0) {
                        //获取原条码ID
                        currentId = String.valueOf(datas.get(0).getId());
                        //展示信息
                        tvSpesc.setText(datas.get(0).getItnbr());
                        tvSpescName.setText(datas.get(0).getItdsc());
                        tvDate.setText(datas.get(0).getWdate().substring(0, 10));
                        tvLorR.setText(datas.get(0).getLr());
                        String s_shift = datas.get(0).getShift();
                        if (!StringUtil.isNullOrEmpty(s_shift)) {
                            if (s_shift.equals("1")) {
                                tvShift.setText("早班");
                            } else if (s_shift.equals("2")) {
                                tvShift.setText("中班");
                            } else {
                                tvShift.setText("晚班");
                            }
                        }
                        String s_team = datas.get(0).getTeam();
                        if (!StringUtil.isNullOrEmpty(s_team)) {
                            if (s_team.equals("1")) {
                                tvTeam.setText("甲班");
                            } else if (s_team.equals("2")) {
                                tvTeam.setText("乙班");
                            } else if (s_team.equals("3")) {
                                tvTeam.setText("丙班");
                            } else {
                                tvTeam.setText("丁班");
                            }
                        }
                        tvCreatUser.setText(datas.get(0).getCreateuser());
                        //获取焦点
                        tvNewBarCode.requestFocus();
                    } else if (res.get("code").equals("500")) {
                        Toast.makeText(BarcodeReplaceActivity.this, "查询成功，没有匹配的条码！", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BarcodeReplaceActivity.this, "错误：" + res.get("ex"), Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(BarcodeReplaceActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //新条码是否更换
    class ReplCodeTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.ChangeTYRE_CODE, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(BarcodeReplaceActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(BarcodeReplaceActivity.this, "未获取到信息", Toast.LENGTH_SHORT).show();
                    }
                    if (res.get("code").equals("200")) {
                        //清空数据
                        tvBarCode.setText("");
                        tvSpesc.setText("");
                        tvSpescName.setText("");
                        tvDate.setText("");
                        tvLorR.setText("");
                        tvShift.setText("");
                        tvTeam.setText("");
                        tvCreatUser.setText("");
                        tvNewBarCode.setText("");
                        //获取焦点
                        tvBarCode.requestFocus();
                        Toast.makeText(BarcodeReplaceActivity.this, "更换成功！", Toast.LENGTH_SHORT).show();
                    } else if (res.get("code").equals("100")) {
                        Toast.makeText(BarcodeReplaceActivity.this, "新条码被使用过无法更换！", Toast.LENGTH_SHORT).show();
                    } else if (res.get("code").equals("300")) {
                        Toast.makeText(BarcodeReplaceActivity.this, "更换失败！", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BarcodeReplaceActivity.this, "错误：" + res.get("ex"), Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(BarcodeReplaceActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
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

        return true;
    }

    //键盘监听
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            //扫描键
//            case 0:
//                if(!StringUtil.isNullOrEmpty(tvBarCode.getText().toString().trim())){
//                    selCode();
//                }
//                break;
            //右方向键
            case 22:
                if (!StringUtil.isNullOrEmpty(tvBarCode.getText().toString().trim())) {
                    selCode();
                }
                //返回键
            case 4:
                startActivity(new Intent(BarcodeReplaceActivity.this, FunctionActivity.class));
                this.finish();
                break;
        }
        super.onKeyDown(keyCode, event);
        return true;
    }

}
