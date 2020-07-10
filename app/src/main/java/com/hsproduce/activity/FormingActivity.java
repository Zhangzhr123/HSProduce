package com.hsproduce.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.DialogItemAdapter;
import com.hsproduce.adapter.FormingItemAdapter;
import com.hsproduce.bean.VPlan;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.SoundPlayUtils;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 成型生产页面
 * 查询此机台所有计划，如果有生产中和等待中或者已完成和等待中的计划则不可以点击开始按钮
 * 如果只有等待中的计划则是可以点击开始按钮，生产中的计划开始按钮不可用，其他的都只可以用返回按钮
 * 点击开始按钮如果有生产中的计划结束这一计划再开始，其他直接开始计划
 * creatBy zhangzhr @ 2019-12-21
 * 1.如果开始条码发生改变则进行修改开始条码操作，如果没有则直接结束上一计划
 */
public class FormingActivity extends BaseActivity {

    private LinearLayout llShowList, llMchId;
    private TableLayout tlShowVPlan;
    private LinearLayout llOnClick;
    private Button btStart, btUpdate, btFinish, btOut;
    private TextView tvSpesc, tvSpescName, tvPro, tvState, tvPNum, tvShift;
    //当前计划展示list  规格交替列表
    private ListView lvPlan;
    private VPlan v = new VPlan();
    //机台号
//    private TextView tvMchid;
    private AutoCompleteTextView tvMchid;
    private List<String> data1 = new ArrayList<>();
    //获取计划按钮
    private ImageButton btGetPlan;
    //计划展示适配器  规格交替适配器
    private FormingItemAdapter adapter;
    //声明一个long类型变量：用于存放上一点击“返回键”的时刻
    private long mExitTime = 0;
    //定义变量 当前计划ID
    private String currid = "";
    public String mchid = "";
    public String number = "";
    public Integer isNull = 0;//判断开始按钮弹窗显示
    private VPlan vplan = new VPlan();
    private String num = "";
    private String startCode = "";//开始的开始条码
    private String preEndCode = "";//上一个计划的开始条码
    private String endCode = "";//当前结束条码
    private String cxjy = "";//成型验证校验

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_forming);
        //加载控件
        initView();
        //设置控件事件
        initEvent();
    }

    public void initView() {
        //点击之前页面
        llMchId = findViewById(R.id.ll_mchid);
        llShowList = findViewById(R.id.showlist);
        //点击之后页面
        tlShowVPlan = (TableLayout) findViewById(R.id.showVplan);
        llOnClick = findViewById(R.id.onclick);
        //点击之后的按钮
        btStart = (Button) findViewById(R.id.start);
        btUpdate = (Button) findViewById(R.id.update);
        btFinish = (Button) findViewById(R.id.finish);
        btOut = (Button) findViewById(R.id.out);
        //点击之后显示明细
        tvSpesc = (TextView) findViewById(R.id.spesc);
        tvSpescName = (TextView) findViewById(R.id.spescname);
        tvPro = (TextView) findViewById(R.id.pro);
        tvState = (TextView) findViewById(R.id.state);
        tvPNum = (TextView) findViewById(R.id.pnum);
        //班次新增
        tvShift = (TextView) findViewById(R.id.shift);
        //list列表
        lvPlan = (ListView) findViewById(R.id.lv_plan);
        //扫描框
        tvMchid = (AutoCompleteTextView) findViewById(R.id.mchid);
        new MCHIDTask().execute("TYPE_ID=10107");
        eventsViews();
        //获取计划按钮
        btGetPlan = (ImageButton) findViewById(R.id.getPlan);
        //是否启用成型验证校验
        String parm = "TYPE_ID=10114";
        new CXJYTask().execute(parm);
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
                if (!StringUtil.isNullOrEmpty(currid)) {
                    currid = "";
                }
                //初始化一下控件属性
                btStart.setEnabled(true);
                btUpdate.setEnabled(true);
                btFinish.setEnabled(true);
                btOut.setEnabled(true);
                //获取点击的数据
                v = adapter.getItem(position);
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
                //班次
                if (v.getShift().equals("1")) {
                    tvShift.setText("早班");
                } else if (v.getShift().equals("2")) {
                    tvShift.setText("中班");
                } else if (v.getShift().equals("3")) {
                    tvShift.setText("晚班");
                } else {
                    tvShift.setText("未知数据");
                }
                //设置按钮是否可用
                if (v.getBtnflag().equals("1")) {//修改和完成不可用
                    //按钮不可用
                    btUpdate.setEnabled(false);
                    btFinish.setEnabled(false);
                } else if (v.getBtnflag().equals("2")) {//开始不可用
                    btStart.setEnabled(false);
                } else {//只有返回
                    btStart.setEnabled(false);
                    btUpdate.setEnabled(false);
                    btFinish.setEnabled(false);
                }
                //点击之后隐藏
                llMchId.setVisibility(View.GONE);
                llShowList.setVisibility(View.GONE);
                lvPlan.setVisibility(View.GONE);
                //点击之后显示
                tlShowVPlan.setVisibility(View.VISIBLE);
                llOnClick.setVisibility(View.VISIBLE);
                //查询生产中计划
                new STARTTask().execute("MCHID=" + mchid);
            }
        });
        //返回
        btOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnPager();
            }
        });
        //开始
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlan();
            }
        });
        //修改
        btUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v1) {
                updateNumber();
            }
        });
        //完成
        btFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v1) {
                finishPlan();
            }
        });

    }

    //开始
    public void startPlan() {
        if (isNull == 1) {
            //开始计划
            dialogToStart();
        }
        else if (isNull == 2) {
            //完成计划
            //执行结束上一计划
            dialogToFinish();
        }
        else if (isNull == 3) {
            Toast.makeText(FormingActivity.this, "网络连接异常，请重新登录。", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(FormingActivity.this, "数据获取异常，请联系管理员。", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    //修改
    public void updateNumber() {
        final EditText et = new EditText(FormingActivity.this);
        et.setText("");
        et.setHint("请修改数量");
        new AlertDialog.Builder(FormingActivity.this).setTitle("修改")
                .setView(et)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (!StringUtil.isNullOrEmpty(number)) {
                            number = "";
                        }
                        number = et.getText().toString();
                        if (number != null && !number.equals("") && Integer.valueOf(number) != 0) {
                            Integer sum = 0;
                            if (v.getBarcodestart() != null) {
                                sum = Integer.valueOf(v.getBarcodestart().substring(6, 12)) + Integer.valueOf(number);
                            } else {
                                sum = Integer.valueOf(number);
                            }
                            if (Integer.valueOf(number) > 500 || sum > 999999) {
                                Toast.makeText(FormingActivity.this, "数量不能大于500或流水号不能大于999999", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            //修改操作接口
                            String param = "VPLANID=" + currid + "&Num=" + number + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                            new UPDATETask().execute(param);
                            //显示修改内容
                            tvPNum.setText("");
                            tvPNum.setText(number);
                            //开始按钮不可用
                            btStart.setEnabled(false);
                            btUpdate.setEnabled(true);
                            btFinish.setEnabled(true);
                            btOut.setEnabled(true);
                            //提示音
                            SoundPlayUtils.startNoti(FormingActivity.this);
                            SoundPlayUtils.stopAlarm();

                        } else {
                            Toast.makeText(FormingActivity.this, "请输入数量", Toast.LENGTH_SHORT).show();
                        }

                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //提示音
                SoundPlayUtils.startAlarm(FormingActivity.this);
                SoundPlayUtils.stopAlarm();
            }
        }).show();
    }

    //跳转页面
    public void returnPager() {
        //点击之后隐藏
        tlShowVPlan.setVisibility(View.GONE);
        llOnClick.setVisibility(View.GONE);
        //点击之后显示
        llMchId.setVisibility(View.VISIBLE);
        llShowList.setVisibility(View.VISIBLE);
        lvPlan.setVisibility(View.VISIBLE);
        //刷新页面数据
        String param1 = "MCHID=" + mchid + "&SHIFT=" + App.shift;
        new GetFormingPlanTask().execute(param1);
        adapter.notifyDataSetChanged();
    }

    private void eventsViews() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data1);
        tvMchid.setAdapter(adapter);
    }

    //查询计划
    public void getCurrentVPlan() {
        //获取输入机台上barcode
        if (!StringUtil.isNullOrEmpty(mchid)) {
            mchid = "";
        }
        mchid = tvMchid.getText().toString().trim();
        if (StringUtil.isNullOrEmpty(mchid)) {
            Toast.makeText(FormingActivity.this, "请扫描机台号", Toast.LENGTH_SHORT).show();
        } else {
            //已下达的计划
            String param1 = "MCHID=" + mchid + "&SHIFT=" + App.shift;
            new GetFormingPlanTask().execute(param1);
        }
    }

    //开始当前计划
    public void dialogToStart() {
        //显示弹窗
        final MaterialDialog dialog = new MaterialDialog.Builder(FormingActivity.this)
                .customView(R.layout.dialog_product, true)
                .show();
        //控件
        View customeView = dialog.getCustomView();
        final EditText next = dialog.findViewById(R.id.input);
        final EditText number = dialog.findViewById(R.id.input2);
        Button returnDialog = customeView.findViewById(R.id.finish);
        returnDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                //提示音
                SoundPlayUtils.startAlarm(FormingActivity.this);
                SoundPlayUtils.stopAlarm();
            }
        });
        Button ok = customeView.findViewById(R.id.ok);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (num != null || !num.equals("")) {
                    num = "";
                }
                //条码位数为12为   05（工厂代码）19（年份）25（机台编码最后两位）123456（流水码）
                String jt = mchid;
                jt = jt.substring(jt.length() - 2, jt.length());
                String nextCode = next.getText().toString();
                num = number.getText().toString();

                //成型验证校验
                if(!StringUtil.isNullOrEmpty(cxjy)) {
                    if (cxjy.equals("1")) {
                        //如果为空则进行操作
                        if (num.equals("") || Integer.valueOf(num) <= 0 || nextCode.equals("")) {
                            Toast.makeText(FormingActivity.this, "数量或开始条码为空，请输入！", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (nextCode.length() != 12) {
                            Toast.makeText(FormingActivity.this, "开始条码规格不正确，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String nextjt = nextCode.substring(4, 6);
                        if (!jt.equals(nextjt)) {
                            Toast.makeText(FormingActivity.this, "开始条码不属于此机台，请重新输入", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Integer sum = Integer.valueOf(nextCode.substring(6, 12)) + Integer.valueOf(num);
                        if (Integer.valueOf(num) > 500 || sum > 999999) {
                            Toast.makeText(FormingActivity.this, "数量不能大于500或者条码流水号不能大于999999", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!StringUtil.isNullOrEmpty(startCode)) {
                            startCode = "";
                        }
                        startCode = nextCode;
                        //如果存在上一个未完成的计划，结束上一计划
//                if (vplan != null && isNull == 2) {
//                    int endnumber = Integer.valueOf(nextCode.substring(6, 12)) - 1;
//                    String endBarcode = String.format("%06d", endnumber);
//                    endBarcode = (nextCode.substring(0, 6)) + endBarcode;
//                    String param1 = "VPLANID=" + vplan.getId() + "&EndBarcode=" + endBarcode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
//                    new DIALOGFINISHTask().execute(param1);
//                }
                        //执行开始计划接口
                        String param = "VPLANID=" + currid + "&StartBarcode=" + nextCode + "&Num=" + num + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                        new GETSTARTTask().execute(param);
                        //显示修改
                        tvState.setText("");
                        tvState.setText("生产中");
                        tvPNum.setText("");
                        tvPNum.setText(num);
                        //开始按钮不可用
                        btStart.setEnabled(false);
                        btUpdate.setEnabled(true);
                        btFinish.setEnabled(true);
                        btOut.setEnabled(true);

                    }else{
                        //如果为空则进行操作
                        if (num.equals("") || Integer.valueOf(num) <= 0 || nextCode.equals("")) {
                            Toast.makeText(FormingActivity.this, "数量或开始条码为空，请输入！", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (nextCode.length() != 12) {
                            Toast.makeText(FormingActivity.this, "开始条码规格不正确，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
//                        String nextjt = nextCode.substring(4, 6);
//                        if (!jt.equals(nextjt)) {
//                            Toast.makeText(FormingActivity.this, "开始条码不属于此机台，请重新输入", Toast.LENGTH_LONG).show();
//                            return;
//                        }
                        Integer sum = Integer.valueOf(nextCode.substring(6, 12)) + Integer.valueOf(num);
                        if (Integer.valueOf(num) > 500 || sum > 999999) {
                            Toast.makeText(FormingActivity.this, "数量不能大于500或者条码流水号不能大于999999", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!StringUtil.isNullOrEmpty(startCode)) {
                            startCode = "";
                        }
                        startCode = nextCode;
                        //如果存在上一个未完成的计划，结束上一计划
//                if (vplan != null && isNull == 2) {
//                    int endnumber = Integer.valueOf(nextCode.substring(6, 12)) - 1;
//                    String endBarcode = String.format("%06d", endnumber);
//                    endBarcode = (nextCode.substring(0, 6)) + endBarcode;
//                    String param1 = "VPLANID=" + vplan.getId() + "&EndBarcode=" + endBarcode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
//                    new DIALOGFINISHTask().execute(param1);
//                }
                        //执行开始计划接口
                        String param = "VPLANID=" + currid + "&StartBarcode=" + nextCode + "&Num=" + num + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                        new GETSTARTTask().execute(param);
                        //显示修改
                        tvState.setText("");
                        tvState.setText("生产中");
                        tvPNum.setText("");
                        tvPNum.setText(num);
                        //开始按钮不可用
                        btStart.setEnabled(false);
                        btUpdate.setEnabled(true);
                        btFinish.setEnabled(true);
                        btOut.setEnabled(true);
                    }
                }else{
                    Toast.makeText(FormingActivity.this, "成型验校验为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                //提示音
                SoundPlayUtils.startNoti(FormingActivity.this);
                SoundPlayUtils.stopAlarm();
            }
        });
    }

    //开始按钮中的完成上一计划
    public void dialogToFinish() {
        final MaterialDialog dialog = new MaterialDialog.Builder(FormingActivity.this)
                .customView(R.layout.dialog_prefinish, true)
                .show();
        //控件
        View customeView = dialog.getCustomView();
        final TextView itnbr = customeView.findViewById(R.id.input);
        final TextView itdec = customeView.findViewById(R.id.input4);
        final EditText ed_StartCode = customeView.findViewById(R.id.input2);
        final EditText ed_EndCode = dialog.findViewById(R.id.input3);
        //设置扫描框输入字符数
        ed_StartCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        ed_EndCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        //获取焦点
        ed_EndCode.requestFocus();
        if (vplan != null) {
            itnbr.setText(vplan.getItnbr());
            itdec.setText(vplan.getItdsc());
            ed_StartCode.setText(vplan.getBarcodestart());
            ed_EndCode.setText("");
        }
        Button returnDialog = customeView.findViewById(R.id.finish);
        returnDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                //提示音
                SoundPlayUtils.startAlarm(FormingActivity.this);
                SoundPlayUtils.stopAlarm();
            }
        });
        Button ok = customeView.findViewById(R.id.ok);
        //取消焦点
        ok.setFocusable(false);
        returnDialog.setFocusable(false);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String jt = mchid;
                jt = jt.substring(jt.length() - 2, jt.length());
                String endCode = ed_EndCode.getText().toString();

                //成型验证校验
                if(!StringUtil.isNullOrEmpty(cxjy)){
                    if(cxjy.equals("1")){
                        //如果为空则进行操作
                        if (StringUtil.isNullOrEmpty(endCode) || StringUtil.isNullOrEmpty(ed_StartCode.getText().toString())) {
                            Toast.makeText(FormingActivity.this, "条码为空，请输入！", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (endCode.length() != 12 || ed_StartCode.getText().toString().length() != 12) {
                            Toast.makeText(FormingActivity.this, "条码规格不正确，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String endjt = endCode.substring(4, 6);
                        String startjt = ed_StartCode.getText().toString().substring(4, 6);
                        if (!jt.equals(endjt) || !jt.equals(startjt)) {
                            Toast.makeText(FormingActivity.this, "条码不属于此机台，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //判断是否跨年
                        String srart = ed_StartCode.getText().toString().substring(0, 4);
                        String end = endCode.substring(0, 4);
                        if (!srart.equals(end)) {
                            Toast.makeText(FormingActivity.this, "条码不能跨年，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //判断是否超过500
                        Integer startNum = Integer.valueOf(ed_StartCode.getText().toString().substring(6, 12));
                        Integer endNum = Integer.valueOf(endCode.substring(6, 12));
                        if ((endNum - startNum) >= 500 || (endNum - startNum) < 0) {
                            final android.app.AlertDialog.Builder normalDialog = new android.app.AlertDialog.Builder(FormingActivity.this);
                            normalDialog.setTitle("提示");
                            normalDialog.setMessage("开始条码为：" + ed_StartCode.getText().toString() + "，结束条码为：" + endCode + ",数量超过500或数量小于等于0，请确认结束条码是否正确");
                            normalDialog.setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startNoti(FormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            normalDialog.setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startAlarm(FormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            // 显示
                            normalDialog.show();
                            return;
                        }

                        //上一计划结束条码
                        if (!StringUtil.isNullOrEmpty(preEndCode)) {
                            preEndCode = "";
                        }
                        preEndCode = ed_EndCode.getText().toString().trim();
                        //如果开始条码发生改变则进行修改开始条码操作，如果没有则直接结束上一计划
                        if (ed_StartCode.getText().toString().equals(vplan.getBarcodestart())) {
                            //执行结束上一计划
                            String param = "VPLANID=" + vplan.getId() + "&EndBarcode=" + preEndCode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                            new DIALOGFINISHTask().execute(param);
                        } else if (!ed_StartCode.getText().toString().equals(vplan.getBarcodestart())) {
                            //执行开始计划接口更新开始条码
                            String param = "VPLANID=" + vplan.getId() + "&StartBarcode=" + ed_StartCode.getText().toString() + "&Num=200" + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                            new PRESTARTTask().execute(param);
                        }

                    }else{
                        //如果为空则进行操作
                        if (StringUtil.isNullOrEmpty(endCode) || StringUtil.isNullOrEmpty(ed_StartCode.getText().toString())) {
                            Toast.makeText(FormingActivity.this, "条码为空，请输入！", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (endCode.length() != 12 || ed_StartCode.getText().toString().length() != 12) {
                            Toast.makeText(FormingActivity.this, "条码规格不正确，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String endjt = endCode.substring(4, 6);
                        String startjt = ed_StartCode.getText().toString().substring(4, 6);
                        if (!endjt.equals(startjt)) {
                            Toast.makeText(FormingActivity.this, "开始条码与结束条码机台不一致，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //判断是否跨年
                        String srart = ed_StartCode.getText().toString().substring(0, 4);
                        String end = endCode.substring(0, 4);
                        if (!srart.equals(end)) {
                            Toast.makeText(FormingActivity.this, "条码不能跨年，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //判断是否超过500
                        Integer startNum = Integer.valueOf(ed_StartCode.getText().toString().substring(6, 12));
                        Integer endNum = Integer.valueOf(endCode.substring(6, 12));
                        if ((endNum - startNum) >= 500 || (endNum - startNum) < 0) {
                            final android.app.AlertDialog.Builder normalDialog = new android.app.AlertDialog.Builder(FormingActivity.this);
                            normalDialog.setTitle("提示");
                            normalDialog.setMessage("开始条码为：" + ed_StartCode.getText().toString() + "，结束条码为：" + endCode + ",数量超过500或数量小于等于0，请确认结束条码是否正确");
                            normalDialog.setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startNoti(FormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            normalDialog.setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startAlarm(FormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            // 显示
                            normalDialog.show();
                            return;
                        }

                        //上一计划结束条码
                        if (!StringUtil.isNullOrEmpty(preEndCode)) {
                            preEndCode = "";
                        }
                        preEndCode = ed_EndCode.getText().toString().trim();
                        //如果开始条码发生改变则进行修改开始条码操作，如果没有则直接结束上一计划
                        if (ed_StartCode.getText().toString().equals(vplan.getBarcodestart())) {
                            //执行结束上一计划
                            String param = "VPLANID=" + vplan.getId() + "&EndBarcode=" + preEndCode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                            new DIALOGFINISHTask().execute(param);
                        } else if (!ed_StartCode.getText().toString().equals(vplan.getBarcodestart())) {
                            //执行开始计划接口更新开始条码
                            String param = "VPLANID=" + vplan.getId() + "&StartBarcode=" + ed_StartCode.getText().toString() + "&Num=200" + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                            new PRESTARTTask().execute(param);
                        }
                    }

                }else{
                    Toast.makeText(FormingActivity.this, "成型验校验为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                //提示音
                SoundPlayUtils.startNoti(FormingActivity.this);
                SoundPlayUtils.stopAlarm();
            }
        });
    }

    //完成操作
    public void finishPlan() {
        final MaterialDialog dialog = new MaterialDialog.Builder(FormingActivity.this)
                .customView(R.layout.dialog_finish, true)
                .show();
        //控件
        View customeView = dialog.getCustomView();
        final TextView itnbr = customeView.findViewById(R.id.input);
        final TextView itdec = customeView.findViewById(R.id.input4);
        final EditText ed_StartCode = customeView.findViewById(R.id.input2);
        final EditText ed_EndCode = dialog.findViewById(R.id.input3);
        //设置扫描框输入字符数
        ed_StartCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        ed_EndCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        //获取焦点
        ed_EndCode.requestFocus();
        if (vplan != null) {
            itnbr.setText(v.getItnbr());
            itdec.setText(v.getItdsc());
            if (v.getBarcodestart() == null) {
                ed_StartCode.setText(startCode);
            } else {
                ed_StartCode.setText(v.getBarcodestart());
            }
            ed_EndCode.setText(v.getBarcodeend());
        }
        Button returnDialog = customeView.findViewById(R.id.finish);
        returnDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                //提示音
                SoundPlayUtils.startAlarm(FormingActivity.this);
                SoundPlayUtils.stopAlarm();
            }
        });
        Button ok = customeView.findViewById(R.id.ok);
        //取消焦点
        ok.setFocusable(false);
        returnDialog.setFocusable(false);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v1) {
                String jt = mchid;
                jt = jt.substring(jt.length() - 2, jt.length());
                //当前计划结束条码
                if (!StringUtil.isNullOrEmpty(endCode)) {
                    endCode = "";
                }
                endCode = ed_EndCode.getText().toString();

                //成型验证校验
                if(!StringUtil.isNullOrEmpty(cxjy)){
                    if(cxjy.equals("1")){
                        //如果为空则进行操作
                        if (StringUtil.isNullOrEmpty(endCode) || StringUtil.isNullOrEmpty(ed_StartCode.getText().toString())) {
                            Toast.makeText(FormingActivity.this, "条码为空，请输入！", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (endCode.length() != 12 || ed_StartCode.getText().toString().length() != 12) {
                            Toast.makeText(FormingActivity.this, "条码规格不正确，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String endjt = endCode.substring(4, 6);
                        String startjt = ed_StartCode.getText().toString().substring(4, 6);
                        if (!jt.equals(endjt) || !jt.equals(startjt)) {
                            Toast.makeText(FormingActivity.this, "条码不属于此机台，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //判断条码是否跨年
                        String startYear = ed_StartCode.getText().toString().substring(0, 4);
                        String endYear = endCode.substring(0, 4);
                        if (!startYear.equals(endYear)) {
                            Toast.makeText(FormingActivity.this, "条码不能跨年，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //判断是否超过500
                        Integer startNum = Integer.valueOf(ed_StartCode.getText().toString().substring(6, 12));
                        Integer endNum = Integer.valueOf(endCode.substring(6, 12));
                        if ((endNum - startNum) >= 500 || (endNum - startNum) < 0) {
                            final android.app.AlertDialog.Builder normalDialog = new android.app.AlertDialog.Builder(FormingActivity.this);
                            normalDialog.setTitle("提示");
                            normalDialog.setMessage("开始条码为：" + ed_StartCode.getText().toString() + "，结束条码为：" + endCode + ",数量超过500或数量小于等于0，请确认结束条码是否正确");
                            normalDialog.setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startNoti(FormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            normalDialog.setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startAlarm(FormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            // 显示
                            normalDialog.show();
                            return;
                        }
                        //判断当前计划是刚开始还是已经开始，根据获取对象中有无开始条码来判断
                        if (!StringUtil.isNullOrEmpty(v.getBarcodestart())) {
                            //如果开始条码不一致怎更新开始条码在结束，如果一致则直接结束计划
                            if (!ed_StartCode.getText().toString().equals(v.getBarcodestart())) {
                                //执行开始计划接口更新开始条码
                                String param = "VPLANID=" + currid + "&StartBarcode=" + ed_StartCode.getText().toString() + "&Num=200" + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                                new THISSTARTTask().execute(param);
                            } else {
                                //执行结束当前计划
                                String param = "VPLANID=" + currid + "&EndBarcode=" + endCode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                                new FINISHTask().execute(param);
                            }
                        } else {
                            //如果开始条码不一致怎更新开始条码在结束，如果一致则直接结束计划
                            if (!ed_StartCode.getText().toString().equals(startCode)) {
                                //执行开始计划接口更新开始条码
                                String param = "VPLANID=" + currid + "&StartBarcode=" + startCode + "&Num=200" + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                                new THISSTARTTask().execute(param);
                            } else {
                                //执行结束当前计划
                                String param = "VPLANID=" + currid + "&EndBarcode=" + endCode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                                new FINISHTask().execute(param);
                            }
                        }
                        //设置数量
                        Integer pNum = 0;
                        pNum = Integer.valueOf(endCode.substring(6, 12)) - Integer.valueOf(ed_StartCode.getText().toString().substring(6, 12)) + 1;
                        //清空数据
                        startCode = "";
                        //设置状态
                        tvState.setText("");
                        tvState.setText("已完成");
                        tvPNum.setText("");
                        tvPNum.setText(pNum + "");
                        //返回按钮可用
                        btStart.setEnabled(false);
                        btUpdate.setEnabled(false);
                        btFinish.setEnabled(false);
                        btOut.setEnabled(true);

                    }else{
                        //如果为空则进行操作
                        if (StringUtil.isNullOrEmpty(endCode) || StringUtil.isNullOrEmpty(ed_StartCode.getText().toString())) {
                            Toast.makeText(FormingActivity.this, "条码为空，请输入！", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (endCode.length() != 12 || ed_StartCode.getText().toString().length() != 12) {
                            Toast.makeText(FormingActivity.this, "条码规格不正确，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String endjt = endCode.substring(4, 6);
                        String startjt = ed_StartCode.getText().toString().substring(4, 6);
                        if (!endjt.equals(startjt)) {
                            Toast.makeText(FormingActivity.this, "开始条码与结束条码机台不一致，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //判断条码是否跨年
                        String startYear = ed_StartCode.getText().toString().substring(0, 4);
                        String endYear = endCode.substring(0, 4);
                        if (!startYear.equals(endYear)) {
                            Toast.makeText(FormingActivity.this, "条码不能跨年，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //判断是否超过500
                        Integer startNum = Integer.valueOf(ed_StartCode.getText().toString().substring(6, 12));
                        Integer endNum = Integer.valueOf(endCode.substring(6, 12));
                        if ((endNum - startNum) >= 500 || (endNum - startNum) < 0) {
                            final android.app.AlertDialog.Builder normalDialog = new android.app.AlertDialog.Builder(FormingActivity.this);
                            normalDialog.setTitle("提示");
                            normalDialog.setMessage("开始条码为：" + ed_StartCode.getText().toString() + "，结束条码为：" + endCode + ",数量超过500或数量小于等于0，请确认结束条码是否正确");
                            normalDialog.setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startNoti(FormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            normalDialog.setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startAlarm(FormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            // 显示
                            normalDialog.show();
                            return;
                        }
                        //判断当前计划是刚开始还是已经开始，根据获取对象中有无开始条码来判断
                        if (!StringUtil.isNullOrEmpty(v.getBarcodestart())) {
                            //如果开始条码不一致怎更新开始条码在结束，如果一致则直接结束计划
                            if (!ed_StartCode.getText().toString().equals(v.getBarcodestart())) {
                                //执行开始计划接口更新开始条码
                                String param = "VPLANID=" + currid + "&StartBarcode=" + ed_StartCode.getText().toString() + "&Num=200" + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                                new THISSTARTTask().execute(param);
                            } else {
                                //执行结束当前计划
                                String param = "VPLANID=" + currid + "&EndBarcode=" + endCode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                                new FINISHTask().execute(param);
                            }
                        } else {
                            //如果开始条码不一致怎更新开始条码在结束，如果一致则直接结束计划
                            if (!ed_StartCode.getText().toString().equals(startCode)) {
                                //执行开始计划接口更新开始条码
                                String param = "VPLANID=" + currid + "&StartBarcode=" + startCode + "&Num=200" + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                                new THISSTARTTask().execute(param);
                            } else {
                                //执行结束当前计划
                                String param = "VPLANID=" + currid + "&EndBarcode=" + endCode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                                new FINISHTask().execute(param);
                            }
                        }
                        //设置数量
                        Integer pNum = 0;
                        pNum = Integer.valueOf(endCode.substring(6, 12)) - Integer.valueOf(ed_StartCode.getText().toString().substring(6, 12)) + 1;
                        //清空数据
                        startCode = "";
                        //设置状态
                        tvState.setText("");
                        tvState.setText("已完成");
                        tvPNum.setText("");
                        tvPNum.setText(pNum + "");
                        //返回按钮可用
                        btStart.setEnabled(false);
                        btUpdate.setEnabled(false);
                        btFinish.setEnabled(false);
                        btOut.setEnabled(true);
                    }
                }else{
                    Toast.makeText(FormingActivity.this, "成型验校验为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                //提示音
                SoundPlayUtils.startNoti(FormingActivity.this);
                SoundPlayUtils.stopAlarm();
            }
        });

    }

    //查询有没有已完成的计划
    class STARTTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.START, strs[0]);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                isNull = 3;
                return;
            }
            try {
                Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                }.getType());
                List<VPlan> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VPlan>>() {
                }.getType());
                if (res == null || res.isEmpty()) {
                    Toast.makeText(FormingActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (res != null) {
                    if (res.get("code").equals("500")) {//返回空
                        isNull = 1;
                        return;
                    } else if (res.get("code").equals("400")) {//报错异常
                        isNull = 0;
                        return;
                    } else {
                        vplan = datas.get(0);
                        isNull = 2;
                        return;
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    //执行开始按钮
    class GETSTARTTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.GETSTART, strs[0]);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
//                        returnPager();
                    } else {
                        Toast.makeText(FormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //执行修改按钮
    class UPDATETask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.UPDATE, strs[0]);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingActivity.this, "修改失败！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
//                        returnPager();
                    } else {
                        Toast.makeText(FormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingActivity.this, "修改失败！", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //修改上一计划开始条码
    class PRESTARTTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.GETSTART, strs[0]);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //执行结束上一计划操作
                        String param = "VPLANID=" + vplan.getId() + "&EndBarcode=" + preEndCode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                        new DIALOGFINISHTask().execute(param);
                    } else {
                        Toast.makeText(FormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //修改当前计划开始条码
    class THISSTARTTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.GETSTART, strs[0]);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //执行操作接口
                        String param = "VPLANID=" + currid + "&EndBarcode=" + endCode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                        new FINISHTask().execute(param);
                    } else {
                        Toast.makeText(FormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //结束上一个计划
    class DIALOGFINISHTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.FINISH, strs[0]);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingActivity.this, "操作失败！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //弹窗执行开始计划
                        dialogToStart();
                    } else {
                        Toast.makeText(FormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingActivity.this, "操作失败！", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //执行完成按钮
    class FINISHTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.FINISH, strs[0]);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingActivity.this, "操作失败！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
//                        returnPager();
                    } else {
                        Toast.makeText(FormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingActivity.this, "操作失败！", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //根据TYPEID 获取数据字典内容  成型机台
    class MCHIDTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.FORMINGSELECTMCHID, strs[0]);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    List<Map<String, String>> map = (List<Map<String, String>>) res.get("data");
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        for (int i = 0; i < map.size(); i++) {
                            if (map.get(i).get("itemid") == null) {
                                continue;
                            }
                            data1.add(map.get(i).get("itemid"));
                        }
                    } else {
                        Toast.makeText(FormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //获取成型计划
    class GetFormingPlanTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.FORMINGPLAN, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {

            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VPlan> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VPlan>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        boolean zxz = false;//执行中
                        boolean ywc = false;//已完成
                        for (int j = 0; j < datas.size(); j++) {
                            if (datas.get(j).getState().equals("30")) {
                                zxz = true;
                                continue;
                            }
                            if (datas.get(j).getState().equals("40")) {
                                ywc = true;
                                continue;
                            }
                        }
                        if (!zxz && !ywc) {//没有正在执行和已完成的
                            for (int i = 0; i < datas.size(); i++) {
                                datas.get(i).setBtnflag("1");//显示开始和返回
                            }
                        } else if (zxz && !ywc) {//有正在执行
                            for (int i = 0; i < datas.size(); i++) {
                                if (datas.get(i).getState().equals("30")) {
                                    datas.get(i).setBtnflag("2");//只显示修改和完成
                                } else {
                                    datas.get(i).setBtnflag("3");//只有返回
                                }
                            }
                        } else if (ywc && !zxz) {//有已完成
                            for (int i = 0; i < datas.size(); i++) {
                                datas.get(i).setBtnflag("3");//只有返回
                            }
                        } else {//有执行中和已完成的
                            for (int i = 0; i < datas.size(); i++) {
                                if (datas.get(i).getState().equals("30")) {
                                    datas.get(i).setBtnflag("2");//只显示修改和完成
                                } else {
                                    datas.get(i).setBtnflag("3");//只有返回
                                }
                            }
                        }

                        //展示计划列表
                        List<VPlan> newDatas = new ArrayList<>();
                        for (int n1 = 0; n1 < datas.size(); n1++) {
                            if (datas.get(n1).getState().equals("30")) {
                                newDatas.add(datas.get(n1));
                            }
                        }
                        for (int n2 = 0; n2 < datas.size(); n2++) {
                            if (datas.get(n2).getState().equals("20")) {
                                newDatas.add(datas.get(n2));
                            }
                        }
                        for (int n3 = 0; n3 < datas.size(); n3++) {
                            if (datas.get(n3).getState().equals("40")) {
                                newDatas.add(datas.get(n3));
                            }
                        }
                        adapter = new FormingItemAdapter(FormingActivity.this, newDatas);
                        lvPlan.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
//                        Toast.makeText(FormingActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG).show();
                    } else {
                        //查询为空时清空数据显示提示
                        datas.clear();
                        adapter = new FormingItemAdapter(FormingActivity.this, datas);
                        lvPlan.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(FormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }

            }
        }
    }

    //获取数据字典内容  校验
    class CXJYTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.GetDictionaries, strs[0]);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            cxjy = "";
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    List<Map<String, String>> map = (List<Map<String, String>>) res.get("data");
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingActivity.this, "未获取到校验信息", Toast.LENGTH_LONG).show();
                    }
                    if (res.get("code").equals("200")) {
                        cxjy = map.get(0).get("itemname");
                        System.out.println("成型验证校验==="+cxjy);
                    }  else {
                        Toast.makeText(FormingActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingActivity.this, "数据处理异常", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //用户提示信息
    public void error(String v) {
        final android.app.AlertDialog.Builder normalDialog = new android.app.AlertDialog.Builder(this);
        normalDialog.setTitle("提示");
        normalDialog.setMessage(v);
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        normalDialog.setNegativeButton("取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        // 显示
        normalDialog.show();
    }

    //键盘监听
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("key", keyCode + "  ");
        //按键按下
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //按键弹开
        switch (keyCode) {
            case 22://右方向键
                getCurrentVPlan();//查询当前机台的生产计划
                break;
            case 4://返回键
                tofunction();//返回菜单栏
                break;
            case 131://F1键
                startPlan();//开始
                break;
            case 132://F2键
                updateNumber();//修改
                break;
            case 133://F3键
                finishPlan();//完成
                break;
            case 134://F4键
                returnPager();//返回
                break;
            default:
                break;
        }
        return true;
    }

}
