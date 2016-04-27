package com.yuan.skeleton.utils;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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

	/**
	 * bitmap缓存至本地
	 * @param path
	 * @param bitName
	 * @param mBitmap
	 * @throws IOException
	 */
	public static void saveMyBitmap(String path,String bitName, Bitmap mBitmap) throws IOException {
		File f = new File(path,bitName);
		f.createNewFile();
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
		try {
			fOut.flush();
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
