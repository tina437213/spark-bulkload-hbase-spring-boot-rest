package com.tinawang.spark.hbase.utils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by wangting26 on 2017/8/25.
 */
public class StringUtil {
    private static Logger Log = LoggerFactory.getLogger(StringUtil.class);

    public static String hexToAsciiString(String hexStr){
        // 行分割符 \0x10，ascii码字符DLE 空格
        // 列分割符 \0x01, ascii码 SOH标题开始
        byte[] bytes = new byte[0];
        String asciiStr = null;
        try {
            bytes = Hex.decodeHex(hexStr.toCharArray());
            asciiStr = new String(bytes,"UTF-8");
            Log.info("ascii str is {}" + asciiStr.toString());
        } catch (DecoderException e) {
            Log.error("hex str {} decoder as hex error",e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.error("ascii str encode error",e.getMessage());
        }
        return asciiStr;
    }

    // 有序hashMap，列名和字段索引对应
    public static Map<String,Integer> str2Map(String str){
        Map<String,Integer> map = new LinkedHashMap<String,Integer>();
        String[] strings = str.split(Constants.strSepChar);
        int idx = 0;
        for(String col: strings){
            map.put(col,idx);
            idx++;
        }
        return map;
    }

    public static String captureName(String name){
        char[] cs = name.toCharArray();
        cs[0] -= 32;
        return String.valueOf(cs);
    }



}
