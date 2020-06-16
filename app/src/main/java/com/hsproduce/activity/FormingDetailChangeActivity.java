package com.hsproduce.activity;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_ACTION_SCODE;
import static com.hsproduce.broadcast.SystemBroadCast.SCN_CUST_EX_SCODE;

/**
 * 成型明细变更页面
 * 扫描条码查询成型生产明细，修改明细发送到后台
 * createBy zhangzhr @ 2019-12-21
 * 1.注意规格名称中文和特殊字符需要转换
 * 2.扫描改为广播监听响应方式
 */
public class FormingDetailChangeActivity extends BaseActivity {

    //定义控件
    private TextView tvBarCode, tvSpesc, tvMchId;
    private ButtonView btGetItnbr, btGetMchId;
    private Button btOk, btOut;
    private ImageButton btGetCode;
    private Spinner spShift;
    //定义变量
    private String barCode = "", spesc = "", createUser = "", shift = "", mchId = "", spescName = "", codeId = "", team = "", itnbr = "", itndsc = "", date = "";
    //Dialog显示列表
    private List<String> itnbrList = new ArrayList<>();
    private DialogItemAdapter itnbrAdapter;
    private List<String> mchidList = new ArrayList<>();
    private DialogItemAdapter mchidAdapter;
    private List<VreCord> itndscList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_forming_detailchange);
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
        tvBarCode = (TextView) findViewById(R.id.barcode);
        //获得焦点
        tvBarCode.requestFocus();
        tvBarCode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(12)});
        //规格编码
        tvSpesc = (TextView) findViewById(R.id.spesc);
        //机台号
        tvMchId = (TextView) findViewById(R.id.mchid);
        //返回
        btOut = (Button) findViewById(R.id.out);
        //班次
        spShift = (Spinner) findViewById(R.id.shift);
        //查询条码明细
        btGetCode = (ImageButton) findViewById(R.id.searchdetail);
        //条码补录
        btOk = (Button) findViewById(R.id.ok);
        //筛选按钮
        btGetItnbr = (ButtonView) findViewById(R.id.getitnbr);
        btGetMchId = (ButtonView) findViewById(R.id.getmchid);
    }

    public void initEvent() {
        //搜索按钮
        btGetCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCodeDetail();
            }
        });
        //变更按钮
        btOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                change();
            }
        });
        //筛选规格
        btGetItnbr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //清空数据
                itnbrList.clear();
                itndscList.clear();
                String search = tvSpesc.getText().toString().trim();
                search = search.toUpperCase();//大写转换
                String parm = "ITNBR=" + search;
                new GetSpecTask().execute(parm);
            }
        });
        //筛选成型机台
        btGetMchId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //清空数据
                mchidList.clear();
                String parm = "TYPE_ID=10107";
                new MCHIDTask().execute(parm);
            }
        });
        //返回功能菜单
        btOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tofunction();
            }
        });
    }

    public void change() {
        //机台号
        mchId = tvMchId.getText().toString().trim();
        //规格编码
        spesc = tvSpesc.getText().toString().trim();
        //规格名称
        spescName = toUtf8String(spescName).replace("/", "%2F").replaceAll(" ", "%20");
        //补录条码
        barCode = tvBarCode.getText().toString().trim();
        //班次
        shift = spShift.getSelectedItem().toString().trim();
        if (shift.equals("早班")) {
            shift = "1";
        } else if (shift.equals("中班")) {
            shift = "2";
        } else if (shift.equals("晚班")) {
            shift = "3";
        } else {
            shift = "";
        }
        String parm = "MCHID=" + mchId + "&ITNBR=" + spesc + "&ITDSC=" + spescName
                + "&SHIFT=" + shift + "&USER_NAME=" + App.username + "&DateTime_W=" + date + "&SwitchID=" + codeId;
        new ChangeDetailedTask().execute(parm);
    }

    //获取条码明细
    public void getCodeDetail() {
        barCode = tvBarCode.getText().toString().trim();
        if (StringUtil.isNullOrEmpty(barCode)) {
//            Toast.makeText(FormingBarCodeActivity.this, "请扫描轮胎条码", Toast.LENGTH_LONG).show();
            return;
        } else {
            String parm = "SwitchTYRE_CODE=" + barCode;
            new SelDetailedTask().execute(parm);
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
                    //判断条码是否为空 是否为12位 是否纯数字组成
                    if (!StringUtil.isNullOrEmpty(barCode) && barCode.length() == 12 && isNum(barCode) == true) {
                        tvBarCode.setText(barCode);
                        getCodeDetail();
                    } else {
                        Toast.makeText(FormingDetailChangeActivity.this, "请重新扫描", Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ScannerService", e.toString());
                }
            }
        }
    };

    //根据条码查询明细
    class SelDetailedTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.FORMINGSECLECTCODE, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            //清空
            tvSpesc.setText("");
            tvMchId.setText("");
            spShift.setSelection(0, true);

            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingDetailChangeActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VreCord> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VreCord>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingDetailChangeActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //填入规格信息
                        tvSpesc.setText(datas.get(0).getItnbr());
                        tvMchId.setText(datas.get(0).getMchid());
                        if (datas.get(0).getShift().equals("早")) {
                            spShift.setSelection(0, true);
                        } else if (datas.get(0).getShift().equals("中")) {
                            spShift.setSelection(1, true);
                        } else {
                            spShift.setSelection(2, true);
                        }
                        //获取信息
                        mchId = datas.get(0).getMchid();
                        spesc = datas.get(0).getItnbr();
                        spescName = datas.get(0).getItdsc();
                        team = datas.get(0).getTeam();
                        createUser = datas.get(0).getCreateuser();
                        codeId = datas.get(0).getId();
                        //时间转换
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                        date = formatter.format(formatter.parse(datas.get(0).getWdate()));
//                        Toast.makeText(FormingDetailChangeActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(FormingDetailChangeActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingDetailChangeActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(FormingDetailChangeActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    List<Map<String, String>> map = (List<Map<String, String>>) res.get("data");
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingDetailChangeActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        String search = tvMchId.getText().toString().trim();
                        for (int i = 0; i < map.size(); i++) {
                            if(map.get(i).get("itemid") == null){
                                continue;
                            }
                            if (search.contains(map.get(i).get("itemid"))) {
                            }
                            mchidList.add(map.get(i).get("itemid"));
                        }
                        mchidAdapter = new DialogItemAdapter(FormingDetailChangeActivity.this, mchidList);
                        //弹窗显示选中消失
                        AlertDialog alertDialog = new AlertDialog
                                .Builder(FormingDetailChangeActivity.this)
                                .setSingleChoiceItems(mchidAdapter, 0, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mchId = mchidList.get(which);
                                        tvMchId.setText("");
                                        tvMchId.setText(mchId);
                                        dialog.dismiss();
                                        Toast.makeText(FormingDetailChangeActivity.this, "选择了" + mchId, Toast.LENGTH_SHORT).show();
                                    }
                                }).create();
                        alertDialog.show();
                    } else {
                        Toast.makeText(FormingDetailChangeActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingDetailChangeActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //根据规格编码模糊查询规格
    class GetSpecTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.FORMINGSECLECTITNBR, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingDetailChangeActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    List<VreCord> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<VreCord>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingDetailChangeActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        //填入规格信息
                        for (int i = 0; i < datas.size(); i++) {
                            if(datas.get(i).getItnbr() == null){
                                continue;
                            }
                            itnbrList.add(datas.get(i).getItnbr());
                            itndscList.add(datas.get(i));
                        }
                        itnbrAdapter = new DialogItemAdapter(FormingDetailChangeActivity.this, itnbrList);
                        //弹窗显示选中消失
                        AlertDialog alertDialog = new AlertDialog
                                .Builder(FormingDetailChangeActivity.this)
                                .setSingleChoiceItems(itnbrAdapter, 0, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        itnbr = itnbrList.get(which);
                                        tvSpesc.setText("");
                                        tvSpesc.setText(itnbr);
                                        //规格名称
                                        for (int j = 0; j < itndscList.size(); j++) {
                                            if (itndscList.get(j).getItnbr().equals(spesc)) {
                                                spescName = itndscList.get(j).getItdsc();//toUtf8String(Itndsc.get(j).getItdsc()).replace("/", "%2F").replaceAll(" ", "%20");
                                            }
                                        }
                                        dialog.dismiss();
                                        Toast.makeText(FormingDetailChangeActivity.this, "选择了" + itnbr, Toast.LENGTH_SHORT).show();
                                    }
                                }).create();
                        alertDialog.show();
                    } else {
                        Toast.makeText(FormingDetailChangeActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingDetailChangeActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //明细修改
    class ChangeDetailedTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = HttpUtil.sendGet(PathUtil.FORMINGCHANGE, strings[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FormingDetailChangeActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                return;
            } else {
                try {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                    }.getType());
                    if (res == null || res.isEmpty()) {
                        Toast.makeText(FormingDetailChangeActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (res.get("code").equals("200")) {
                        Toast.makeText(FormingDetailChangeActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        Toast.makeText(FormingDetailChangeActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(FormingDetailChangeActivity.this, "数据处理异常", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    //转换为%E4%BD%A0形式  中文转url编码
    public static String toUtf8String(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0 && c <= 255) {
                sb.append(c);
            } else {
                byte[] b;
                try {
                    b = String.valueOf(c).getBytes("utf-8");
                } catch (Exception ex) {
                    System.out.println(ex);
                    b = new byte[0];
                }
                for (int j = 0; j < b.length; j++) {
                    int k = b[j];
                    if (k < 0)
                        k += 256;
                    sb.append("%" + Integer.toHexString(k).toUpperCase());
                }
            }
        }
        return sb.toString();
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
        switch (keyCode) {
            case 0:
                tvBarCode.requestFocus();
                tvBarCode.setText("");
                break;
        }

        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //弹开时
        switch (keyCode) {
//            case 0://扫描键
//                getCodeDetail();//查询明细
//                break;
            case 22://右方向键
                getCodeDetail();
                break;
            case 4:
                tofunction();
                break;
            case 131://F1键
                change();//变更
                break;
            case 132://F2键
                tofunction();
                break;
            default:
                break;
        }

        return true;
    }


}
