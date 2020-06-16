package com.hsproduce.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.adapter.FormingReplAdapter;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能显示页面
 * 权限管理和接收用户名
 * createBy zhangzhr @ 2019-12-21
 */
public class FunctionActivity extends BaseActivity {

    //硫化、装车、检测---控件
    private View view1, view2, view3, view4, view5, view6,
            view7, view8, view9, view10, view11, view12,
            view13, view14, view15, view16, view17, view18, view20, view21;
    private ImageButton vplan, repl, load, loadsc, barrep, barsup, detch, check,
            forming, switchforming, formingchange, formingbarcode, barcodedetail, selectformingplan,
            delectformingcode, ProductNum, deletevulcanization, formingsupplement, newcheck, checkagain;
    private RelativeLayout cx, lh, jc, zc;
    //声明一个long类型变量：用于存放上一点击“返回键”的时刻
    private long mExitTime = 0;
    //修改密码
    private TextView tv_updatePW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_function);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titel_update);
        //修改密码控件ID
        tv_updatePW = (TextView) findViewById(R.id.updatePW);
        //获取控件
        initView();
    }

    public void initView() {
        //功能分类控件
        cx = (RelativeLayout) findViewById(R.id.cx);
        lh = (RelativeLayout) findViewById(R.id.lh);
        jc = (RelativeLayout) findViewById(R.id.jc);
        zc = (RelativeLayout) findViewById(R.id.zc);
        //view功能显示
        view1 = findViewById(R.id.view1);
        view2 = findViewById(R.id.view2);
        view3 = findViewById(R.id.view3);
        view4 = findViewById(R.id.view4);
        view5 = findViewById(R.id.view5);
        view6 = findViewById(R.id.view6);
        view7 = findViewById(R.id.view7);
        view8 = findViewById(R.id.view8);
        view9 = findViewById(R.id.view9);
        view10 = findViewById(R.id.view10);
        view11 = findViewById(R.id.view11);
        view12 = findViewById(R.id.view12);
        view13 = findViewById(R.id.view13);
        view14 = findViewById(R.id.view14);
        view15 = findViewById(R.id.view15);
        view16 = findViewById(R.id.view16);
        view17 = findViewById(R.id.view17);
        view18 = findViewById(R.id.view18);
        view20 = findViewById(R.id.view20);
        view21 = findViewById(R.id.view21);
        //按钮
        vplan = (ImageButton) findViewById(R.id.vplan);//硫化生产
        repl = (ImageButton) findViewById(R.id.repl);//规格交替
        load = (ImageButton) findViewById(R.id.load);//装车出厂
        detch = (ImageButton) findViewById(R.id.detch);//明细更改
        loadsc = (ImageButton) findViewById(R.id.loadsc);//退厂扫描
        barrep = (ImageButton) findViewById(R.id.barrep);//条码更换
        barsup = (ImageButton) findViewById(R.id.barsup);//条码补录
        check = (ImageButton) findViewById(R.id.check);//检测
        forming = (ImageButton) findViewById(R.id.forming);//成型生产
        switchforming = (ImageButton) findViewById(R.id.switchforming);//成型规格切换
        formingchange = (ImageButton) findViewById(R.id.formingchange);//成型明细变更
        formingbarcode = (ImageButton) findViewById(R.id.formingbarcode);//成型胚胎报废
        barcodedetail = (ImageButton) findViewById(R.id.barcodeDetail);//条码追溯
        selectformingplan = (ImageButton) findViewById(R.id.selectformingplan);//查看成型计划
        delectformingcode = (ImageButton) findViewById(R.id.delectformingcode);//成型取消扫描
        ProductNum = (ImageButton) findViewById(R.id.ProductNum);//硫化当班产量
        deletevulcanization = (ImageButton) findViewById(R.id.deletevulcanization);//硫化取消扫描
        formingsupplement = (ImageButton) findViewById(R.id.formingsupplement);//成型条码补录
        newcheck = (ImageButton) findViewById(R.id.newcheck);//新改质检
        checkagain = (ImageButton) findViewById(R.id.checkagain);//热补复检
        //菜单权限管理
        String parm = "UserName=" + App.usercode;
        new TeamTask().execute(parm);

        //修改密码
        tv_updatePW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MaterialDialog dialog = new MaterialDialog.Builder(FunctionActivity.this)
                        .customView(R.layout.dialog_update_pw, true)
                        .show();
                //控件
                View customeView = dialog.getCustomView();
                final EditText ed_OldPW = (EditText) customeView.findViewById(R.id.oldPW);
                final EditText ed_NewPW = (EditText) customeView.findViewById(R.id.newPW);
                Button btn_Cancel = (Button) customeView.findViewById(R.id.btn_cancel);
                Button btn_Ok = (Button) customeView.findViewById(R.id.btn_ok);
                btn_Cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                btn_Ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ed_OldPW.getText().toString() == null || ed_OldPW.getText().toString().equals("")) {
                            Toast.makeText(FunctionActivity.this, "原密码不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (ed_NewPW.getText().toString() == null || ed_NewPW.getText().toString().equals("")) {
                            Toast.makeText(FunctionActivity.this, "新密码不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!ed_OldPW.getText().toString().equals(App.password)) {
                            Toast.makeText(FunctionActivity.this, "原密码错误，请重新输入", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String oldPW = ed_OldPW.getText().toString();
                        String newPW = ed_NewPW.getText().toString();
                        //修改密码
                        String parm = "UserName=" + App.username + "&oldPwd=" + oldPW + "&newPwd=" + newPW;
                        new UpdatePWTask().execute(parm);
                        dialog.dismiss();

                    }
                });

            }
        });

        //Button点击事件
        initEvent();
    }

    private void initEvent() {
        //硫化生产  控件监听事件
        vplan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, VulcanizationActivity.class));
                finish();
            }
        });
        //装车出厂  控件监听事件
        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, LoadFactoryActivity.class));
                finish();
            }
        });
        //条码更换  控件监听事件
        barrep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, BarcodeReplaceActivity.class));
                finish();
            }
        });
        //条码补录  控件监听事件
        barsup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, BarcodeSupplementActivity.class));
                finish();
            }
        });
        //明细变更  控件监听事件
        detch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, DetailChangeActivity.class));
                finish();
            }
        });
        //退厂扫描  控件监听事件
        loadsc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, LoadScanningActivity.class));
                finish();
            }
        });
        //规格交替  控件监听事件
        repl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, SwitchPlanActivity.class));
                finish();
            }
        });
        //检测  控件监听事件
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, CheckActivity.class));
                finish();
            }
        });
        //成型生产
        forming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, FormingActivity.class));
                finish();
            }
        });
        //成型规格切换
        switchforming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, SwitchFormingActivity.class));
                finish();
            }
        });
        //成型明细变更
        formingchange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, FormingDetailChangeActivity.class));
                finish();
            }
        });
        //成型胚胎报废
        formingbarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, FormingBarCodeActivity.class));
                finish();
            }
        });
        //条码追溯
        barcodedetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, BarCodeDetailActivity.class));
                finish();
            }
        });
        //查看成型计划
        selectformingplan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, SelectFormingPlanActivity.class));
                finish();
            }
        });
        //成型取消扫描
        delectformingcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, DeleteFormingVreCordActivity.class));
                finish();
            }
        });
        //硫化当班产量
        ProductNum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, ProductNumActivity.class));
                finish();
            }
        });
        //硫化取消扫描
        deletevulcanization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, DeleteVulcanizationActivity.class));
                finish();
            }
        });
        //成型条码补录
        formingsupplement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, FormingSupplementActivity.class));
                finish();
            }
        });
        //新改质检
        newcheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, NewCheckActivity.class));
                finish();
            }
        });
        //热补复检
        checkagain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FunctionActivity.this, CheckAgainActivity.class));
                finish();
            }
        });

    }

    //菜单权限管理
    class TeamTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.GET_TEAM, strs[0]);
            return result;
        }

        //事后执行
        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FunctionActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
            } else {
                Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<Map<Object, Object>>() {
                }.getType());
                List<Map<String, String>> map = (List<Map<String, String>>) res.get("data");
                if (res == null || res.isEmpty()) {
                    Toast.makeText(FunctionActivity.this, "未获取到数据", Toast.LENGTH_SHORT).show();
                }
                if (res.get("code").equals("200")) {
                    if (map.size() == 1) {
                        if (map.get(0).get("m_CNAME").equals("硫化生产")) {
                            startActivity(new Intent(FunctionActivity.this, VulcanizationActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("硫化规格交替")) {
                            startActivity(new Intent(FunctionActivity.this, SwitchPlanActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("装车出厂")) {
                            startActivity(new Intent(FunctionActivity.this, LoadFactoryActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("条码更换")) {
                            startActivity(new Intent(FunctionActivity.this, BarcodeReplaceActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("条码补录")) {
                            startActivity(new Intent(FunctionActivity.this, BarcodeSupplementActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("硫化明细变更")) {
                            startActivity(new Intent(FunctionActivity.this, DetailChangeActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("退厂扫描")) {
                            startActivity(new Intent(FunctionActivity.this, LoadScanningActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("检测")) {
                            startActivity(new Intent(FunctionActivity.this, CheckActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("成型生产")) {
                            startActivity(new Intent(FunctionActivity.this, FormingActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("成型规格切换")) {
                            startActivity(new Intent(FunctionActivity.this, SwitchFormingActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("成型生产变更")) {
                            startActivity(new Intent(FunctionActivity.this, FormingDetailChangeActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("成型胎胚报废")) {
                            startActivity(new Intent(FunctionActivity.this, FormingBarCodeActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("生产追溯")) {
                            startActivity(new Intent(FunctionActivity.this, BarCodeDetailActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("查看成型计划")) {
                            startActivity(new Intent(FunctionActivity.this, SelectFormingPlanActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("成型取消扫描")) {
                            startActivity(new Intent(FunctionActivity.this, DeleteFormingVreCordActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("当班产量")) {
                            startActivity(new Intent(FunctionActivity.this, ProductNumActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("硫化取消扫描")) {
                            startActivity(new Intent(FunctionActivity.this, DeleteVulcanizationActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("成型条码补录")) {
                            startActivity(new Intent(FunctionActivity.this, FormingSupplementActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("质检")) {
                            startActivity(new Intent(FunctionActivity.this, NewCheckActivity.class));
                            finish();
                        } else if (map.get(0).get("m_CNAME").equals("热补复检")) {
                            startActivity(new Intent(FunctionActivity.this, CheckAgainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(FunctionActivity.this, "您没有操作PDA权限", Toast.LENGTH_SHORT).show();
                        }

                    } else if (map.size() > 1) {
                        for (int i = 0; i < map.size(); i++) {
                            if (map.get(i).get("m_CNAME") == null) {
                                continue;
                            }
                            if (map.get(i).get("m_CNAME").equals("硫化生产")) {
                                lh.setVisibility(View.VISIBLE);
                                view1.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("条码补录")) {
                                lh.setVisibility(View.VISIBLE);
                                view2.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("条码更换")) {
                                lh.setVisibility(View.VISIBLE);
                                view3.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("硫化明细变更")) {
                                lh.setVisibility(View.VISIBLE);
                                view4.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("硫化规格交替")) {
                                lh.setVisibility(View.VISIBLE);
                                view5.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("检测")) {
                                jc.setVisibility(View.VISIBLE);
                                view6.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("装车出厂")) {
                                zc.setVisibility(View.VISIBLE);
                                view7.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("退厂扫描")) {
                                zc.setVisibility(View.VISIBLE);
                                view8.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("成型生产")) {
                                cx.setVisibility(View.VISIBLE);
                                view9.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("成型规格切换")) {
                                cx.setVisibility(View.VISIBLE);
                                view10.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("成型胎胚报废")) {
                                cx.setVisibility(View.VISIBLE);
                                view11.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("成型生产变更")) {
                                cx.setVisibility(View.VISIBLE);
                                view12.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("生产追溯")) {
                                jc.setVisibility(View.VISIBLE);
                                view13.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("查看成型计划")) {
                                cx.setVisibility(View.VISIBLE);
                                view14.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("成型取消扫描")) {
                                cx.setVisibility(View.VISIBLE);
                                view15.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("当班产量")) {
                                lh.setVisibility(View.VISIBLE);
                                view16.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("硫化取消扫描")) {
                                lh.setVisibility(View.VISIBLE);
                                view17.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("成型条码补录")) {
                                cx.setVisibility(View.VISIBLE);
                                view18.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("质检")) {
                                jc.setVisibility(View.VISIBLE);
                                view20.setVisibility(View.VISIBLE);
                            } else if (map.get(i).get("m_CNAME").equals("热补复检")) {
                                jc.setVisibility(View.VISIBLE);
                                view21.setVisibility(View.VISIBLE);
                            } else {
//                                Toast.makeText(FunctionActivity.this, map.get(i).get("m_CNAME")
//                                        + "此功能未在PDA当中", Toast.LENGTH_LONG).show();
                            }
                        }
                        for (int u = 0; u < map.size(); u++) {
                            if (map.get(u).get("moduleid").equals("0")) {
                                App.username = map.get(u).get("m_CNAME");
                                break;
                            }
                        }
                    } else {
                        Toast.makeText(FunctionActivity.this, "您没有操作PDA权限,请退出", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FunctionActivity.this, "菜单查询失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //修改密码
    class UpdatePWTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.UPDATEPW, strs[0]);
            //String result = HttpUtil.sendPost(PathUtil.LOGIN, strs[0], HttpUtil.formContent);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(FunctionActivity.this, "网络连接异常", Toast.LENGTH_LONG).show();
            } else {
                Map<String, String> res = App.gson.fromJson(s, new TypeToken<HashMap<String, String>>() {
                }.getType());
                if (res.get("code").equals("200")) {
                    Toast.makeText(FunctionActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(FunctionActivity.this, res.get("msg").toString(), Toast.LENGTH_LONG).show();
                }

            }
        }
    }

    //键盘监听
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("key", keyCode + "  ");
        //两次返回键时间间隔超过两秒 退出登录
        if (keyCode == 4) {
            if (System.currentTimeMillis() - mExitTime > 2000) {
                Toast.makeText(this, "再按一次退出登录", Toast.LENGTH_SHORT).show();
                //并记录下本次点击“返回键”的时刻，以便下次进行判断
                mExitTime = System.currentTimeMillis();
            } else {
                System.exit(0);//注销功能
            }
        }
        return true;
    }

}
