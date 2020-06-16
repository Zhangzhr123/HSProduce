package com.hsproduce.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.*;
import com.hsproduce.bean.VLoad;
import com.hsproduce.bean.VLoadHxm;
import com.hsproduce.bean.VreCord;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.actionbar.TitleBar;
import com.xuexiang.xui.widget.button.ButtonView;

import java.net.IDN;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;
import static com.xuexiang.xui.widget.picker.widget.WheelTime.dateFormat;
import static java.lang.Thread.sleep;

/**
 * 装车出厂页面
 * 点击装车单列表进入出厂扫描、取消扫描、装车完成页面
 * 点击出厂扫描扫描条码记录在装车单明细中
 * 点击取消扫描从装车单明细中删除
 * 点击装车完成发送装车单给WMS
 * creatBy zhangzhr @ 2020-01-07
 * 1.扫描方式改为广播监听响应
 * 2.倒序输出修改
 */
public class LoadFactoryActivity extends BaseActivity {

    //定义控件
    private HorizontalScrollView hs_load, hs_spesc;
    private View ll_search, ll_load, llfacok, llcode, llscanok, llcodelog, lloutcode, lloutcodelog, lloutok;
    private TextView outbarcodelog, barcode, anum, search, itnbr, itndsc, outbarcode, outanum;
    private TextView barcodelog;
    private ButtonView ok, outok;
    private Button out, loadfacok, inscan, outscan;
    private ImageButton getsearch;
    private TableRow table;
    private TableLayout lltable;
    private View view6;
    //标题控件
    private TitleBar load, inload, outload;
    //显示列表
    private ListView lvload, lvloadfac, lvloadspesc;
    private List<VLoad> lists = new ArrayList<>();
    //展示信息
    private List<VLoad> list = new ArrayList<>();
    //适配器
    private LoadAdapter loadAdapter;
    private LoadFacAdapter loadFacAdapter;
    private LoadSpescAdapter loadSpescAdapter;
    //定义变量  主表ID  条码  装车单ID 规格
    private String Id = "", code = "", loadid = "", codeitnbr = "", outCode = "";
    private List<String> loaditnbr = new ArrayList<>();
    private int number = 0;
    private int outnumber = 0;
    //显示条码记录
    private List<String> log = new ArrayList<>();
    private List<String> outlog = new ArrayList<>();
    //添加条码判断是否重复
    private List<String> codeList = new ArrayList<>();
    private List<String> outCodeList = new ArrayList<>();
    private Boolean isNew = true;
    private Boolean outIsNew = true;
    //页面跳转初始值
    private Integer sizePager = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_loadfactory);
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
        //标题栏
        load = (TitleBar) findViewById(R.id.load);
        inload = (TitleBar) findViewById(R.id.inload);
        outload = (TitleBar) findViewById(R.id.outload);
        //HorizontalScrollView  ListView列表
        hs_load = (HorizontalScrollView) findViewById(R.id.hs_load);
        hs_spesc = (HorizontalScrollView) findViewById(R.id.hs_spesc);
        view6 = findViewById(R.id.view6);
        //layout
        ll_search = findViewById(R.id.ll_search);
        ll_load = findViewById(R.id.ll_load_fac);
        llfacok = findViewById(R.id.ll_fac_ok);
        llscanok = findViewById(R.id.ll_scan_ok);
        llcode = findViewById(R.id.ll_code);
        llcodelog = findViewById(R.id.ll_codelog);
        table = (TableRow) findViewById(R.id.table);
        lltable = (TableLayout) findViewById(R.id.ll_table);
        //取消扫描
        lloutcode = findViewById(R.id.ll_out_code);
        lloutcodelog = findViewById(R.id.ll_out_codelog);
        lloutok = findViewById(R.id.ll_scan_out_ok);
        outok = (ButtonView) findViewById(R.id.out_ok);
        //条码扫描
        outbarcode = (TextView) findViewById(R.id.out_barcode);
        outbarcode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        //条码记录
        outbarcodelog = (TextView) findViewById(R.id.out_barcode_log);
        //计数
        outanum = (TextView) findViewById(R.id.out_anum);
        //搜索框
        search = (TextView) findViewById(R.id.search);
        //输入框
        barcode = (TextView) findViewById(R.id.barcode);
        barcode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        //显示录入条码个数
        anum = (TextView) findViewById(R.id.anum);
        //规格
        itnbr = (TextView) findViewById(R.id.itnbr);
        itndsc = (TextView) findViewById(R.id.itndsc);
        //条码记录
        barcodelog = (TextView) findViewById(R.id.barcode_log);
        //点击按钮
        getsearch = (ImageButton) findViewById(R.id.get_search);
        inscan = (Button) findViewById(R.id.in_scan);
        outscan = (Button) findViewById(R.id.out_scan);
        out = (Button) findViewById(R.id.out);
        loadfacok = (Button) findViewById(R.id.loadfac_ok);
        ok = (ButtonView) findViewById(R.id.ok);
        //ListView
        lvload = (ListView) findViewById(R.id.listview);
        lvloadfac = (ListView) findViewById(R.id.lv_load_fac);
        lvloadspesc = (ListView) findViewById(R.id.lv_load_spesc);
        //展示装车单
        String parm = "SUB_CODE=" + "&CAR_CODE=";
        new SelVLoadTask().execute(parm);
    }

    public void initEvent() {
        //搜索按钮
        getsearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSearch();
            }
        });
        //点击列表
        lvload.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //查询装车单明细
                Id = loadAdapter.getItem(position).getId();
                //获取装车单号
                loadid = loadAdapter.getItem(position).getLoadno();
                //查询当前单据的装车单明细
                loadVS();
            }
        });
        //进入出厂扫描
        inscan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInScan();
            }
        });
        //进入取消扫描
        outscan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOutScan();
            }
        });
        //取消扫描返回
        outok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOutOk();
            }
        });
        //返回初始页面
        out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOut();
            }
        });
        //出厂扫描返回
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOk();
            }
        });
        //装车完成发送wms
        loadfacok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setWMS();
            }
        });
    }

    //发送装车单到WMS
    public void setWMS() {
        String parm = "LOADNO=" + loadid + "&USER_NAME=" + App.username;
        new ToWMSTask().execute(parm);
    }

    //搜索按钮
    public void getSearch() {
        String searchstr = search.getText().toString().trim();
        searchstr = searchstr.toUpperCase();
        //展示装车单
        String parm = "SUB_CODE=" + "&CAR_CODE=" + searchstr;
        new SelVLoadTask().execute(parm);
        search.setText("");
    }

    //进入出厂扫描
    public void getInScan() {
        //隐藏
        load.setVisibility(View.GONE);//标题
        llfacok.setVisibility(View.GONE);
        hs_spesc.setVisibility(View.GONE);
        table.setVisibility(View.GONE);
        ll_load.setVisibility(View.GONE);
        lvloadfac.setVisibility(View.GONE);
        //显示
        inload.setVisibility(View.VISIBLE);//标题
        llcode.setVisibility(View.VISIBLE);
        lltable.setVisibility(View.VISIBLE);
        view6.setVisibility(View.VISIBLE);
        llcodelog.setVisibility(View.VISIBLE);
        llscanok.setVisibility(View.VISIBLE);
        //焦点扫描框
        barcode.setText("");
        //barcodelog.setText("");
        barcode.requestFocus();
        //第二页
        sizePager = 2;
    }

    //进入取消扫描
    public void getOutScan() {
        //隐藏
        load.setVisibility(View.GONE);//标题
        llfacok.setVisibility(View.GONE);
        hs_spesc.setVisibility(View.GONE);
        table.setVisibility(View.GONE);
        ll_load.setVisibility(View.GONE);
        lvloadfac.setVisibility(View.GONE);
        //显示
        outload.setVisibility(View.VISIBLE);//标题
        lloutcode.setVisibility(View.VISIBLE);
        lloutcodelog.setVisibility(View.VISIBLE);
        lloutok.setVisibility(View.VISIBLE);
        //锁定扫描框
        outbarcode.setText("");
        //outbarcodelog.setText("");
        outbarcode.requestFocus();
        //第三页
        sizePager = 3;
    }

    //返回初始页面
    public void getOut() {
        //隐藏
        llfacok.setVisibility(View.GONE);
        hs_spesc.setVisibility(View.GONE);
        table.setVisibility(View.GONE);
        ll_load.setVisibility(View.GONE);
        lvloadfac.setVisibility(View.GONE);
        //显示
        ll_search.setVisibility(View.VISIBLE);
        hs_load.setVisibility(View.VISIBLE);
        //刷新装车单列表
        String parm = "SUB_CODE=" + "&CAR_CODE=";
        new SelVLoadTask().execute(parm);
        //初始页
        sizePager = 0;
        //数据清空
        //出厂扫描
        itnbr.setText("");
        itndsc.setText("");
        number = 0;
        anum.setText("0");
        barcodelog.setText("");
        log.clear();
        codeList.clear();
        //取消扫描
        outnumber = 0;
        outanum.setText("0");
        outbarcodelog.setText("");
        outlog.clear();
        outCodeList.clear();
    }

    //出厂扫描返回
    public void getOk() {
        //隐藏
        inload.setVisibility(View.GONE);//标题
        llscanok.setVisibility(View.GONE);
        llcodelog.setVisibility(View.GONE);
        view6.setVisibility(View.GONE);
        lltable.setVisibility(View.GONE);
        llcode.setVisibility(View.GONE);
        //显示
        load.setVisibility(View.VISIBLE);//标题
        lvloadfac.setVisibility(View.VISIBLE);
        ll_load.setVisibility(View.VISIBLE);
        table.setVisibility(View.VISIBLE);
        hs_spesc.setVisibility(View.VISIBLE);
        llfacok.setVisibility(View.VISIBLE);
        //刷新规格列表
        //清空数据
        //itnbr.setText("");
        //itndsc.setText("");
        //anum.setText("0");
        //number = 0;
        //log.clear();
        codeList.clear();
        new SelVLoadListMXTask().execute("ID=" + Id);
        //第1页
        sizePager = 1;
    }

    //取消扫描返回
    public void getOutOk() {
        //隐藏
        lloutok.setVisibility(View.GONE);
        lloutcodelog.setVisibility(View.GONE);
        lloutcode.setVisibility(View.GONE);
        outload.setVisibility(View.GONE);//标题
        //显示
        load.setVisibility(View.VISIBLE);//标题
        lvloadfac.setVisibility(View.VISIBLE);
        ll_load.setVisibility(View.VISIBLE);
        table.setVisibility(View.VISIBLE);
        hs_spesc.setVisibility(View.VISIBLE);
        llfacok.setVisibility(View.VISIBLE);
        //刷新规格列表
        //outanum.setText("0");
        //outlog.clear();
        //outnumber = 0;
        outCodeList.clear();
        new SelVLoadListMXTask().execute("ID=" + Id);
        //第1页
        sizePager = 1;
    }

    //查询当前单据的装车单明细
    public void loadVS() {
        list.clear();
        for (int i = 0; i < lists.size(); i++) {
            if (lists.get(i).getId().equals(Id)) {
                list.add(lists.get(i));
            }
        }
        //展示当前装车单
        loadFacAdapter = new LoadFacAdapter(LoadFactoryActivity.this, list);
        lvloadfac.setAdapter(loadFacAdapter);
        loadFacAdapter.notifyDataSetChanged();
        //获取装车单明细
        new SelVLoadListMXTask().execute("ID=" + Id);
    }

    //出厂扫描
    public void loadcode(String barCode) {
        //获取轮胎条码
        code = barCode;
        if (codeList.contains(code)) {
            isNew = false;
        }

        if (isNew) {
            //查询轮胎条码规格
            String parm1 = "TYRE_CODE=" + code;
            new SelCodeTask().execute(parm1);
        } else {
            isNew = true;
            Toast toast = Toast.makeText(LoadFactoryActivity.this, "此条码已经扫描", Toast.LENGTH_LONG);
            showMyToast(toast, 500);
            return;
        }

    }

    //取消扫描
    public void outcode(String barCode) {
        outCode = barCode;
        if (outCodeList.contains(outCode)) {
            outIsNew = false;
        }

        if (outIsNew) {
            String parm = "TYRE_CODE=" + outCode + "&VLOAD_ID=" + Id + "&USER_NAME=" + App.username;
            new DelVLoadTask().execute(parm);
        } else {
            outIsNew = true;
            Toast toast = Toast.makeText(LoadFactoryActivity.this, "此条码已经扫描", Toast.LENGTH_LONG);
            showMyToast(toast, 500);
            return;
        }

    }

    //广播监听
    private BroadcastReceiver scanDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SCN_CUST_ACTION_SCODE)) {
                try {
                    String barCode = "";
                    barCode = intent.getStringExtra(SCN_CUST_EX_SCODE);
                    //判断条码是否为空
                    if (!StringUtil.isNullOrEmpty(barCode)) {
                        if (barCode.length() == 12 && isNumeric(barCode) == true) {
                            if (sizePager == 2) {//出厂扫描
                                loadcode(barCode);
                            } else if (sizePager == 3) {//取消扫描
                                outcode(barCode);
                            } else {
                                return;
                            }

                        } else {
                            Toast toast = Toast.makeText(LoadFactoryActivity.this, "条码不正确，请重新扫描", Toast.LENGTH_LONG);
                            showMyToast(toast, 500);
                            return;
                        }
                    } else {
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ScannerService", e.toString());
                }
            }
        }
    };

    //展示装车单信息
    class SelVLoadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.SelVLOADList, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {

            if (StringUtil.isNullOrBlank(s)) {
                Toast toast = Toast.makeText(LoadFactoryActivity.this, "网络连接异常", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    lists = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VLoad>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, "未获取到数据", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //展示装车单
                        loadAdapter = new LoadAdapter(LoadFactoryActivity.this, lists);
                        lvload.setAdapter(loadAdapter);
                        loadAdapter.notifyDataSetChanged();
                    } else {
                        lists.clear();
                        loadAdapter = new LoadAdapter(LoadFactoryActivity.this, lists);
                        lvload.setAdapter(loadAdapter);
                        loadAdapter.notifyDataSetChanged();
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(LoadFactoryActivity.this, "数据处理异常", Toast.LENGTH_LONG);
                    showMyToast(toast, 500);
                    return;
                }
            }
        }
    }

    //展示装车单规格信息
    class SelVLoadListMXTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.SelVLOADListMX, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {

            if (StringUtil.isNullOrBlank(s)) {
                Toast toast = Toast.makeText(LoadFactoryActivity.this, "网络连接异常", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VLoadHxm> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VLoadHxm>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, "未获取数据", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //获取装车单轮胎规格  获取装车单ID
                        for (int i = 0; i < datas.size(); i++) {
                            loaditnbr.add(datas.get(i).getItnbr());
                        }
                        //展示装车单明细
                        loadSpescAdapter = new LoadSpescAdapter(LoadFactoryActivity.this, datas);
                        lvloadspesc.setAdapter(loadSpescAdapter);
                        loadSpescAdapter.notifyDataSetChanged();
                        //隐藏
                        ll_search.setVisibility(View.GONE);
                        hs_load.setVisibility(View.GONE);
                        //显示
                        lvloadfac.setVisibility(View.VISIBLE);
                        ll_load.setVisibility(View.VISIBLE);
                        table.setVisibility(View.VISIBLE);
                        hs_spesc.setVisibility(View.VISIBLE);
                        llfacok.setVisibility(View.VISIBLE);
                        //第一页
                        sizePager = 1;
                    } else {
                        datas.clear();
                        list.clear();
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(LoadFactoryActivity.this, "数据处理异常", Toast.LENGTH_LONG);
                    showMyToast(toast, 500);
                    return;
                }
            }
        }
    }

    //根据条码查询轮胎规格
    class SelCodeTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.SelTYRE_CODE, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            //清空数据
            itnbr.setText("");
            itndsc.setText("");
            if (StringUtil.isNullOrBlank(s)) {
                Toast toast = Toast.makeText(LoadFactoryActivity.this, "网络连接异常", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VreCord> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VreCord>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, "未获取数据", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //获取条码规格
                        codeitnbr = datas.get(0).getItnbr();
                        //判断条码规格是否在装车单中
                        if (loaditnbr.contains(codeitnbr)) {
                            //展示装车单明细
                            itnbr.setText(datas.get(0).getItnbr());
                            itndsc.setText(datas.get(0).getItdsc());
                            //出厂扫描
                            String parm2 = "TYRE_CODE=" + code + "&VLOAD_ID=" + Id + "&USER_NAME=" + App.username;
                            new InsVLoadTask().execute(parm2);
                        } else {
                            //清空数据
                            itnbr.setText("");
                            itndsc.setText("");
                            Toast toast = Toast.makeText(LoadFactoryActivity.this, "装车单中无此规格，请重新扫描！", Toast.LENGTH_LONG);
                            showMyToast(toast, 500);
                            return;
                        }
                    } else {
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(LoadFactoryActivity.this, "数据处理异常", Toast.LENGTH_LONG);
                    showMyToast(toast, 500);
                    return;
                }
            }
        }
    }

    //出厂扫描
    class InsVLoadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.InsVLOAD, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast toast = Toast.makeText(LoadFactoryActivity.this, "网络连接异常", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, "未获取数据", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        codeList.add(code);
                        //显示绑定条码数量
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        log.add("[" + date + "]" + code);
                        barcodelog.setText("");
                        for (int i = 0; i < log.size(); i++) {
                            if (i == 0) {
                                barcodelog.setText(log.get(i));
                            } else {
                                barcodelog.setText(getlog(log));
                            }
                        }
                        anum.setText("");
                        number++;
                        anum.setText(number + "");
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                    } else {
                        //清空数据
                        itnbr.setText("");
                        itndsc.setText("");
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(LoadFactoryActivity.this, "数据处理异常", Toast.LENGTH_LONG);
                    showMyToast(toast, 500);
                    return;
                }
            }
        }
    }

    //取消扫描
    class DelVLoadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.DelVLOAD, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast toast = Toast.makeText(LoadFactoryActivity.this, "网络连接异常", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, "未获取数据", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        outCodeList.add(outCode);
                        //显示绑定条码数量
                        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        outlog.add("[" + date + "]" + outCode);
                        outbarcodelog.setText("");
                        for (int i = 0; i < outlog.size(); i++) {
                            if (i == 0) {
                                outbarcodelog.setText(outlog.get(i));
                            } else {
                                outbarcodelog.setText(getoutlog(outlog));
                            }
                        }
                        outanum.setText("");
                        outnumber++;
                        outanum.setText(outnumber + "");
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                    } else {
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(LoadFactoryActivity.this, "数据处理异常", Toast.LENGTH_LONG);
                    showMyToast(toast, 500);
                    return;
                }
            }
        }
    }

    //发送WMS
    class ToWMSTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.SendWMS, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast toast = Toast.makeText(LoadFactoryActivity.this, "网络连接异常", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, "未获取数据", Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                    } else {
                        Toast toast = Toast.makeText(LoadFactoryActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG);
                        showMyToast(toast, 500);
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast toast = Toast.makeText(LoadFactoryActivity.this, "数据处理异常", Toast.LENGTH_LONG);
                    showMyToast(toast, 500);
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

    //递归显示倒序输出
    public String getlog(List<String> list) {
        String logstr = "";
        for (int i = list.size() - 1; i >= 0; i--) {
            logstr += list.get(i) + "\n";
        }
        return logstr;
    }

    public String getoutlog(List<String> list) {
        String logstr = "";
        for (int i = list.size() - 1; i >= 0; i--) {
            logstr += list.get(i) + "\n";
        }
        return logstr;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onPause() {
        unregisterReceiver(scanDataReceiver);
        super.onPause();
    }

    //键盘监听
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("key", keyCode + "  ");
        if (keyCode == 0) {
            if (sizePager == 2) {
                barcode.setText("");
            }
            if (sizePager == 3) {
                outbarcode.setText("");
            }
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //弹开时
        if (keyCode == 4) {
            if (sizePager == 0) {
                clear();
                tofunction();
            } else if (sizePager == 1) {
                getOut();
            } else if (sizePager == 2) {
                getOk();
            } else if (sizePager == 3) {
                getOutOk();
            }

        }
        //右方向键
        if (keyCode == 22) {
            if (!StringUtil.isNullOrEmpty(barcode.getText().toString().trim())) {
                if (sizePager == 2) {
                    String barCode = barcode.getText().toString().trim();
                    loadcode(barCode);
                }
            }
            if (!StringUtil.isNullOrEmpty(outbarcode.getText().toString().trim())) {
                if (sizePager == 3) {
                    String barCode = outbarcode.getText().toString().trim();
                    outcode(barCode);
                }
            }
            if (sizePager == 0) {
                getSearch();
            }
        }
        //增加快捷键
        switch (keyCode) {
            case 131://F1键
                if (sizePager == 1) {
                    getInScan();//出场扫描
                }
                break;
            case 132://F2键
                if (sizePager == 1) {
                    getOutScan();//取消扫描
                }
                break;
            case 133://F3键
                if (sizePager == 1) {
                    getOut();//返回
                }
                break;
            case 134://F4键
                if (sizePager == 1) {
                    setWMS();//装车完成
                }
                break;
            default:
                break;
        }
        return true;
    }

    //清空数据
    public void clear() {
        anum.setText("0");
        outanum.setText("0");
        number = 0;
        outnumber = 0;
        log.clear();
        outlog.clear();
        list.clear();
        lists.clear();
        loaditnbr.clear();
        codeList.clear();
        outCodeList.clear();
        Id = "";
        code = "";
        loadid = "";
        codeitnbr = "";
        outCode = "";
        sizePager = 0;
    }

}
