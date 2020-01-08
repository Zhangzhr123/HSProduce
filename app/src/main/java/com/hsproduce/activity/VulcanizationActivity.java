package com.hsproduce.activity;

import android.content.*;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.DialogItemAdapter;
import com.hsproduce.adapter.VPlanAdapter;
import com.hsproduce.adapter.VPlanItemAdapter;
import com.hsproduce.adapter.VPlanItnbrAdapter;
import com.hsproduce.bean.Result2;
import com.hsproduce.bean.VPlan;
import com.hsproduce.fragment.VulcanizationFragment;
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

/**
 * 硫化生产页面
 * 扫描机台号直接进入扫描条码页面，扫描条码，硫化计划更改为生产中，并将扫描的条码添加进生产实绩中
 * 成功添加进生产实绩后此条码写入扫描纪录框中
 * createBy zhangzhr @ 2019-12-21
 */
public class VulcanizationActivity extends BaseActivity {

    //view 输入机台  记录条码  扫描条码
    private View ll_Mchid, ll_CodeLog, ll_Code;
    //计划展示list
    private ListView listView;
    //机台号  轮胎条码 条码计数  条码记录
    private TextView tvBarCode, tvAnum, tvBarCodeLog, tvSum;
    private TextView tvMchid;
    //计划按钮   扫描按钮
    private ButtonView btGetplan, btBarCode_Ok;
    //加载
    private MiniLoadingView loadingView;
    //计划展示适配器
    private VPlanAdapter adapter;
    //声明一个long类型变量：用于存放上一点击“返回键”的时刻
//    private long mExitTime = 0;
    //保证每次操作完成后再进行下一次操作
//    public boolean iscomplate = true;
    //绑定条码个数
    private int number = 0;
    //轮胎条码
    private String barCode = "", planId = "";
    //    private List<String> list = new ArrayList<>();
    //添加条码防止重复扫描
    private List<String> codeList = new ArrayList<>();
    private Boolean isVual = false;
    //判断弹窗是否已经存在
    private MaterialDialog materialDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_vulcanization);
        //加载控件
        initView();
        //设置控件事件
        initEvent();
    }

    //初始化控件
    public void initView() {
        //layout
        ll_Code = findViewById(R.id.ll_code);
        ll_Mchid = findViewById(R.id.ll_mchid);
        ll_CodeLog = findViewById(R.id.ll_codelog);
        //list列表
        listView = (ListView) findViewById(R.id.lv_plan);
        //扫描框
        tvMchid = (TextView) findViewById(R.id.mchid);
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
        //机台本班次累计数量
        tvSum = (TextView) findViewById(R.id.sum);
        //获取计划按钮
        btGetplan = (ButtonView) findViewById(R.id.bt_getPlan);
        //条码按钮
        btBarCode_Ok = (ButtonView) findViewById(R.id.barcode_ok);
        //加载条
        loadingView = (MiniLoadingView) findViewById(R.id.loading);
        //设置扫描框输入字符数
        tvBarCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        tvMchid.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
    }


    //初始化事件
    public void initEvent() {
        //获取机台计划
        btGetplan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //显示加载
                loadingView.setVisibility(View.VISIBLE);
                //获取计划
                getPlan();
            }
        });
        //获取扫描条码绑定计划
        btBarCode_Ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBarCode();
            }
        });
    }

    //获取计划
    public void getPlan() {
        //获取输入机台上barcode
        String mchid = tvMchid.getText().toString().trim();
        if (!StringUtil.isNullOrEmpty(mchid)) {
            mchid = mchid.toUpperCase();
        }
        if (StringUtil.isNullOrEmpty(mchid)) {
            Toast.makeText(VulcanizationActivity.this, "请扫描机台号", Toast.LENGTH_LONG).show();
        } else {
            String param = "MCHIDLR=" + mchid + "&SHIFT=" + App.shift;
            new MyTask().execute(param);
        }
    }

    //获取轮胎条码
    public void getBarCode() {
        //获取轮胎上barcode
        barCode = tvBarCode.getText().toString().trim();
        if (StringUtil.isNullOrEmpty(barCode)) {
            Toast.makeText(VulcanizationActivity.this, "请扫描轮胎条码", Toast.LENGTH_LONG).show();
        } else {
            //扫描记录中是否已经存在该条码，存在提示已扫描，不存在调用接口记录硫化记录
            if (codeList.contains(barCode)) {
                Toast.makeText(VulcanizationActivity.this, "此条码已经扫描", Toast.LENGTH_LONG).show();
                tvBarCode.setText("");
            } else {
                //记录硫化生产记录
                String param1 = "PLAN_ID=" + planId + "&barcode=" + barCode + "&User_Name=" + App.username + "&TEAM=" + App.shift + "&doit=0";
                new TypeCodeTask().execute(param1);
            }
        }
    }

    //查询任务
    class MyTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.VUL_GET_PLAN, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(VulcanizationActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VPlan> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VPlan>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(VulcanizationActivity.this, "未获取到数据", Toast.LENGTH_LONG).show();
                    }
                    if (res.get("code").equals("200")) {
                        //获取计划ID
                        planId = datas.get(0).getId();
                        //显示生产
                        showVual();
                        //展示列表
                        adapter = new VPlanAdapter(VulcanizationActivity.this, datas);
                        listView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        number = Integer.valueOf(datas.get(0).getDnum());
                        tvSum.setText(datas.get(0).getDnum());
                        tvMchid.setText("");
                    } else if (res.get("code").equals("300")) {
                        Toast.makeText(VulcanizationActivity.this, "机台号不正确！", Toast.LENGTH_LONG).show();
                    } else if (res.get("code").equals("500")) {
                        Toast.makeText(VulcanizationActivity.this, "查询成功，没有匹配的计划！", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(VulcanizationActivity.this, "计划查询错误，请重新操作！", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(VulcanizationActivity.this, "数据处理异常", Toast.LENGTH_LONG).show();
                }

            }
        }
    }

    //显示硫化生产
    private void showVual() {
        //关闭加载
        loadingView.setVisibility(View.GONE);
        ll_Mchid.setVisibility(View.GONE);
        //显示硫化扫描
        listView.setVisibility(View.VISIBLE);
        ll_Code.setVisibility(View.VISIBLE);
        ll_CodeLog.setVisibility(View.VISIBLE);
        //获得焦点
        tvBarCode.requestFocus();
        isVual = true;
    }

    //显示扫描机台
    private void showMchid() {
        ll_Mchid.setVisibility(View.VISIBLE);
        //显示硫化扫描
        listView.setVisibility(View.GONE);
        ll_Code.setVisibility(View.GONE);
        ll_CodeLog.setVisibility(View.GONE);
        //获得焦点
        tvMchid.requestFocus();
        isVual = false;
    }

    //新增硫化生产记录
    class TypeCodeTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.VUL_AddActualAchievement, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(VulcanizationActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(VulcanizationActivity.this, "未获取到数据", Toast.LENGTH_LONG).show();
                    }
                    if (res.get("code").equals("200")) {
                        tvBarCodeLog.append(barCode + "\n");
                        tvBarCode.setText("");
                        codeList.add(barCode);
                        tvAnum.setText(codeList.size() + "");
                        tvSum.setText((number+codeList.size())+"");
                    } else if (res.get("code").equals("100")) {
                        Toast.makeText(VulcanizationActivity.this, "扫描条码位数不正确！", Toast.LENGTH_LONG).show();
                    } else if (res.get("code").equals("300")) {
                        Toast.makeText(VulcanizationActivity.this, barCode + ":" + res.get("msg") + "", Toast.LENGTH_LONG).show();
                    } else if (res.get("code").equals("400")) {
                        //提示音
                        SoundPlayUtils.playSoundByMedia(VulcanizationActivity.this,R.raw.raw3);

                        if (materialDialog == null) {
                            materialDialog = new MaterialDialog.Builder(VulcanizationActivity.this)
                                    .title("提示")
                                    .content(res.get("msg") + "")
                                    .positiveText(R.string.vul_confirm)
                                    .negativeText(R.string.vul_cancel)
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            //强制记录硫化生产记录
//                                        iscomplate = false;
                                            String param1 = "PLAN_ID=" + planId + "&barcode=" + barCode + "&User_Name=" + App.username + "&TEAM=" + App.shift + "&doit=1";
                                            new TypeCodeTask().execute(param1);
                                            //提示音
                                            SoundPlayUtils.startNoti(VulcanizationActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    })
                                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            //提示音
                                            SoundPlayUtils.startAlarm(VulcanizationActivity.this);
                                            SoundPlayUtils.stopAlarm();
                                        }
                                    })
                                    .cancelable(false)
                                    .show();
                        }
                        materialDialog.show();

                    } else {
                        Toast.makeText(VulcanizationActivity.this, "错误，条码未识别！", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(VulcanizationActivity.this, "数据处理异常", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

//    //扫描条码绑定计划
//    class CodeInPlanTask extends AsyncTask<String, Void, String> {
//        @Override
//        protected String doInBackground(String... strings) {
//            String result = HttpUtil.sendGet(PathUtil.VUL_AddActualAchievement, strings[0]);
//            return result;
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
////            iscomplate = true;
//            if (StringUtil.isNullOrBlank(s)) {
//                Toast.makeText(VulcanizationActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
//            } else {
//                try {
//                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
//                    }.getType());
//                    if (res == null || res.isEmpty()) {
//                        Toast.makeText(VulcanizationActivity.this, "未获取到数据", Toast.LENGTH_LONG).show();
//                    }
//                    if (res.get("code").equals("200")) {
//                        codeList.add(barCode);
//                        //显示绑定条码数量
//                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
//                        list.add("[" + date + "]" + barCode);
//                        //list.add(tvbarcode);
//                        tvBarCodeLog.setText("");
//                        System.out.println(list.size());
//                        for (int i = 0; i < list.size(); i++) {
//                            if (i == 0) {
//                                tvBarCodeLog.setText(list.get(i));
//                            } else {
//                                tvBarCodeLog.setText(getlog(list));
//                            }
//                        }
//                        tvAnum.setText("");
//                        number++;//计算成功次数
//                        tvAnum.setText(number + "");
//                        tvBarCode.setText("");
////                        Toast.makeText(VulcanizationActivity.this, "扫描成功！", Toast.LENGTH_LONG).show();
//                    } else if (res.get("code").equals("100")) {
//                        Toast.makeText(VulcanizationActivity.this, "扫描条码位数不正确！", Toast.LENGTH_LONG).show();
//                    } else if (res.get("code").equals("300")) {
//                        Toast.makeText(VulcanizationActivity.this, "扫描插入数据库失败！", Toast.LENGTH_LONG).show();
//                    } else {
//                        Toast.makeText(VulcanizationActivity.this, "错误：" + res.get("ex"), Toast.LENGTH_LONG).show();
//                    }
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Toast.makeText(VulcanizationActivity.this, "数据处理异常", Toast.LENGTH_LONG).show();
//                }
//            }
//        }
//    }

    //递归显示
//    public String getlog(List<String> list) {
//        String logstr = "";
//        Collections.reverse(list);//倒序
//        for (int i = 0; i < list.size(); i++) {
//            logstr += list.get(i) + "\n";
//        }
//        return logstr;
//    }


    //键盘监听
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("key", keyCode + "  ");

        switch (keyCode) {
            case 0:
                if (isVual) {
                    tvBarCode.requestFocus();
                } else {
                    tvMchid.requestFocus();
                }
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
            //返回键
            case 4:
                //返回上级页面
                //先返回扫描机台，再返回功能页
                if (isVual) {
                    codeList.clear();
                    tvBarCodeLog.setText("");
                    tvBarCode.setText("");
                    tvMchid.setText("");
                    tvAnum.setText("0");
                    showMchid();
                } else {
                    startActivity(new Intent(VulcanizationActivity.this, FunctionActivity.class));
                    this.finish();
                }
                break;
            //右方向键
            case 22:
                msg = "扫描失败！";
                operate(msg);
                break;
            //回车键
            case 66:
                msg = "扫描失败！";
                operate(msg);
                break;
            //扫描键
            case 0:
                msg = "扫描失败！";
                operate(msg);
                break;
            default:

                break;

        }
        return true;
    }

    private void operate(String msg) {
//        if (!iscomplate) {
////            Toast.makeText(this, "请等待上一次操作完成再继续！", Toast.LENGTH_SHORT).show();
//            return;
//        }
        if (!StringUtil.isNullOrEmpty(tvBarCode.getText().toString().trim())) {
            getBarCode();
        } else if (!StringUtil.isNullOrEmpty(tvMchid.getText().toString().trim())) {
            getPlan();
        } else {
//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }


}