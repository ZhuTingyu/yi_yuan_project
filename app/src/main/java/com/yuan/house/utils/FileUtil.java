package com.yuan.house.utils;

import android.os.Environment;

import java.io.File;

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
}
