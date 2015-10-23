package com.onewaveinc.mrc;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class License {

    public static boolean ENABLED = true;

    private static final Map<String,byte[]> data = new HashMap<String,byte[]>();

    public static boolean enabled() {
        return ENABLED;
    }

    static void setEnable(boolean enable) {
        ENABLED = enable;
    }

    public static byte[] get(String key) {
        return data.get(key);
    }

    public static Set<String> getKeys() {
        return data.keySet();
    }

    /**
     * 将 byte 数组反序列化成对象
     * @param buf byte 数组
     * @return 对象
     */
    public static Object deserialize(byte[] buf){
        try {
			return new ObjectInputStream(new ByteArrayInputStream(buf)).readObject();
		} catch (Exception e) {
			return null;
		}
    }

    public static byte[] serialize(Object obj) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            new ObjectOutputStream(buf).writeObject(obj);
            return buf.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
    
    public static byte[] decode(String data) throws IOException {
        return new BASE64Decoder().decodeBuffer(data);
    }
    
    public static String encode(byte[] data) {
        return new BASE64Encoder().encode(data).replaceAll("\r|\n| ", "");
    }
    
    static void putAll(Map<String,byte[]> data) {
        synchronized (License.data) {
            if (License.data.isEmpty()) {
                License.data.putAll(data);
            }
        }
    }
}
