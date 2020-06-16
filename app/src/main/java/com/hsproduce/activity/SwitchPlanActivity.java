package com.hsproduce.activity;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.VPlanReplAdapter;
import com.hsproduce.bean.VPlan;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.SoundPlayUtils;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;
import com.xuexiang.xui.widget.dialog.materialdialog.DialogAction;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;

/**
 * 硫化规格交替页面
 * 扫描机台号，查询等待中的计划，点击规格交替显示是否交替，传递计划ID切换计划
 * createBy zhangzhr @ 2019-12-21
 * 1.扫描改为广播监听响应方式
 */
public class SwitchPlanActivity extends BaseActivity {

    private LinearLayout llShowList, llMchId;
    private TableLayout tlShowVPlan;
    private LinearLayout llOnClick;
    private Button btRepl, btOut;
    private TextView tvSpesc, tvSpescName, tvPro, tvState, tvPNum;
    //当前计划展示list  规格交替列表
    private ListView lvPlan;
    private VPlan v = new VPlan();
    private List<VPlan> planList = new ArrayList<>();
    //输入框
    private TextView tvMchid;
    //获取计划按钮
    private ButtonView btGetPlan;
    //计划展示适配器  规格交替适配器
    private VPlanReplAdapter adaprer;
    //定义变量 当前计划ID
    private String currid = "";
    public String mchid = "";
    private String num = "";
    private boolean isComplate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_switchplan);
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
        //点击之前页面
        llMchId = findViewById(R.id.ll_mchid);
        llShowList = findViewById(R.id.showlist);
        //点击之后页面
        tlShowVPlan = (TableLayout) findViewById(R.id.showVplan);
        llOnClick = findViewById(R.id.onclick);
        //点击之后的按钮
        btRepl = (Button) findViewById(R.id.repl);
        btOut = (Button) findViewById(R.id.out);
        //点击之后显示明细
        tvSpesc = (TextView) findViewById(R.id.spesc);
        tvSpescName = (TextView) findViewById(R.id.spescname);
        tvPro = (TextView) findViewById(R.id.pro);
        tvState = (TextView) findViewById(R.id.state);
        tvPNum = (TextView) findViewById(R.id.pnum);
        //list列表
        lvPlan = (ListView) findViewById(R.id.lv_plan);
        //扫描框
        tvMchid = (TextView) findViewById(R.id.mchid);
        tvMchid.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        //获取计划按钮
        btGetPlan = (ButtonView) findViewById(R.id.getSwitchPlan);

    }

    public void initEvent() {
        //点击当期计划 和 规格交替计划
        btGetPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentVPlan();
            }
        });
        //点击跳转
        lvPlan.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currid != null || !currid.equals("")){
                    currid = "";
                }
                //初始化一下控件属性
                btRepl.setEnabled(true);
                btOut.setEnabled(true);
                //获取选中的计划
                v = adaprer.getItem(position);
                //计划id
                currid = v.getId();
                //展示数据在页面
                tvSpesc.setText(v.getItnbr());
                tvSpescName.setText(v.getItdsc());
                tvPro.setText(v.getPro());
                if (v.getState().equals("10")) {
                    tvState.setText("新计划");
                } else if (v.getState().equals("20")) {
                    tvState.setText("等待中");
                } else if (v.getState().equals("30")) {
                    tvState.setText("生产中");
                } else if (v.getState().equals("40")) {
                    tvState.setText("已完成");
                } else {
                    tvState.setText("未知状态");
                }
                tvPNum.setText(v.getPnum());
                //点击之后隐藏
                llMchId.setVisibility(View.GONE);
                llShowList.setVisibility(View.GONE);
                lvPlan.setVisibility(View.GONE);
                //点击之后显示
                tlShowVPlan.setVisibility(View.VISIBLE);
                llOnClick.setVisibility(View.VISIBLE);
            }
        });
        //返回
        btOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnPager();
            }
        });
        //切换规格
        btRepl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogToStart();
            }
        });

    }

    //开始计划
    public void dialogToStart() {
        //显示弹窗
        new MaterialDialog.Builder(SwitchPlanActivity.this)
                .title("提示")
                .content("请确认是否切换规格")
                .positiveText(R.string.vul_confirm)
                .negativeText(R.string.vul_cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        //开始计划
                        String param = "SwitchID=" + v.getId() + "&USER_NAME=" + App.username;
                        new ChangePlanTask().execute(param);
                        //提示音
                        SoundPlayUtils.startNoti(SwitchPlanActivity.this);
                        SoundPlayUtils.stopAlarm();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        isComplate = true;
                        //提示音
                        SoundPlayUtils.startAlarm(SwitchPlanActivity.this);
                        SoundPlayUtils.stopAlarm();
                    }
                })
                .cancelable(false)
                .show();
    }

    //跳转页面
    public void returnPager() {
//        //清空ID
//        currid = "";
        //点击之后隐藏
        tlShowVPlan.setVisibility(View.GONE);
        llOnClick.setVisibility(View.GONE);
        //点击之后显示
        llMchId.setVisibility(View.VISIBLE);
        llShowList.setVisibility(View.VISIBLE);
        lvPlan.setVisibility(View.VISIBLE);
        //刷新数据
        String param1 = "MCHIDLR=" + mchid + "&SHIFT=" + App.shift + "&&TYPE_N=20";
        new GetPlanTask().execute(param1);
        adaprer.notifyDataSetChanged();
    }

    //根据状态查询计划
    public void getCurrentVPlan() {
        //获取输入机台上barcode
        mchid = tvMchid.getText().toString().trim();
        if (StringUtil.isNullOrEmpty(mchid)) {
            Toast.makeText(SwitchPlanActivity.this, "请扫描机台号", Toast.LENGTH_SHORT).show();
        } else {
            mchid = mchid.toUpperCase();
            String param1 = "MCHIDLR=" + mchid + "&SHIFT=" + App.shift + "&TYPE_N=20";
            new GetPlanTask().execute(param1);
        }
    }

    //广播监听
    private BroadcastReceiver scanDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SCN_CUST_ACTION_SCODE)) {
                try {
                    String mchID = "";
                    mchID = intent.getStringExtra(SCN_CUST_EX_SCODE);
                    //判断条码是否为空 是否为12位 是否纯数字组成
                    if (!StringUtil.isNullOrEmpty(mchID) && mchID.length() == 4 && isNum(mchID) == false) {
                        tvMchid.setText(mchID);
                        operate("扫描失败！");
                    } else {
                        Toast.makeText(SwitchPlanActivity.this, "请重新扫描", Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ScannerService", e.toString());
                }
            }
        }
    };

    //获取硫化计划
    class GetPlanTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.GetCurrentVPlan, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            isComplate = true;
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(SwitchPlanActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VPlan> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VPlan>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(SwitchPlanActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        if (datas != null && datas.size() > 0) {//有正在执行，并且有等待中的计划
                            //显示等待中的计划；
                            adaprer = new VPlanReplAdapter(SwitchPlanActivity.this, datas);
                            lvPlan.setAdapter(adaprer);
                            adaprer.notifyDataSetChanged();
                            planList = datas;
                        } else {
                            Toast.makeText(SwitchPlanActivity.this, "无可规格交替的计划！", Toast.LENGTH_SHORT).show();
                            adaprer = new VPlanReplAdapter(SwitchPlanActivity.this, new ArrayList<VPlan>());
                            lvPlan.setAdapter(adaprer);
                            adaprer.notifyDataSetChanged();
                            planList = datas;
                        }

                    } else if (res.get("code").equals("300")) {
                        Toast.makeText(SwitchPlanActivity.this, "未到换班时间不可进行倒班！", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (res.get("code").equals("500")) {
                        Toast.makeText(SwitchPlanActivity.this, "查询成功，没有匹配的计划！", Toast.LENGTH_SHORT).show();
                        adaprer = new VPlanReplAdapter(SwitchPlanActivity.this, new ArrayList<VPlan>());
                        lvPlan.setAdapter(adaprer);
                        adaprer.notifyDataSetChanged();
                        planList = datas;
                        return;
                    } else {
                        Toast.makeText(SwitchPlanActivity.this, "计划查询错误,请重新操作！", Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SwitchPlanActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    //切换规格
    class ChangePlanTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.SwitchVplan, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            isComplate = true;
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(SwitchPlanActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VPlan> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VPlan>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(SwitchPlanActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        Toast.makeText(SwitchPlanActivity.this, "规格交替成功！", Toast.LENGTH_SHORT).show();
                        returnPager();
                    } else {
                        Toast.makeText(SwitchPlanActivity.this, res.get("msg") + "", Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SwitchPlanActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
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
    public boolean onKeyDown(int keyCode, KeyEvent event){
        Log.e("key", keyCode + "  ");
        switch (keyCode){
            case 0:
                //按下扫描键时，先清空之前内容
                tvMchid.setText("");
                break;
        }
        return true;
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event){
        //扫描键 弹开时获取计划
        //右方向键
        String msg = "";
        switch (keyCode){
            //返回键
            case 4:
                //返回上级页面
                startActivity(new Intent(SwitchPlanActivity.this, FunctionActivity.class));
                this.finish();
                break;
            //右方向键
            case 22:
                msg = "扫描失败！";
                operate(msg);
                break;
            //扫描键
//            case 0:
//                msg = "扫描失败！";
//                operate(msg);
//                break;
            default:

                break;

        }
        return true;
    }
    private void operate(String msg) {
        if(!isComplate){
            Toast.makeText(this, "请等待上一次操作完成再继续！", Toast.LENGTH_SHORT).show();
            return;
        }
        isComplate = false;
        if (!StringUtil.isNullOrBlank(tvMchid.getText().toString().trim())) {
            getCurrentVPlan();
        } else {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            isComplate = true;
        }
    }

}
