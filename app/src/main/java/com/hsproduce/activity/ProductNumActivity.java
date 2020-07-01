package com.hsproduce.activity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.ProductItemAdapter;
import com.hsproduce.bean.VPlan;
import com.hsproduce.bean.VreCord;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.StringUtil;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;

/**
 * 当班产量页面
 * 获取当班产量
 * createBy zhangzhr @ 2019-12-30
 * 1.修改为根据机台号和日期查询产量
 * 2.页面修改
 */
public class ProductNumActivity extends BaseActivity {

    //定义控件
    private TextView tvDnum;
    //机台
    private EditText ed_MchId;
    //日期
//    private TextView tv_Date;
    //查询按钮
    private ImageButton bt_GetProduct;
    //当班产量
    private ListView listView;
    private ProductItemAdapter adapter;
    //机台号
    private String mchId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_productnum);
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
//        tvDnum = (TextView) findViewById(R.id.dnum);
//        new GetDnumSumTask().execute("UserName=" + App.username);
        //机台
        ed_MchId = (EditText) findViewById(R.id.tv_MchId);
        //日期
//        tv_Date = (TextView) findViewById(R.id.tv_Date);
        //查询按钮
        bt_GetProduct = (ImageButton) findViewById(R.id.getProduct);
        //ListView
        listView = (ListView)findViewById(R.id.lv_product);
    }

    public void initEvent() {
        //点击查询成型计划
        bt_GetProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mchid = ed_MchId.getText().toString().trim();
                setMchId(mchid);
            }
        });
        //时间
//        tv_Date.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Calendar c = Calendar.getInstance();
//                // 直接创建一个DatePickerDialog对话框实例，并将它显示出来
//                new DatePickerDialog(ProductNumActivity.this,
//                        // 绑定监听器
//                        new DatePickerDialog.OnDateSetListener() {
//
//                            @Override
//                            public void onDateSet(DatePicker view, int year,
//                                                  int monthOfYear, int dayOfMonth) {
//                                TextView show = (TextView) findViewById(R.id.tv_Date);
//                                show.setText(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
//                            }
//                        }
//                        // 设置初始日期
//                        , c.get(Calendar.YEAR), c.get(Calendar.MONTH), c
//                        .get(Calendar.DAY_OF_MONTH)).show();
//            }
//        });
    }

    //按钮点击事件获取产量
    public void getProduct() {
//        if (StringUtil.isNullOrEmpty(mchId)) {
//            Toast toast = Toast.makeText(ProductNumActivity.this, "请扫描机台号", Toast.LENGTH_LONG);
//            showMyToast(toast, 500);
//            ed_MchId.setText("");
//            return;
//        }
//        if (StringUtil.isNullOrEmpty(tv_Date.getText().toString().trim())) {
//            Toast toast = Toast.makeText(ProductNumActivity.this, "请选择日期", Toast.LENGTH_LONG);
//            showMyToast(toast, 500);
//            tv_Date.setText("");
//            return;
//        }
    }

    //获取机台号
    public void setMchId(String mchid) {
        if(!StringUtil.isNullOrEmpty(mchId)){
            mchId = "";
        }
        if(StringUtil.isNullOrEmpty(mchid)){
            mchId = 0+"";
        } else {
//            mchid = mchid.substring(0,3);
            mchId = mchid;
        }
        //api/PDA/GetDnumSum?UserName=R18183&team=3&mchid=0
        String param = "UserName=" + App.username + "&team=" + App.shift + "&mchid=" + mchId;
        new GetDnumSumTask().execute(param);
    }

    //广播监听
    private BroadcastReceiver scanDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SCN_CUST_ACTION_SCODE)) {
                try {
                    String mchid = "";
                    mchid = intent.getStringExtra(SCN_CUST_EX_SCODE);
                    //判断机台号是否为空
                    if (!StringUtil.isNullOrEmpty(mchid)) {
                        //判断位数和是否是纯数字
                        if (mchid.length() == 4 && isNumeric(mchid) == false) {
                            setMchId(mchid);
                        } else {
                            Toast toast = Toast.makeText(ProductNumActivity.this, "请重新扫描", Toast.LENGTH_LONG);
                            showMyToast(toast, 500);
                            return;
                        }

                    } else {
                        Toast toast = Toast.makeText(ProductNumActivity.this, "请重新扫描", Toast.LENGTH_LONG);
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

    //获取当班产量
    class GetDnumSumTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.GetDnumSum, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(ProductNumActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VPlan> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VPlan>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(ProductNumActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        adapter = new ProductItemAdapter(ProductNumActivity.this, datas);
                        listView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }else{
                        datas.clear();
                        adapter = new ProductItemAdapter(ProductNumActivity.this, datas);
                        listView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(ProductNumActivity.this, "没有当班产量", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ProductNumActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
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
        //按键按下
        switch (keyCode) {
            case 0://扫描键
                ed_MchId.setText("");
                ed_MchId.requestFocus();
                break;
        }
        return true;
    }

    //键盘监听
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //按键弹开
        switch (keyCode) {
            case 22://右方向键
//                getProduct();
                String mchid = ed_MchId.getText().toString().trim();
                setMchId(mchid);
                break;
            case 4://返回键
                mchId = "";
                ed_MchId.setText("");
//                tv_Date.setText("");
                tofunction();
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
