package com.dimo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.blankj.utilcode.utils.ScreenUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import id.zelory.compressor.Compressor;

/**
 * Created by Alsor Zhou on 4/29/15.
 */
public class BitmapUtil {
    private static float FIT_LENGTH = 260f;
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

        bm.recycle();
//        try {
//            MediaStore.Images.Media.insertImage(context.getContentResolver(),
//                    file.getAbsolutePath(),
//                    file.getName(),
//                    file.getName());
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    public static Bitmap zoomImg(Bitmap bm, float scale) {
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

    public static Bitmap getFitCropImg(Bitmap bm) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        int least = Math.min(width, height);
        if (least < FIT_LENGTH) {
            float sclae = FIT_LENGTH / least;
            return zoomImg(bm, sclae);
        } else {
            return bm;
        }
    }

}
