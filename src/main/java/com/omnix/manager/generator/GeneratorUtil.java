package com.omnix.manager.generator;

import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StrSubstitutor;

public class GeneratorUtil {
    
    public static String makeDeviceId(String id) {
        String value = "1" + StringUtils.leftPad(id, 6, "0");
        return value;
    }
    
    public static String replaceString(Map<String, String> keys, String value) {
        return new StrSubstitutor(keys).replace(value);
    }
    
    public static String makeRandomIp(int classA, int classB, int classC, int classD) {
        String ipA = makeRandomValue(classA, 192);
        String ipB = makeRandomValue(classB, 1);
        String ipC = makeRandomValue(classC, 1);
        String ipD = makeRandomValue(classD, 1);
        
        return new StringBuilder().append(ipA).append(".").append(ipB).append(".").append(ipC).append(".").append(ipD).toString();
    }
    
    public static String makeRandomPort() {
        return makeRandomValue(65535);
    }
    
    public static String makeRandom100() {
        return makeRandomValue(100);
    }
    
    public static String makeRandomValue(int max) {
        return makeRandomValue(max, 1);
    }
    
    public static String makeRandomValue(int max, int offset) {
        int port = RandomUtils.nextInt(0, max) + offset;
        return String.valueOf(port);
    }
    
    public static int makeRandomForInt(int max, int offset) {
        return RandomUtils.nextInt(0, max) + offset;
    }
    
    public static int makeRandomValueForInt(int max) {
        return RandomUtils.nextInt(0, max);
    }
    
    public static int makeRandomValueForInt(int min, int max) {
        return RandomUtils.nextInt(min, max);
    }
}
