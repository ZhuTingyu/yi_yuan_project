package com.yuan.cp.uitl;

import android.os.Environment;

import java.io.File;

/**
 * Created by LiFengYi on 16/3/2.
 */
public class FileUtil {

    private static String TAG = FileUtil.class.getSimpleName();

    private static String path = "";

    static {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            path = Environment.getExternalStorageDirectory() + "/yuan";
        } else {
            path = Environment.getDataDirectory().getAbsolutePath() + "/yuan";
        }
    }

    /**
     * 拍照路径
     * @return
     */
    public static String getPhotoPath(){
        File file = new File(path + "/photo/");
        if(!file.exists()){
            file.mkdirs();
        }
        return path + "/photo/";
    }

    public static String getClipPath(){
        File file = new File(path + "/clip/");
        if(!file.exists()){
            file.mkdirs();
        }
        return path + "/clip/";
    }


}
