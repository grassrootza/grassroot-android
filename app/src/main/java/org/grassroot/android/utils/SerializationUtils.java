package org.grassroot.android.utils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by paballo on 2016/11/02.
 */

public class SerializationUtils {


    public static byte[] serialize(Object obj) {
        byte[] result = null;
        ByteArrayOutputStream fos = null;

        try {
            fos = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(fos);
            o.writeObject(obj);
            result = fos.toByteArray();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }


    public static Object deserialize(byte[] arr) {
        InputStream fis = null;

        try {
            fis = new ByteArrayInputStream(arr);
            ObjectInputStream o = new ObjectInputStream(fis);
            return o.readObject();
        } catch (IOException e) {
            System.err.println(e);
        } catch (ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }

        return null;
    }


    @SuppressWarnings("unchecked")
    public static <T> T cloneObject(T obj) {
        return (T) deserialize(serialize(obj));
    }
}