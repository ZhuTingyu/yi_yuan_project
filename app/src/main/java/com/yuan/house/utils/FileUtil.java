package com.yuan.house.utils;

import android.os.Environment;
import android.util.Log;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

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
                    public void inProgress(float progress, long total, int id) {

                    }

                    @Override
                    public void onError(Call call, Exception e, int id) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(File response, int id) {
                        boolean b = response.exists();
                        Log.d("Proposal", response.getName());
                    }
                });
    }

    /**
     * 文件拷贝
     * @param oldPath
     * @param newPath
     */
    public static void copyFile(String oldPath, String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);

            if (oldfile.exists()) {
                InputStream inStream = new FileInputStream(oldPath);
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                //int length;
                while ( (byteread = inStream.read(buffer)) != -1) {
                    //bytesum += byteread;
                    //System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
            }
        }  catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();
        }
    }
}
