package com.yuan.skeleton.utils;

import android.os.Environment;

import java.io.File;

public class FileUtil {
	private static String path="";
	static{
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			path=Environment.getExternalStorageDirectory()+"/IEYUAN";
		}else{
			path=Environment.getDataDirectory().getAbsolutePath()+"/IEYUAN";
		}
	}
	
	public static String getCacheFile(){
		File file=new File(path+"/CACHE/Files/");
		if(!file.exists()){
			file.mkdirs();
		}
		return path+"/CACHE/Files/";
	}
	
	public static String getWaterPhotoPath(){
		File file=new File(path+"/Photo/");
		if(!file.exists()){
			file.mkdirs();
		}
		return path+"/Photo/";
	}
}
