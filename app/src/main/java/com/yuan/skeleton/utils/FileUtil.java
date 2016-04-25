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
	
	public static String getAudioFile(){
		File file=new File(path+"/house/audios/");
		if(!file.exists()){
			file.mkdirs();
		}
		return path+"/house/audios/";
	}
	
	public static String getWaterPhotoPath(){
		File file=new File(path+"/house/photo/");
		if(!file.exists()){
			file.mkdirs();
		}
		return path+"/house/photo/";
	}
}
