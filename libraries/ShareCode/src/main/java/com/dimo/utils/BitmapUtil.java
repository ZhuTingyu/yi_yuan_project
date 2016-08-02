package com.dimo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Alsor Zhou on 4/29/15.
 */
public class BitmapUtil {

    private static float FIT_LENGTH = 260f;

    public static void saveBitmap(Context context, Bitmap bm, String path) {
        OutputStream fOut = null;
        File file = new File(path); // the File to save to

        try {
            fOut = new FileOutputStream(file);

            bm.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            fOut.flush();
            fOut.close(); // do not forget to close the stream
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(),
                    file.getName(),
                    file.getName());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap zoomImg(Bitmap bm, float scale){
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }

    public static Bitmap getFitCropImg(Bitmap bm){
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        int least = Math.min(width,height);
        if(least < FIT_LENGTH){
            float sclae = FIT_LENGTH / least;
            return zoomImg(bm,sclae);
        }else
            return bm;
    }

}
