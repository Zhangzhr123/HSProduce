package com.hsproduce.activity;
import android.annotation.SuppressLint;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.DialogItemAdapter;
import com.hsproduce.bean.Team;
import com.hsproduce.bean.VreCord;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;

/**
 * 硫化明细变更页面
 * 扫描条码获取此条码的生产明细，修改明细内容，发送至后台
 * createBy zhangzhr @ 2019-12-21
 * 1.时间控件以及时间转换格式需要注意，另外月份需要加一显示正常月份
 * 2.规格名称中文和特殊字符需要转换
 * 3.班组字段不传递给后台
 * 4.扫描改为广播监听方式
 */
public class DetailChangeActivity extends BaseActivity {

    //定义控件
    private TextView tvBarcode, tvItnbr, tvItdsc, tvMaster;
    private AutoCompleteTextView autoTvMchid;
    private ButtonView btFindCode, btSubmit, btFindItdsc;
    private Spinner spLR, spShift, spTeam;
    //下拉列表
    private List<String> teamlist = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    //存放班组数据
    private List<Team> teamList = new ArrayList<>();
    //声明一个long类型变量：用于存放上一点击“返回键”的时刻
    private long mExitTime = 0;
    //定义变量
    private String barCode = "", spesc = "", createUser = "", lorR = "", shift = "", mchId = "", spescName = "", codeId = "", team = "", itnbr = "", itdsc = "d";
    //Dialog显示列表
    private List<String> itdscList = new ArrayList<>();
    private DialogItemAdapter itnbrAdapter;
    private List<String> mchidList = new ArrayList<>();
    private DialogItemAdapter mchidAdapter;
    private Map<String, VreCord> itdscMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_detailchange);
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
        //补录条码
        tvBarcode = (TextView) findViewById(R.id.barcode);
        //获得焦点
        tvBarcode.requestFocus();
        tvBarcode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        //规格编码
        tvItnbr = (TextView) findViewById(R.id.spesc);
        //规格描述
        tvItdsc = (TextView) findViewById(R.id.spescname);
        //主手
        tvMaster = (TextView) findViewById(R.id.master);
        //机台号
        autoTvMchid = (AutoCompleteTextView) findViewById(R.id.mchid);
        //左右模
        spLR = (Spinner) findViewById(R.id.lorR);
        //班组
        spTeam = (Spinner) findViewById(R.id.team);
        new ShiftTask().execute();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, teamlist);
        spTeam.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                team = parent.getItemAtPosition(position).toString();
                for(int i = 0;i<teamList.size();i++){
                    if (team.equals(teamList.get(i).getName())) {
                        team = teamList.get(i).getId();
                        break;
                    } else if (team.equals(teamList.get(i).getName())) {
                        team = teamList.get(i).getId();
                        break;
                    } else if (team.equals(teamList.get(i).getName())) {
                        team = teamList.get(i).getId();
                        break;
                    } else if (team.equals(teamList.get(i).getName())) {
                        team = teamList.get(i).getId();
                        break;
                    } else {
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        //班次
        spShift = (Spinner) findViewById(R.id.shift);
        //查询条码明细
        btFindCode = (ButtonView) findViewById(R.id.bt_getCode);
        //条码补录
        btSubmit = (ButtonView) findViewById(R.id.ok);
        //筛选按钮
        btFindItdsc = (ButtonView) findViewById(R.id.getitnbr);
    }

    public void initEvent() {
        btFindCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCodeDetail();
            }
        });
        btSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //机台
                mchId = autoTvMchid.getText().toString().trim();
                //规格编码
                spesc = tvItnbr.getText().toString().trim();
                //规格名称
                spescName = tvItdsc.getText().toString().trim();
                spescName = StringUtil.toUtf8String(spescName);
                //主手
                createUser = tvMaster.getText().toString().trim();
                //补录条码
