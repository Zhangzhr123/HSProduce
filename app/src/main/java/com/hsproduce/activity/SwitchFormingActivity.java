package com.hsproduce.activity;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.FormingAdapter;
import com.hsproduce.adapter.FormingReplAdapter;
import com.hsproduce.adapter.VPlanItnbrAdapter;
import com.hsproduce.adapter.VPlanReplAdapter;
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

/**
 * 成型规格交替页面
 * 输入机台号，查询计划中有生产中或者已完成并且有等待中的计划可以进行规格交替，只有等待中则不可以规格交替
 * 点击规格交替按钮如果有生产中的计划结束这一计划，成功后开始下一计划
 * createBy zhangzhr @ 2019-12-21
 */
public class SwitchFormingActivity extends BaseActivity {

    private LinearLayout llShowList, llMchId;
    private TableLayout tlShowVPlan;
    private LinearLayout llOnClick;
    private Button btRepl, btOut;
    private TextView tvSpesc, tvSpescName, tvPro, tvState, tvPNum;
    //当前计划展示list  规格交替列表
    private ListView lvPlan;
    private VPlan v = new VPlan();
    //输入框
    private AutoCompleteTextView tvMchid;
    private List<String> data1 = new ArrayList<>();
    //获取计划按钮
    private ButtonView btGetPlan;
    //计划展示适配器  规格交替适配器
    private FormingReplAdapter adaprer;
    //定义变量 当前计划ID
    private String currid = "";
    public String mchid = "";
    public Integer isNull = 0;//判断开始按钮弹窗显示
    private VPlan vplan = new VPlan();
    private String num = "";
    //是否提示错误
    private Boolean isShow = true;
    private String preEndCode = "";//上一计划结束条码
    private String cxjy = "";//成型验证校验

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_switchforming);
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
        tvMchid = (AutoCompleteTextView) findViewById(R.id.mchid);
        new MCHIDTask().execute("TYPE_ID=10107");
        eventsViews();
        //获取计划按钮
        btGetPlan = (ButtonView) findViewById(R.id.getSwitchPlan);
        //是否启用成型验证校验
        String parm = "TYPE_ID=10114";
        new CXJYTask().execute(parm);

    }

    private void eventsViews() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data1);
        tvMchid.setAdapter(adapter);
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
                if (currid != null || !currid.equals("")) {
                    currid = "";
                }
                //初始化一下控件属性
                btRepl.setEnabled(true);
                btOut.setEnabled(true);
                //获取点击的数据
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
        //切换规格
        btRepl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replace();
            }
        });

    }

    public void replace() {
        if (isNull == 1) {
            //开始计划
            dialogToStart();
            //返回上一页面，并且上一页面重新查询。

        }
        else if (isNull == 2) {
            //完成计划
            dialogToFinish();
            //返回上一页面，并且上一页面重新查询。

        }
        else if (isNull == 3) {
            Toast.makeText(SwitchFormingActivity.this, "网络连接异常，请重新登录。", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(SwitchFormingActivity.this, "数据获取异常，请联系管理员。", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    //开始计划
    public void dialogToStart() {
        //显示弹窗
        final MaterialDialog dialog = new MaterialDialog.Builder(SwitchFormingActivity.this)
                .customView(R.layout.dialog_product, true)
                .show();
        //控件
        View customeView = dialog.getCustomView();
        final EditText next = dialog.findViewById(R.id.input);
        final EditText number = dialog.findViewById(R.id.input2);
        Button finish = customeView.findViewById(R.id.finish);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                //提示音
                SoundPlayUtils.startAlarm(SwitchFormingActivity.this);
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
                            Toast.makeText(SwitchFormingActivity.this, "数量或开始条码为空，请输入！", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (nextCode.length() != 12) {
                            Toast.makeText(SwitchFormingActivity.this, "开始条码规格不正确，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String nextjt = nextCode.substring(4, 6);
                        if (!jt.equals(nextjt)) {
                            Toast.makeText(SwitchFormingActivity.this, "开始条码不属于此机台，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Integer sum = Integer.valueOf(nextCode.substring(6, 12)) + Integer.valueOf(num);
                        if (Integer.valueOf(num) > 500 || sum > 999999) {
                            Toast.makeText(SwitchFormingActivity.this, "数量不能大于500或者条码流水号不能大于999999", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //如果存在上一个未完成的计划，结束上一计划
//                if (vplan != null && isNull == 2) {
//                    int endnumber = Integer.valueOf(nextCode.substring(6, 12)) - 1;
//                    String endBarcode = String.format("%06d", endnumber);
//                    endBarcode = (nextCode.substring(0, 6)) + endBarcode;
//                    String param1 = "VPLANID=" + vplan.getId() + "&EndBarcode=" + endBarcode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
//                    new FINISHTask().execute(param1);
//                }
                        //执行操作接口
                        String param = "VPLANID=" + currid + "&StartBarcode=" + nextCode + "&Num=" + num + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                        new GETSTARTTask().execute(param);
                        //显示数据
                        tvState.setText("");
                        tvState.setText("生产中");
                        tvPNum.setText("");
                        tvPNum.setText(num);
                        //开始按钮不可用
                        btRepl.setEnabled(false);
                        btOut.setEnabled(true);

                    }else{
                        //如果为空则进行操作
                        if (num.equals("") || Integer.valueOf(num) <= 0 || nextCode.equals("")) {
                            Toast.makeText(SwitchFormingActivity.this, "数量或开始条码为空，请输入！", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (nextCode.length() != 12) {
                            Toast.makeText(SwitchFormingActivity.this, "开始条码规格不正确，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
//                        String nextjt = nextCode.substring(4, 6);
//                        if (!jt.equals(nextjt)) {
//                            Toast.makeText(SwitchFormingActivity.this, "开始条码不属于此机台，请重新输入", Toast.LENGTH_SHORT).show();
//                            return;
//                        }
                        Integer sum = Integer.valueOf(nextCode.substring(6, 12)) + Integer.valueOf(num);
                        if (Integer.valueOf(num) > 500 || sum > 999999) {
                            Toast.makeText(SwitchFormingActivity.this, "数量不能大于500或者条码流水号不能大于999999", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //如果存在上一个未完成的计划，结束上一计划
//                if (vplan != null && isNull == 2) {
//                    int endnumber = Integer.valueOf(nextCode.substring(6, 12)) - 1;
//                    String endBarcode = String.format("%06d", endnumber);
//                    endBarcode = (nextCode.substring(0, 6)) + endBarcode;
//                    String param1 = "VPLANID=" + vplan.getId() + "&EndBarcode=" + endBarcode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
//                    new FINISHTask().execute(param1);
//                }
                        //执行操作接口
                        String param = "VPLANID=" + currid + "&StartBarcode=" + nextCode + "&Num=" + num + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                        new GETSTARTTask().execute(param);
                        //显示数据
                        tvState.setText("");
                        tvState.setText("生产中");
                        tvPNum.setText("");
                        tvPNum.setText(num);
                        //开始按钮不可用
                        btRepl.setEnabled(false);
                        btOut.setEnabled(true);
                    }
                }else{
                    Toast.makeText(SwitchFormingActivity.this, "成型验校验为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                //提示音
                SoundPlayUtils.startNoti(SwitchFormingActivity.this);
                SoundPlayUtils.stopAlarm();
            }
        });

    }

    //开始按钮中的完成上一计划
    public void dialogToFinish() {
        final MaterialDialog dialog = new MaterialDialog.Builder(SwitchFormingActivity.this)
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
        Button finish = customeView.findViewById(R.id.finish);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                //提示音
                SoundPlayUtils.startAlarm(SwitchFormingActivity.this);
                SoundPlayUtils.stopAlarm();
            }
        });
        Button ok = customeView.findViewById(R.id.ok);
        //取消焦点
        ok.setFocusable(false);
        finish.setFocusable(false);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String jt = mchid;
                jt = jt.substring(jt.length() - 2, jt.length());
                String endCode = ed_EndCode.getText().toString();

                //成型验证校验
                if(!StringUtil.isNullOrEmpty(cxjy)) {
                    if (cxjy.equals("1")) {
                        //如果为空则进行操作
                        if (endCode.equals("") || StringUtil.isNullOrEmpty(ed_StartCode.getText().toString())) {
                            Toast.makeText(SwitchFormingActivity.this, "条码为空，请输入！", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (endCode.length() != 12 || ed_StartCode.getText().toString().length() != 12) {
                            Toast.makeText(SwitchFormingActivity.this, "条码规格不正确，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String endjt = endCode.substring(4, 6);
                        String startjt = ed_StartCode.getText().toString().substring(4, 6);
                        if (!jt.equals(endjt) || !jt.equals(startjt)) {
                            Toast.makeText(SwitchFormingActivity.this, "条码不属于此机台，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String start = ed_StartCode.getText().toString().substring(0, 4);
                        String end = endCode.substring(0, 4);
                        if (!start.equals(end)) {
                            Toast.makeText(SwitchFormingActivity.this, "条码不能跨年，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //判断数量是否正确
                        Long startSum = Long.parseLong(ed_StartCode.getText().toString().substring(0, 12));
                        Long endSum = Long.parseLong(endCode.substring(0, 12));
                        if ((endSum - startSum) >= 500 || (endSum - startSum) < 0) {
                            final android.app.AlertDialog.Builder normalDialog = new android.app.AlertDialog.Builder(SwitchFormingActivity.this);
                            normalDialog.setTitle("提示");
                            normalDialog.setMessage("开始条码为：" + ed_StartCode.getText().toString() + "，结束条码为：" + endCode + ",数量超过500或数量小于等于0，请确认结束条码是否正确");
                            normalDialog.setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startNoti(SwitchFormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            normalDialog.setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startAlarm(SwitchFormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });

                            // 显示
                            normalDialog.show();
                            return;
                        }

                        //判断数量是否正确
                        Integer startNum = Integer.valueOf(ed_StartCode.getText().toString().substring(6, 12));
                        Integer endNum = Integer.valueOf(endCode.substring(6, 12));
                        if ((endNum - startNum) >= 500 || (endNum - startNum) < 0) {
                            final android.app.AlertDialog.Builder normalDialog = new android.app.AlertDialog.Builder(SwitchFormingActivity.this);
                            normalDialog.setTitle("提示");
                            normalDialog.setMessage("开始条码为：" + ed_StartCode.getText().toString() + "，结束条码为：" + endCode + ",数量超过500或数量小于等于0，请确认结束条码是否正确");
                            normalDialog.setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startNoti(SwitchFormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            normalDialog.setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startAlarm(SwitchFormingActivity.this);
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
                            //执行结束上一计划操作
                            String param = "VPLANID=" + vplan.getId() + "&EndBarcode=" + preEndCode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                            new FINISHTask().execute(param);
                        } else if (!ed_StartCode.getText().toString().equals(vplan.getBarcodestart())) {
                            //执行开始计划接口更新开始条码
                            String param = "VPLANID=" + vplan.getId() + "&StartBarcode=" + ed_StartCode.getText().toString() + "&Num=200" + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                            new PRESTARTTask().execute(param);
                        }

                    }else{
                        //如果为空则进行操作
                        if (endCode.equals("") || StringUtil.isNullOrEmpty(ed_StartCode.getText().toString())) {
                            Toast.makeText(SwitchFormingActivity.this, "条码为空，请输入！", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (endCode.length() != 12 || ed_StartCode.getText().toString().length() != 12) {
                            Toast.makeText(SwitchFormingActivity.this, "条码规格不正确，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String endjt = endCode.substring(4, 6);
                        String startjt = ed_StartCode.getText().toString().substring(4, 6);
                        if (!endjt.equals(startjt)) {
                            Toast.makeText(SwitchFormingActivity.this, "看开始条码与结束条码的机台不一致，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String start = ed_StartCode.getText().toString().substring(0, 4);
                        String end = endCode.substring(0, 4);
                        if (!start.equals(end)) {
                            Toast.makeText(SwitchFormingActivity.this, "条码不能跨年，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //判断数量是否正确
                        Long startSum = Long.parseLong(ed_StartCode.getText().toString().substring(0, 12));
                        Long endSum = Long.parseLong(endCode.substring(0, 12));
                        if ((endSum - startSum) >= 500 || (endSum - startSum) < 0) {
                            final android.app.AlertDialog.Builder normalDialog = new android.app.AlertDialog.Builder(SwitchFormingActivity.this);
                            normalDialog.setTitle("提示");
                            normalDialog.setMessage("开始条码为：" + ed_StartCode.getText().toString() + "，结束条码为：" + endCode + ",数量超过500或数量小于等于0，请确认结束条码是否正确");
                            normalDialog.setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startNoti(SwitchFormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            normalDialog.setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startAlarm(SwitchFormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });

                            // 显示
                            normalDialog.show();
                            return;
                        }

                        //判断数量是否正确
                        Integer startNum = Integer.valueOf(ed_StartCode.getText().toString().substring(6, 12));
                        Integer endNum = Integer.valueOf(endCode.substring(6, 12));
                        if ((endNum - startNum) >= 500 || (endNum - startNum) < 0) {
                            final android.app.AlertDialog.Builder normalDialog = new android.app.AlertDialog.Builder(SwitchFormingActivity.this);
                            normalDialog.setTitle("提示");
                            normalDialog.setMessage("开始条码为：" + ed_StartCode.getText().toString() + "，结束条码为：" + endCode + ",数量超过500或数量小于等于0，请确认结束条码是否正确");
                            normalDialog.setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startNoti(SwitchFormingActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    });
                            normalDialog.setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            ed_EndCode.setText("");
                                            //提示音
                                            SoundPlayUtils.startAlarm(SwitchFormingActivity.this);
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
                            //执行结束上一计划操作
                            String param = "VPLANID=" + vplan.getId() + "&EndBarcode=" + preEndCode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                            new FINISHTask().execute(param);
                        } else if (!ed_StartCode.getText().toString().equals(vplan.getBarcodestart())) {
                            //执行开始计划接口更新开始条码
                            String param = "VPLANID=" + vplan.getId() + "&StartBarcode=" + ed_StartCode.getText().toString() + "&Num=200" + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                            new PRESTARTTask().execute(param);
                        }
                    }
                }else{
                    Toast.makeText(SwitchFormingActivity.this, "成型验校验为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                //提示音
                SoundPlayUtils.startNoti(SwitchFormingActivity.this);
                SoundPlayUtils.stopAlarm();
            }
        });

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
        //刷新数据
        String param1 = "MCHID=" + mchid + "&SHIFT=" + App.shift;
        new GetFormingPlanTask().execute(param1);
        adaprer.notifyDataSetChanged();
    }

    //根据状态查询计划
    public void getCurrentVPlan() {
        //获取输入机台上barcode
        mchid = tvMchid.getText().toString().trim();
        if (StringUtil.isNullOrEmpty(mchid)) {
//            Toast.makeText(SwitchFormingActivity.this, "请扫描机台号", Toast.LENGTH_LONG).show();
            return;
        } else {
            String param1 = "MCHID=" + mchid + "&SHIFT=" + App.shift;
            new GetFormingPlanTask().execute(param1);
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
                Toast.makeText(SwitchFormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    List<Map<String, String>> map = (List<Map<String, String>>) res.get("data");
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(SwitchFormingActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(SwitchFormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SwitchFormingActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SwitchFormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VPlan> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VPlan>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(SwitchFormingActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        List<VPlan> zxz = new ArrayList<>();//执行中
                        List<VPlan> ddz = new ArrayList<>();//等待中
                        List<VPlan> ywc = new ArrayList<>();//等待中
                        for (int j = 0; j < datas.size(); j++) {
                            if (datas.get(j).getState().equals("30")) {
                                zxz.add(datas.get(j));
                                continue;
                            }
                            if (datas.get(j).getState().equals("20")) {
                                ddz.add(datas.get(j));
                                continue;
                            }
                            if (datas.get(j).getState().equals("40")) {
                                ywc.add(datas.get(j));
                                continue;
                            }
                        }
                        if ((zxz.size() > 0 && ddz.size() > 0) || (ywc.size() > 0 && ddz.size() > 0)) {//有正在执行，并且有等待中的计划 或者有已完成和等待中的计划
                            //显示等待中的计划；
                            adaprer = new FormingReplAdapter(SwitchFormingActivity.this, ddz);
                            lvPlan.setAdapter(adaprer);
                            adaprer.notifyDataSetChanged();
                        } else {
                            //清空数据
                            ddz.clear();
                            adaprer = new FormingReplAdapter(SwitchFormingActivity.this, ddz);
                            lvPlan.setAdapter(adaprer);
                            adaprer.notifyDataSetChanged();
                            Toast.makeText(SwitchFormingActivity.this, "无可规格交替的计划！", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(SwitchFormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SwitchFormingActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }

            }
        }
    }

    //查询有没有正在执行的计划
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
                Toast.makeText(SwitchFormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(SwitchFormingActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {

                    } else {
                        Toast.makeText(SwitchFormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SwitchFormingActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SwitchFormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(SwitchFormingActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //执行结束上一计划操作
                        String param = "VPLANID=" + vplan.getId() + "&EndBarcode=" + preEndCode + "&TEAM=" + App.shift + "&User_Name=" + App.username;
                        new FINISHTask().execute(param);
                    } else {
                        Toast.makeText(SwitchFormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SwitchFormingActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SwitchFormingActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(SwitchFormingActivity.this, "操作失败！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //弹窗
                        dialogToStart();
                    } else {
                        Toast.makeText(SwitchFormingActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SwitchFormingActivity.this, "操作失败！", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(SwitchFormingActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    List<Map<String, String>> map = (List<Map<String, String>>) res.get("data");
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(SwitchFormingActivity.this, "未获取到校验信息", Toast.LENGTH_LONG).show();
                    }
                    if (res.get("code").equals("200")) {
                        cxjy = map.get(0).get("itemname");
                        System.out.println("成型验证校验==="+cxjy);
                    }  else {
                        Toast.makeText(SwitchFormingActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SwitchFormingActivity.this, "数据处理异常", Toast.LENGTH_LONG).show();
                }
            }
        }
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
        //弹开
        switch (keyCode) {
            case 22://右方向键
                getCurrentVPlan();//查询计划
                break;
            case 0://扫描键
                tvMchid.setText("");
                break;
            case 4://返回键
                tofunction();
                break;
            case 131://F1键
                replace();
                break;
            case 132://F2键
                returnPager();//返回上一页面
                break;
            default:
                break;
        }
        return true;
    }

}
