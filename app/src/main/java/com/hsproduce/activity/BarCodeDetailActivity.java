package com.hsproduce.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.FormingItemAdapter;
import com.hsproduce.bean.VPlan;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;

import java.util.List;
import java.util.Map;

public class BarCodeDetailActivity extends BaseActivity {

    //轮胎条码
    private TextView barCode;
    //获取计划按钮
    private Button btGetplan;
    //声明一个long类型变量：用于存放上一点击“返回键”的时刻
    private long mExitTime = 0;
    //成型明细
    private TextView fspesc, fspescname, fmchid, fdate, fshift, fmaster, fstate;
    //硫化明细
    private TextView vspesc, vspescname, vmchid, lorR, vdate, vshift, vmaster, vstate;
    //轮胎条码
    public String tvbarCode = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcodedetail);
        //加载控件
        initView();
        //设置控件事件
        initEvent();
    }

    public void initView() {
        //扫描框
        barCode = (TextView) findViewById(R.id.BarCode);
        //获取计划按钮
        btGetplan = (Button) findViewById(R.id.getBarCode);
        //成型
        fspesc = (TextView) findViewById(R.id.fspesc);
        fspescname = (TextView) findViewById(R.id.fspescname);
        fmchid = (TextView) findViewById(R.id.fmchid);
        fdate = (TextView) findViewById(R.id.fdate);
        fshift = (TextView) findViewById(R.id.fshift);
        fmaster = (TextView) findViewById(R.id.fmaster);
        fstate = (TextView) findViewById(R.id.fstate);
        //硫化
        vspesc = (TextView) findViewById(R.id.vspesc);
        vspescname = (TextView) findViewById(R.id.vspescname);
        vmchid = (TextView) findViewById(R.id.vmchid);
        lorR = (TextView) findViewById(R.id.LorR);
        vdate = (TextView) findViewById(R.id.vdate);
        vshift = (TextView) findViewById(R.id.vshift);
        vmaster = (TextView) findViewById(R.id.vmaster);
        vstate = (TextView) findViewById(R.id.vstate);

    }

    public void initEvent() {
        //点击查询成型和硫化条码信息
        btGetplan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentVPlan();
            }
        });
    }

    //生产追溯
    public void getCurrentVPlan() {
        //获取输入机台上barcode
        tvbarCode = barCode.getText().toString().trim();
        if (StringUtil.isNullOrEmpty(tvbarCode)) {
            Toast.makeText(BarCodeDetailActivity.this, "请扫描轮胎条码", Toast.LENGTH_LONG).show();
        } else {
            String parm = "SwitchTYRE_CODE=" + tvbarCode;
            new GetFormingDetail().execute(parm);
            new GetVulcanizaDetail().execute(parm);
        }
    }


    //获取成型明细
    class GetFormingDetail extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.FORMINGSECLECTCODE, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {

            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(BarCodeDetailActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VPlan> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VPlan>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(BarCodeDetailActivity.this, "未获取到数据", Toast.LENGTH_LONG).show();
                    }
                    if (res.get("code").equals("200")) {
                        //成型明细
                        fspesc.setText(datas.get(0).getItnbr());
                        fspescname.setText(datas.get(0).getItdsc());
                        fmchid.setText(datas.get(0).getMchid());
                        fdate.setText(datas.get(0).getAdate());
                        fshift.setText(datas.get(0).getShift());
                        fmaster.setText(datas.get(0).getCreateuser());
                        fstate.setText(datas.get(0).getState());
//                        Toast.makeText(SwitchPlanActivity.this, "计划查询成功！", Toast.LENGTH_LONG).show();
                    } else if (res.get("code").equals("300")) {
                        Toast.makeText(BarCodeDetailActivity.this, "轮胎条码不正确！", Toast.LENGTH_LONG).show();
                    } else if (res.get("code").equals("500")) {
                        Toast.makeText(BarCodeDetailActivity.this, "没有此轮胎条码！", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(BarCodeDetailActivity.this, "查询错误,请重新操作！", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(BarCodeDetailActivity.this, "数据处理异常", Toast.LENGTH_LONG).show();
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

            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(BarCodeDetailActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VPlan> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VPlan>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(BarCodeDetailActivity.this, "未获取到数据", Toast.LENGTH_LONG).show();
                    }
                    if (res.get("code").equals("200")) {
                        //硫化明细
                        vspesc.setText(datas.get(0).getItnbr());
                        vspescname.setText(datas.get(0).getItdsc());
                        vmchid.setText(datas.get(0).getMchid());
                        lorR.setText(datas.get(0).getLr());
                        vdate.setText(datas.get(0).getAdate());
                        vshift.setText(datas.get(0).getShift());
                        vmaster.setText(datas.get(0).getCreateuser());
                        vstate.setText(datas.get(0).getState());
//                        Toast.makeText(SwitchPlanActivity.this, "计划查询成功！", Toast.LENGTH_LONG).show();
                    } else if (res.get("code").equals("300")) {
                        Toast.makeText(BarCodeDetailActivity.this, "轮胎条码不正确！", Toast.LENGTH_LONG).show();
                    } else if (res.get("code").equals("500")) {
                        Toast.makeText(BarCodeDetailActivity.this, "没有此轮胎条码！", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(BarCodeDetailActivity.this, "查询错误,请重新操作！", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(BarCodeDetailActivity.this, "数据处理异常", Toast.LENGTH_LONG).show();
                }

            }
        }
    }

    //键盘监听
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("key", keyCode + "  ");
        //扫描键 按下时清除
        if (keyCode == 0) {
            barCode.setText("");
        }
        if (keyCode == 4) {
            if (System.currentTimeMillis() - mExitTime > 2000) {
                Toast.makeText(this, "再按一次退出登录", Toast.LENGTH_SHORT).show();
                //并记录下本次点击“返回键”的时刻，以便下次进行判断
                mExitTime = System.currentTimeMillis();
            } else {
                System.exit(0);//注销功能
            }
        }
        //返回键时间间隔超过两秒 返回功能页面
        if (keyCode == 21) {
            tofunction(); //BaseActivity  返回功能页面函数
//            Toast.makeText(this, "返回菜单栏", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //扫描键 弹开时获取计划
        if (keyCode == 66) {
            getCurrentVPlan();
        }
        super.onKeyDown(keyCode, event);
        return true;
    }

}