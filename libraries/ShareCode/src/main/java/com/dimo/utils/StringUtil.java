package com.dimo.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alsor Zhou on 13-11-9.
 */
public class StringUtil {
    /**
     * Generate random string with specific length
     * <p>
     * http://stackoverflow.com/a/12116194
     *
     * @param len random string length
     * @return random string
     */
    public static String randomString(int len) {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(len);
        char tempChar;
        for (int i = 0; i < randomLength; i++) {
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    /**
     * Generate random alphabet string
     *
     * @param len random string length
     * @return random string
     */
    public static String randomAlphabetString(int len) {
        char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < len; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }

        String output = sb.toString();
        return output;
    }

    public static String UUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate 8 character string as filename
     *
     * @return filename
     */
    public static String randomFilename() {
        return randomString(8);
    }

    /**
     * http://stackoverflow.com/a/3414749
     *
     * @param context
     * @param contentUri
     * @return
     */
    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static String getRealPathFromString(String url) {
        return url.substring(7, url.length());
    }

    public static boolean stringMatchedWithRegex(String s, String pattern) {
        try {
            Pattern patt = Pattern.compile(pattern);
            Matcher matcher = patt.matcher(s);
            return matcher.matches();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static HashMap<String, Object> JSONString2HashMap(String jsonString) throws JSONException {
        HashMap<String, Object> map;

        JSONObject object = new JSONObject(jsonString);
        map = JSON2HashMap(object);

        return map;
    }

    public static HashMap<String, Object> JSON2HashMap(JSONObject object) throws JSONException {
        HashMap<String, Object> map = new HashMap<>();

        Iterator<?> keys = object.keys();

        while (keys.hasNext()) {
            String key = (String) keys.next();

            map.put(key, object.getString(key));
        }
        return map;
    }

    public static RequestParams JSONString2RequestParams(String jsonString) throws JSONException {
        RequestParams requestParams;

        if (jsonString != null) {
            JSONObject object = new JSONObject(jsonString);
            requestParams = JSON2RequestParams(object);
        } else
            requestParams = new RequestParams();

        return requestParams;
    }

    public static RequestParams JSON2RequestParams(JSONObject json) throws JSONException {
        RequestParams requestParams = new RequestParams();
        Iterator<?> keys = json.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            requestParams.put(key, json.get(key));
        }
        return requestParams;
    }

    public static Boolean isValidHTTPUrl(String url) {
        if (url.contains("http:") || url.contains("https:"))
            return true;
        else
            return false;
    }

    public static String prependZero(String str, int bits) {
        return String.format("%0"+bits+"d", str);
    }

    public static String prependZero(int i, int bits) {
        return String.format("%0"+bits+"d", i);
    }

    public static String quote(String str) {
        return "'" + str + "'";
    }

    public static String formatString(Context cxt, int id, Object... args) {
        return String.format(cxt.getString(id), args);
    }
}
