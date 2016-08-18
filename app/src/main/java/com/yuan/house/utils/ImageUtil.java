package com.yuan.house.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.dimo.utils.StringUtil;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Created by LiFengYi on 16/1/29.
 */
public class ImageUtil {
    /**
     * 处理图片
     *
     * @param bm        所要转换的bitmap
     * @param newWidth  新的宽
     * @param newHeight 新的高
     * @return 指定宽高的bitmap
     */
    public static Bitmap scale(Bitmap bm, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }

    /**
     * @param bitmap
     * @return 压缩后的bitmap
     */
    public static Bitmap compress(Bitmap bitmap, boolean isSysUp) {
        Bitmap destBitmap;
        /* 图片宽度调整为100，大于这个比例的，按一定比例缩放到宽度为100 */
        if (bitmap.getWidth() > 80) {
            float scaleValue = 80f / bitmap.getWidth();
            System.out.println("缩放比例---->" + scaleValue);

            Matrix matrix = new Matrix();
            /* 针对系统拍照，旋转90° */
            if (isSysUp)
                matrix.setRotate(90);
            matrix.postScale(scaleValue, scaleValue);

            destBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            int widthTemp = destBitmap.getWidth();
            int heightTemp = destBitmap.getHeight();
            Log.i("压缩后的宽高", "width: " + heightTemp
                    + " height:" + widthTemp);
        } else {
            return bitmap;
        }
        return destBitmap;

    }

    public static Bitmap compress(Bitmap image) {
        byte[] raw = compressToByteArray(image);

        ByteArrayInputStream isBm = new ByteArrayInputStream(raw);//把压缩后的数据baos存放到ByteArrayInputStream中

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;// 以上options的两个属性必须联合使用才会有效果
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, opt);//把ByteArrayInputStream数据生成图片

        return bitmap;
    }

    public static byte[] compressToByteArray(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中

        int options = 100;
        while (baos.toByteArray().length / 1024 > 100 && options > 0) {    //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }

        return baos.toByteArray();
    }

    public static byte[] compressToByteArray(String imageFile) {
        Bitmap image = BitmapFactory.decodeFile(imageFile);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中

        int options = 100;
        while (baos.toByteArray().length / 1024 > 100 && options > 0) {    //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }

        return baos.toByteArray();
    }

    /**
     * load from url
     */
    public static void loadImageThumbnail(ImageView iv, String imageUrl, int dimension) {
        if (TextUtils.isEmpty(imageUrl)) {
            iv.setImageDrawable(null);
            return;
        }

        if (imageUrl.contains("http")) {
            if (dimension > 0) {
                Picasso.with(iv.getContext())
                        .load(imageUrl)
                        .resize(dimension, dimension)
                        .centerCrop()
                        .into(iv);
            } else {
                Picasso.with(iv.getContext())
                        .load(imageUrl)
                        .into(iv);
            }
        } else {
            loadImageThumbnailFromFile(iv, imageUrl, dimension);
        }
    }

    /**
     * load from local file
     */
    public static void loadImageThumbnailFromFile(ImageView iv, String path, int dimension) {
        File imageFile = new File(path);
        if (imageFile.exists()) {
            if (dimension > 0) {
                Picasso.with(iv.getContext())
                        .load(imageFile)
                        .resize(dimension, dimension)
                        .centerCrop()
                        .into(iv);
            } else {
                Picasso.with(iv.getContext())
                        .load(imageFile)
                        .into(iv);
            }
        } else {
            iv.setImageDrawable(null);
        }
    }

    /**
     * load from local
     * resourceId : don't be xml type drawable
     */
    public static void loadImageThumbnail(ImageView iv, int resourceId, int dimension) {
        if (dimension > 0) {
            Picasso.with(iv.getContext())
                    .load(resourceId)
                    .resize(dimension, dimension)
                    .centerCrop()
                    .into(iv);
        } else {
            Picasso.with(iv.getContext())
                    .load(resourceId)
                    .into(iv);
        }
    }

    public static void loadFullScreenImage(final ImageView iv, String imageUrl, int width, final LinearLayout bgLinearLayout) {
        if (!TextUtils.isEmpty(imageUrl)) {
            if (StringUtil.isValidHTTPUrl(imageUrl)) {
                Picasso.with(iv.getContext())
                        .load(imageUrl)
                        .into(iv);
            } else {
                Picasso.with(iv.getContext())
                        .load("file://" + imageUrl)
                        .into(iv);
            }
        } else {
            iv.setImageDrawable(null);
        }
    }
}