//                barCode = tvBarcode.getText().toString().trim();
                //班组
                team = spTeam.getSelectedItem().toString().trim();
                team = StringUtil.isNullOrBlank(team) ? "" : (team.equals("甲班") ? "1" : (team.equals("乙班") ? "2" : (team.equals("丙班") ? "3" : team.equals("丁班") ? "4" : team)));
                //左右模
                lorR = spLR.getSelectedItem().toString().trim();
                String parm = "MCHID=" + mchId + "&ITNBR=" + spesc + "&ITDSC=" + spescName + "&LoR=" + lorR
                        + "&TEAM=" + team + "&USER_NAME=" + createUser + "&DateTime_W=" + "&SwitchID=" + codeId;
                new ChangeDetailedTask().execute(parm);
            }
        });
        //筛选规格
        btFindItdsc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //清空数据
                itdscList.clear();
                itdscMap.clear();
                String search = tvItdsc.getText().toString().trim();
                if(!StringUtil.isNullOrEmpty(search) && search.length() > 12){
                    search = search.substring(0, 12);
                }
                search = search.toUpperCase();//大写转换
                String parm = "ITDSC=" + search;
                new GetSpecTask().execute(parm);
            }
        });
        //筛选机台
        String parm = "TYPE_ID=10098";
        new MCHIDTask().execute(parm);
    }

    public void getCodeDetail() {
        //api/PDA/SelDetailed?SwitchTYRE_CODE=111600000447
        barCode = tvBarcode.getText().toString().trim();
        String parm = "SwitchTYRE_CODE=" + barCode;
        new SelDetailedTask().execute(parm);
        //tvBarcode.setText("");
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
                        tvBarcode.setText(barCode);
                        getCodeDetail();
                    } else {
                        Toast.makeText(DetailChangeActivity.this, "请重新扫描", Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ScannerService", e.toString());
                }
            }
        }
    };

    //班组
    class ShiftTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.GET_SHIFT, null);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(DetailChangeActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                }.getType());
                List<Map<String, String>> map = (List<Map<String, String>>) res.get("data");
                List<com.hsproduce.bean.Team> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<com.hsproduce.bean.Team>>(){}.getType());
                if (res.get("code").equals("200")) {
                    //班组数据清空
                    teamList.clear();
                    teamList.addAll(datas);
                    //班组名称数据清空
                    teamlist.clear();
                    for (int i = 0; i < map.size(); i++) {
                        teamlist.add(map.get(i).get("name"));
                    }
                    spTeam.setAdapter(adapter);
                }
            }
        }
    }

    //根据规条码模糊查询明细
    class SelDetailedTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.SelDetailed, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(DetailChangeActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VreCord> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VreCord>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(DetailChangeActivity.this, "未获取到数据", Toast.LENGTH_LONG).show();
                    }
                    if (res.get("code").equals("200") && datas != null && datas.size() > 0) {
                        VreCord vrecord = datas.get(0);
                        //填入规格信息
                        tvItnbr.setText(vrecord.getItnbr());
                        tvItdsc.setText(vrecord.getItdsc());
                        autoTvMchid.setText(vrecord.getMchid());
                        //设置默认值
                        if(!StringUtil.isNullOrEmpty(vrecord.getLr())){
                            if (vrecord.getLr().equals("L")) {
                                spLR.setSelection(0, true);
                            } else {
                                spLR.setSelection(1, true);
                            }
                        }
                        if(!StringUtil.isNullOrEmpty(vrecord.getTeam())){
                            if (vrecord.getTeam().equals("甲班")) {
                                spTeam.setSelection(0, true);
                            } else if (vrecord.getTeam().equals("乙班")) {
                                spTeam.setSelection(1, true);
                            } else if (vrecord.getTeam().equals("丙班")) {
                                spTeam.setSelection(2, true);
                            } else {
                                spTeam.setSelection(3, true);
                            }
                        }
                        if(!StringUtil.isNullOrEmpty(vrecord.getShift())){
                            if (vrecord.getShift().equals("1")) {
                                spShift.setSelection(0, true);
                            } else if (vrecord.getShift().equals("2")) {
                                spShift.setSelection(1, true);
                            } else {
                                spShift.setSelection(2, true);
                            }
                        }
                        tvMaster.setText(vrecord.getCreateuser());
                        //获取信息
                        spescName = vrecord.getItdsc().replaceAll(" ", "%20");
                        codeId = vrecord.getId();
//                        Toast.makeText(DetailChangeActivity.this, "轮胎查询成功！", Toast.LENGTH_LONG).show();
                    } else if (res.get("code").equals("500")) {
                        Toast.makeText(DetailChangeActivity.this, "查询成功，没有匹配的轮胎信息！", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(DetailChangeActivity.this, "轮胎查询错误", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DetailChangeActivity.this, "数据处理异常", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //根据TYPEID 获取数据字典内容  机台
    class MCHIDTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.GetDictionaries, strs[0]);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(DetailChangeActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    List<Map<String, String>> map = (List<Map<String, String>>) res.get("data");
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(DetailChangeActivity.this, "未获取到数据", Toast.LENGTH_LONG).show();
                    }
                    if (res.get("code").equals("200")) {
                        String search = autoTvMchid.getText().toString().trim();
                        for(int i=0;i<map.size();i++){
                            if(search.contains(map.get(i).get("itemid"))){}
                            mchidList.add(map.get(i).get("itemid"));
                        }
                        //创建 AutoCompleteTextView 适配器 (输入提示)
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                DetailChangeActivity.this, android.R.layout.simple_dropdown_item_1line,mchidList);
                        //初始化autoCompleteTextView
                        autoTvMchid.setAdapter(adapter);
                        //设置输入多少字符后提示，默认值为2，在此设为１
                        autoTvMchid.setThreshold(1);
//                        Toast.makeText(DetailChangeActivity.this, "机台查询成功！", Toast.LENGTH_LONG).show();
                    } else if (res.get("code").equals("500")) {
                        Toast.makeText(DetailChangeActivity.this, "查询成功，没有匹配的机台！", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(DetailChangeActivity.this, "错误：" + res.get("ex"), Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DetailChangeActivity.this, "数据处理异常", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //根据规格编码模糊查询规格
    class GetSpecTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.GetSPECIFICATION, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(DetailChangeActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VreCord> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VreCord>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(DetailChangeActivity.this, "未获取到数据", Toast.LENGTH_LONG).show();
                    }
                    if (res.get("code").equals("200")) {
                        //填入规格信息
                        for (int i = 0; i < datas.size(); i++) {
                            itdscList.add(datas.get(i).getItdsc());
                            itdscMap.put(datas.get(i).getItdsc(), datas.get(i));
                        }
                        itnbrAdapter = new DialogItemAdapter(DetailChangeActivity.this, itdscList);
                        //弹窗显示选中消失
                        AlertDialog alertDialog = new AlertDialog
                                .Builder(DetailChangeActivity.this)
                                .setSingleChoiceItems(itnbrAdapter, 0, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        itdsc = itdscList.get(which);
                                        tvItdsc.setText(itdsc);
                                        itnbr = itdscMap.get(itdsc).getItnbr();
                                        tvItnbr.setText(itnbr);
                                        dialog.dismiss();
//                                        Toast.makeText(DetailChangeActivity.this, "选择了" + itnbr, Toast.LENGTH_SHORT).show();
                                    }
                                }).create();
                        alertDialog.show();
                    } else if (res.get("code").equals("500")) {
                        Toast.makeText(DetailChangeActivity.this, "查询成功，没有匹配的规格！", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(DetailChangeActivity.this, "查询错误", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DetailChangeActivity.this, "数据处理异常", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //明细修改
    class ChangeDetailedTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.ChangeDetailed, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(DetailChangeActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(DetailChangeActivity.this, "未获取到数据", Toast.LENGTH_LONG).show();
                    }
                    if (res.get("code").equals("200")) {
                        tvBarcode.setText("");
                        tvItnbr.setText("");
                        tvItdsc.setText("");
                        autoTvMchid.setText("");
                        spLR.setSelection(1, true);
                        spTeam.setSelection(1, true);
                        spShift.setSelection(1, true);
                        tvMaster.setText("");
                        Toast.makeText(DetailChangeActivity.this, "变更成功！", Toast.LENGTH_LONG).show();
                    } else if (res.get("code").equals("100")) {
                        Toast.makeText(DetailChangeActivity.this, "未找到轮胎信息，变更失败！", Toast.LENGTH_LONG).show();
                    } else if (res.get("code").equals("300")) {
                        Toast.makeText(DetailChangeActivity.this, "变更失败！", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(DetailChangeActivity.this, "错误：" + res.get("ex"), Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DetailChangeActivity.this, "数据处理异常", Toast.LENGTH_LONG).show();
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
        switch (keyCode){
            case 0://扫描键
                tvBarcode.requestFocus();
                tvBarcode.setText("");//成功后清空输入框
                break;
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
//            case 0:
//                if(!StringUtil.isNullOrEmpty(tvBarcode.getText().toString().trim())){
//                    getCodeDetail();
//                }
//                break;
            case 22:
                if(!StringUtil.isNullOrEmpty(tvBarcode.getText().toString().trim())){
                    getCodeDetail();
                }
                break;
            case 4:
                startActivity(new Intent(DetailChangeActivity.this, FunctionActivity.class));
                this.finish();
                break;

        }
        return true;
    }


}
