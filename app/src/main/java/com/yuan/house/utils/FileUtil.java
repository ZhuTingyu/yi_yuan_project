package com.yuan.house.utils;

import android.os.Environment;
import android.util.Log;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;

import java.io.File;

import okhttp3.Call;

public class FileUtil {
    private static String path = "";

    static {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            path = Environment.getExternalStorageDirectory() + "/IEYUAN";
        } else {
            path = Environment.getDataDirectory().getAbsolutePath() + "/IEYUAN";
        }
    }

    // http://stackoverflow.com/a/8955087/2619161
    public static String getFileExt(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
    }

    public static String getAudioFile() {
        File file = new File(path + "/house/audios/");
        if (!file.exists()) {
            file.mkdirs();
        }
        return path + "/house/audios/";
    }

    public static String getWaterPhotoPath() {
        File file = new File(path + "/house/photo/");
        if (!file.exists()) {
            file.mkdirs();
        }
        return path + "/house/photo/";
    }

    public static boolean isFileExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static void downloadFile(String url, String path, String name) {

        OkHttpUtils.get().url(url)
                .build()
                .execute(new FileCallBack(path, name) {
                    @Override
                    public void inProgress(float progress, long total) {

                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(File response) {
                        boolean b = response.exists();
                        Log.d("Proposal", response.getName());
                    }
                });
    }
}
