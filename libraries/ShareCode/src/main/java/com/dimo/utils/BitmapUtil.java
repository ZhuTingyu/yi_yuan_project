package com.dimo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Alsor Zhou on 4/29/15.
 */
public class BitmapUtil {
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
}
