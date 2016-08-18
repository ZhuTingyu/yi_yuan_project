package com.dimo.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.provider.MediaStore;
import android.util.DisplayMetrics;

import com.blankj.utilcode.utils.ScreenUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import id.zelory.compressor.Compressor;

import static android.R.attr.factor;
import static android.R.attr.width;

/**
 * Created by Alsor Zhou on 4/29/15.
 */
public class BitmapUtil {
    private static float FIT_LENGTH = 270f;
    private static int kImageCompressQuality = 60;

    public static void compressImage(Context context, String filename) {
        File file = new File(filename);

        int width = ScreenUtils.getScreenWidth(context);
        int height = ScreenUtils.getScreenHeight(context);

        Bitmap bm = new Compressor.Builder(context)
                .setMaxWidth(width)
                .setMaxHeight(height)
                .setQuality(kImageCompressQuality)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .build()
                .compressToBitmap(file);

        saveBitmap(context, bm, filename);
    }

    public static void saveBitmap(Context context, Bitmap bm, String path) {
        OutputStream fOut = null;
        File file = new File(path); // the File to save to

        try {
            fOut = new FileOutputStream(file);

            bm.compress(Bitmap.CompressFormat.JPEG, 40, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
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

    public static Bitmap zoomImg(Bitmap bm, float scale) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        // 得到新的图片
        Bitmap newBm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newBm;
    }

    public static Bitmap getFitCropImg(Bitmap bm, Activity activity, int imageType) {

        DisplayMetrics metric = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metric);

        float windowWidth = metric.widthPixels;     // 屏幕宽度（像素）
        float windowHeight = metric.widthPixels;
        // 获得图片的宽高
        int bmWidth = bm.getWidth();
        int bmHeight = bm.getHeight();
        //剪切头像
        if (imageType == 13) {
            int least = Math.min(bmWidth, bmHeight);
            if (least < FIT_LENGTH) {
                float scale = FIT_LENGTH / least;
                return zoomImg(bm, scale);
            } else {
                return bm;
            }
        } else {
            float scale;
            if (bmWidth >= bmHeight) {
                scale = windowWidth / bmWidth;
            } else {
                scale = windowHeight / bmHeight;
            }
            if(scale <= 1){
                return bm;
            }
            return zoomImg(bm, scale);
        }
    }
}

