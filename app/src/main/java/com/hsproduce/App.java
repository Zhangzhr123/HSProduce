package com.hsproduce;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.xuexiang.xui.XUI;

import java.io.*;

import static android.support.constraint.Constraints.TAG;

public class App extends Application {

    public static Gson gson = new Gson();
    public static String username = "";//用户名
    public static String usercode = "";//工号
    public static String password = "";//密码
    public static String shift = "";//班组
    public static String lr = "";//左右膜
    public static String access_token = "";
    public static String Iu = "";
    public static String ip = "10.2.129.132:8001";//10.2.129.132:8001
    public static String version = "1.3.9";//版本号 要与系统发布版本号对比一致不用更新不一致更新
    public static String pdaType = "";//设备类型

    @Override
    public void onCreate() {
        XUI.init(this); //初始化UI框架
        XUI.debug(true);  //开启UI框架调试日志
        // 获取设置的IP地址
        String text = get("myIP");
        if (!TextUtils.isEmpty(text)) {
            App.ip = text;
        }
        super.onCreate();
    }

    //从系统文件中的获取版本号
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

}
