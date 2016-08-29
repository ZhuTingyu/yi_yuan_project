package com.dimo.utils;

/**
 * Created by Alsor Zhou on 8/29/16.
 */

public class DebugUtil {
    public static <T> T assertNotNull(T object) {
        if (object == null)
            throw new AssertionError("Object cannot be null");
        return object;
    }
}
