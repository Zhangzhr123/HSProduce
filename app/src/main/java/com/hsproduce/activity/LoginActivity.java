package com.hsproduce.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.google.gson.reflect.TypeToken;
import com.hsproduce.App;
import com.hsproduce.R;
import com.hsproduce.bean.Team;
import com.hsproduce.bean.UpdateVersion;
import com.hsproduce.util.HttpUtil;
import com.hsproduce.util.PathUtil;
import com.hsproduce.util.StringUtil;
import com.xuexiang.xui.widget.button.ButtonView;
import java.io.*;
import java.util.*;

import static android.support.constraint.Constraints.TAG;
/**
 * 登录页面
 * 版本自动更新、设置动态IP地址，接收用户工号和密码
 * createBy zhangzhr @ 2019-12-21
 * 1.程序更新修改为强制更新，版本号在APP文件中
 */
public class LoginActivity extends BaseActivity {

    //用户名  密码
    private TextView tv_code, tv_password,VersionName;
    //班组下拉列表
    private Spinner sp_shift;
    //登录按钮
    private ButtonView login;
    //声明一个long类型变量：用于存放上一点击“返回键”的时刻
    private long mExitTime = 0;
    //点击图片更换ip地址
    private ImageView ip;
    //下拉列表及适配器
    private List<String> shiftlist = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    //存放班组数据
    private List<Team> teamList = new ArrayList<>();
    //IP
    public static String IP = "";
    private String 当前版本 = "", 最新版本 = "";
    private String teamName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_login);

        //判断设备型号并赋值
        String device_model = Build.MODEL; // 设备型号
        App.pdaType = device_model;
        System.out.println("设备型号=="+device_model);

        new 版本更新Task().execute();

        new ShiftTask().execute();

        initView();
    }

    public void initView() {
        //用户名
        tv_code = (TextView) findViewById(R.id.code);
        //密码
        tv_password = (TextView) findViewById(R.id.password);
        //班组
        sp_shift = findViewById(R.id.shift);
        //ip地址
        ip = (ImageView) findViewById(R.id.ip);
        //登录
        login = (ButtonView)findViewById(R.id.login);
        //版本信息
        VersionName = (TextView)findViewById(R.id.VersionName);
        VersionName.setText("华盛条码系统 v"+App.version);

        //自行输入IP
        ip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText et = new EditText(LoginActivity.this);
                et.setHint(App.ip);
                new AlertDialog.Builder(LoginActivity.this).setTitle("请输入IP地址")
                        .setView(et)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //按下确定键后的事件
                                IP = et.getText().toString();
                                if (IP.equals("") || IP == null) {
                                    IP = App.ip;
                                }
                                App.ip = IP;
                                //数据持久化
                                File file = getDir("myIP", Context.MODE_PRIVATE);
                                if (file != null) {
                                    deleteFile(file);
                                    save("myIP", IP);
                                } else {
                                    save("myIP", IP);
                                }
                                Toast.makeText(getApplicationContext(), et.getText().toString(), Toast.LENGTH_SHORT).show();
                            }
                        }).setNegativeButton("取消", null).show();
            }
        });

        //下拉列表
        //创建一个数组适配器
        //new ShiftTask().execute();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, shiftlist);
        sp_shift.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!StringUtil.isNullOrEmpty(teamName)){
                    teamName = "";
                }
                teamName = parent.getItemAtPosition(position).toString();
                String teamId = "";
                //根据ID获取内容
                for(int i = 0;i<teamList.size();i++){
                    if (teamName.equals(teamList.get(i).getName())) {
                        teamId = teamList.get(i).getId();
                        break;
                    }

                }
                App.shift = teamId;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //点击登录
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

    }

    public void login() {
        if (!最新版本.equals(当前版本)) {
            //文件下载
            download(PathUtil.文件下载);
        } else {
            if(teamName.equals("请选择") || teamName.equals("") || teamName == null){
                Toast toast = Toast.makeText(LoginActivity.this, "请选择班组", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            }
            String username = tv_code.getText().toString().trim();
            String password = tv_password.getText().toString().trim();
            if(StringUtil.isNullOrEmpty(username)){
                Toast toast = Toast.makeText(LoginActivity.this, "用户名不能为空", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            }
            if(StringUtil.isNullOrEmpty(password)){
                Toast toast = Toast.makeText(LoginActivity.this, "密码不能为空", Toast.LENGTH_LONG);
                showMyToast(toast, 500);
                return;
            }
            App.usercode = username;
            App.password = password;
            String param = "UserName=" + username + "&PWD=" + password;
            new MyTask().execute(param);
        }

    }

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
                Toast.makeText(LoginActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
            } else {
                Map<String, Object> res = App.gson.fromJson(s, new TypeToken<Map<String, Object>>() {
                }.getType());
                List<Map<String, String>> map = (List<Map<String, String>>) res.get("data");
                List<Team> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<Team>>(){}.getType());
                if (res.get("code").equals("200")) {
                    //班组数据清空
                    teamList.clear();
                    teamList.addAll(datas);
                    //班组名称数据清空
                    shiftlist.clear();
                    shiftlist.add("请选择");
                    for (int i = 0; i < map.size(); i++) {
                        shiftlist.add(map.get(i).get("name"));
                    }
                    sp_shift.setAdapter(adapter);
                } else {
                    Toast.makeText(LoginActivity.this, res.get("msg").toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //登录
    class MyTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strs) {
            String result = HttpUtil.sendGet(PathUtil.LOGIN, strs[0]);
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            if (StringUtil.isNullOrBlank(s)) {
                Toast.makeText(LoginActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
            } else {
                Map<String, String> res = App.gson.fromJson(s, new TypeToken<HashMap<String, String>>() {
                }.getType());
                if(res == null || res.isEmpty()){
                    Toast.makeText(LoginActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (res.get("code").equals("200")) {
                    Intent intent = new Intent(LoginActivity.this, FunctionActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(LoginActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    /**
     * 保存到文件
     *
     * @param value
     */
    private void save(String packName, String value) {
        BufferedWriter writer = null;
        try {

            FileOutputStream out = openFileOutput(packName, Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(value);

        } catch (FileNotFoundException e) {
//            Log.e(TAG, "save: 找不到文件", e);
        } catch (IOException e) {
            Log.e(TAG, "save:IO 异常", e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "save: 无法关闭 BufferedWriter", e);
            }
        }
    }

    /**
     * 从文件中获取数据
     *
     * @return
     */
    private String get(String packName) {

        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            FileInputStream in = openFileInput(packName);
            reader = new BufferedReader(new InputStreamReader(in));
            String temp;
            while ((temp = reader.readLine()) != null) {
                content.append(temp);
            }
        } catch (FileNotFoundException e) {
//            Log.e(TAG, "get: FileNotFoundException", e);
        } catch (IOException e) {
            Log.e(TAG, "get: IOException", e);
        }
        return content.toString();
    }

    public static void deleteFile(File file) {
        if (file.exists()) {                          //判断文件是否存在
            if (file.isFile()) {                      //判断是否是文件
                boolean isSucess = file.delete();
                Log.i("TAG:", "文件删除状态--->" + isSucess);
            } else if (file.isDirectory()) {           //判断是否是文件夹
                File files[] = file.listFiles();    //声明目录下所有文件
                for (int i = 0; i < files.length; i++) {   //遍历目录下所有文件
                    deleteFile(files[i]);           //把每个文件迭代删除
                }
                boolean isSucess = file.delete();
                Log.i("TAG:", "文件夹删除状态--->" + isSucess);
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
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                //并记录下本次点击“返回键”的时刻，以便下次进行判断
                mExitTime = System.currentTimeMillis();
            } else {
                System.exit(0);//注销操作
            }
        }
        if (keyCode == 22){
            login();
        }
            return true;
    }

    //版本更新
    class 版本更新Task extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... voids) {
            // 获取服务器最新版本
            String result = HttpUtil.sendGet(PathUtil.获取最新版本, "");
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                if (StringUtil.isNullOrBlank(s)) {
                    Toast.makeText(LoginActivity.this, "网络连接异常", Toast.LENGTH_SHORT).show();
                } else {
                    Map<Object, Object> res = App.gson.fromJson(s, new TypeToken<HashMap<Object, Object>>() {
                    }.getType());
                    List<UpdateVersion> datas = App.gson.fromJson(App.gson.toJson(res.get("data")), new TypeToken<List<UpdateVersion>>() {
                    }.getType());
                    if (res.get("code").equals("200")) {
                        if (null != datas) {
                            最新版本 = datas.get(0).getItemname();
                            当前版本 = App.version;
                            // 版本判断
                            if (!当前版本.equals(最新版本)) {
                                //文件下载
                                download(PathUtil.文件下载);
                            } else {
                                Toast toast = Toast.makeText(LoginActivity.this, "已经是最新版本", Toast.LENGTH_LONG);
                                showMyToast(toast, 500);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {

            }
        }
    }

    //根据地址下载APK并自动安装
    public void download(String url) {
        final DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        // 设置下载路径和文件名
        request.setDestinationInExternalPublicDir("download", "update.apk");
        request.setDescription("软件新版本下载");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setMimeType("application/vnd.android.package-archive");
        // 设置为可被媒体扫描器找到
        request.allowScanningByMediaScanner();
        // 设置为可见和可管理
        request.setVisibleInDownloadsUi(true);
        // 获取此次下载的ID
        final long refernece = dManager.enqueue(request);
        // 注册广播接收器，当下载完成时自动安装
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        BroadcastReceiver receiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                long myDwonloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (refernece == myDwonloadID) {
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    Uri downloadFileUri = dManager.getUriForDownloadedFile(refernece);
                    install.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
                    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(install);
                }
            }
        };
        registerReceiver(receiver, filter);
    }

}
