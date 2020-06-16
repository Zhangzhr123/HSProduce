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
import com.hsproduce.adapter.FormingPlanAdapter;
import com.hsproduce.bean.VPlan;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;
import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;

/**
 * 成型取消扫描页面
 * 输入机台号和时间查询已完成成型计划，点击计划展示信息扫描条码删除生产实绩
 * 广播监听回调触发响应事件
 * createBy zahngzr @ 2019-12-30
 */
public class DeleteFormingVreCordActivity extends BaseActivity {

    //展示页面
    private LinearLayout llPlan, llCode;
    //机台号和日期
    private TextView tvDate, tvSpesc, tvSpescName, tvPdate, tvMchid, tvOutBarCodeLog, tvNum;
    private EditText etBarCode;
    private ButtonView bvOutBarCode;
    private AutoCompleteTextView actvMchid;
    private List<String> data1 = new ArrayList<>();
    //获取计划按钮
    private ImageButton btGetplan;
    //成型计划
    private ListView lvPlan;
    private FormingPlanAdapter adapter;
    private VPlan vPlan = new VPlan();
    //机台和日期
    private String mchid = "", date = "", planID = "", barCode = "";
    //绑定条码个数
    private int number = 0;
    private List<String> list = new ArrayList<>();
    //返回上一级
    private Boolean isShow = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_delete_formingvrecord);
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
        //展示页面
        llPlan = findViewById(R.id.ll_plan);
        llCode = findViewById(R.id.ll_code);
        //机台号
        actvMchid = (AutoCompleteTextView) findViewById(R.id.mchid);
        new MCHIDTask().execute("TYPE_ID=10107");
        eventsViews();
        //日期
        tvDate = (TextView) findViewById(R.id.date);
        tvDate.setFocusable(false);//让EditText失去焦点，然后获取点击事件
        //list列表
        lvPlan = (ListView) findViewById(R.id.lv_plan);
        //点击查询阿牛
        btGetplan = (ImageButton) findViewById(R.id.getPlan);
        //展示信息控件
        tvSpesc = (TextView) findViewById(R.id.spesc);
        tvSpescName = (TextView) findViewById(R.id.spescname);
        tvMchid = (TextView) findViewById(R.id.tv_mchid);
        tvPdate = (TextView) findViewById(R.id.wdate);
        //条码扫描框
        etBarCode = (EditText) findViewById(R.id.barCode);
        //取消点击按钮
        bvOutBarCode = (ButtonView) findViewById(R.id.outBarCode);
        //扫描记录
        tvOutBarCodeLog = (TextView) findViewById(R.id.outBarCodeLog);
        tvNum = (TextView) findViewById(R.id.num);

    }

    private void eventsViews() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data1);
        actvMchid.setAdapter(adapter);
    }

    public void initEvent() {
        //点击查询成型计划
        btGetplan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentVPlan();
            }
        });
        //时间
        tvDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                // 直接创建一个DatePickerDialog对话框实例，并将它显示出来
                new DatePickerDialog(DeleteFormingVreCordActivity.this,
                        // 绑定监听器
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                TextView show = (TextView) findViewById(R.id.date);
                                show.setText(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth);
                            }
                        }
                        // 设置初始日期
                        , c.get(Calendar.YEAR), c.get(Calendar.MONTH), c
                        .get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        //点击已完成的计划进入扫描条码页面
        lvPlan.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!StringUtil.isNullOrEmpty(planID)) {
                    planID = "";
                }
                vPlan = adapter.getItem(position);
                //获取计划ID
                planID = vPlan.getId();
                //填入选择计划的信息
                tvSpesc.setText(vPlan.getItnbr());
                tvSpescName.setText(vPlan.getItdsc());
                tvMchid.setText(vPlan.getMchid());
                tvPdate.setText(vPlan.getPdate());
                //跳转页面
                llPlan.setVisibility(View.GONE);
                llCode.setVisibility(View.VISIBLE);
                isShow = false;
            }
        });
        //点击取消按钮
        bvOutBarCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!StringUtil.isNullOrEmpty(barCode)) {
                    barCode = "";
                }
                barCode = etBarCode.getText().toString().trim();
                if (barCode.length() == 12 && isNum(barCode) == true) {
                    getBarCode(barCode);
                }else{
                    Toast.makeText(DeleteFormingVreCordActivity.this, "条码不正确，请重新扫描", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

    }

    //查看成型计划
    public void getCurrentVPlan() {
        mchid = actvMchid.getText().toString().trim();
        date = tvDate.getText().toString().trim();

        if (StringUtil.isNullOrEmpty(mchid)) {
            Toast.makeText(DeleteFormingVreCordActivity.this, "机台号不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (StringUtil.isNullOrEmpty(date)) {
            Toast.makeText(DeleteFormingVreCordActivity.this, "查询日期不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            date = sdFormat.format(sdFormat.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String parm = "MCHID=" + mchid + "&CurrDate=" + date;
        new GetCurrDateFormingVPlanTask().execute(parm);
    }

    //删除此条码生产实绩
    public void getBarCode(String barCode) {

        if (list.contains(barCode)) {
            Toast.makeText(DeleteFormingVreCordActivity.this, "此条码已经扫描", Toast.LENGTH_LONG).show();
            etBarCode.setText("");
            return;
        } else {
            String param = "BARCODE=" + barCode + "&PLANID=" + planID;
            new OutCodeTask().execute(param);
//            Toast.makeText(DeleteFormingVreCordActivity.this, "扫描条码："+ barCode + "计划号：" + planID, Toast.LENGTH_LONG).show();
        }
        etBarCode.requestFocus();

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
                        if (barCode.length() == 12 && isNum(barCode) == true) {
                            getBarCode(barCode);
                        }else{
                            Toast.makeText(DeleteFormingVreCordActivity.this, "条码不正确，请重新扫描", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        Toast.makeText(DeleteFormingVreCordActivity.this, "请重新扫描", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(DeleteFormingVreCordActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    List<Map<String, String>> map = (List<Map<String, String>>) res.get("data");
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(DeleteFormingVreCordActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(DeleteFormingVreCordActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DeleteFormingVreCordActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //获取成型计划
    class GetCurrDateFormingVPlanTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.GetCurrDateFormingVPlan, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {

            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(DeleteFormingVreCordActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VPlan> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VPlan>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(DeleteFormingVreCordActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        List<VPlan> finish = new ArrayList<>();
                        finish.clear();
                        //添加已完成的成型计划
                        for (int i = 0; i < datas.size(); i++) {
                            if (datas.get(i).getState().equals("40")) {
                                finish.add(datas.get(i));
                            }
                        }
                        //判断是否有已完成的成型计划
                        if (finish.size() != 0 && finish != null) {
                            adapter = new FormingPlanAdapter(DeleteFormingVreCordActivity.this, finish);
                            lvPlan.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(DeleteFormingVreCordActivity.this, "没有已完成的成型计划", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        datas.clear();
                        adapter = new FormingPlanAdapter(DeleteFormingVreCordActivity.this, datas);
                        lvPlan.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(DeleteFormingVreCordActivity.this, "没有成型计划", Toast.LENGTH_SHORT).show();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DeleteFormingVreCordActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }

            }
        }
    }

    //成型胚胎报废
    class OutCodeTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.OUTFORMINGBARCODE, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(DeleteFormingVreCordActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(DeleteFormingVreCordActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        tvOutBarCodeLog.append(barCode + "\n");
                        etBarCode.setText("");
                        list.add(barCode);
                        tvNum.setText(list.size() + "");
//                        Toast.makeText(DeleteFormingVreCordActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DeleteFormingVreCordActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DeleteFormingVreCordActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
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

    //递归显示
    public String getlog(List<String> list) {
        String logstr = "";
        Collections.reverse(list);//倒序
        for (int i = 0; i < list.size(); i++) {
            logstr += list.get(i) + "\n";
        }
        return logstr;
    }

    //键盘监听
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("key", keyCode + "  ");
        //按键按下
        switch (keyCode) {
            case 0:
                etBarCode.requestFocus();
                etBarCode.setText("");
                break;
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //按键弹开
        switch (keyCode) {
            case 22://右方向键
                if (isShow) {
                    getCurrentVPlan();
                } else {
                    if (!StringUtil.isNullOrEmpty(barCode)) {
                        barCode = "";
                    }
                    barCode = etBarCode.getText().toString().trim();
                    if (barCode.length() == 12 && isNum(barCode) == true) {
                        getBarCode(barCode);
                    }else{
                        Toast.makeText(DeleteFormingVreCordActivity.this, "请重新输入机台号", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case 4://返回键
                if (!isShow) {
                    llCode.setVisibility(View.GONE);
                    //清空数据
                    tvOutBarCodeLog.setText("");
                    etBarCode.setText("");
                    list.clear();
                    tvNum.setText("0");
                    llPlan.setVisibility(View.VISIBLE);
                    //返回上一级
                    isShow = true;
                    getCurrentVPlan();
                } else {
                    tofunction();//返回功能菜单页面
                }
                break;
            default:
                break;
        }
        return true;
    }

}
