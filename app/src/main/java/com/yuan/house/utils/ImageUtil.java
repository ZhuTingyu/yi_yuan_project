package com.yuan.house.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by LiFengYi on 16/1/29.
 */
public class ImageUtil {

    private static final ImageUtil instance = new ImageUtil();

    private ImageUtil(){}

    public static ImageUtil getInstance(){
        return instance;
    }

    /**
     * @param bitmap
     * @return 压缩后的bitmap
     */
    public Bitmap compressionBigBitmap(Bitmap bitmap, boolean isSysUp) {
        Bitmap destBitmap = null;
		/* 图片宽度调整为100，大于这个比例的，按一定比例缩放到宽度为100 */
        if (bitmap.getWidth() > 80) {
            float scaleValue = (float) (80f / bitmap.getWidth());
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

    /**
     *  处理图片
     * @param bm 所要转换的bitmap
     * @param newWidth 新的宽
     * @param newHeight 新的高
     * @return 指定宽高的bitmap
     */
    public static Bitmap zoomImg(Bitmap bm, int newWidth ,int newHeight){
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

    public Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while ( baos.toByteArray().length / 1024>100 && options > 0) {	//循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig =  Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;// 以上options的两个属性必须联合使用才会有效果
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, opt);//把ByteArrayInputStream数据生成图片

        return bitmap;
    }
}
