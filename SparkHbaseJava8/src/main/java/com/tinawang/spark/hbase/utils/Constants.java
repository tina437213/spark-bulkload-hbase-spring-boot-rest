package com.tinawang.spark.hbase.utils;

/**
 * Created by wangting26 on 2017/9/15.
 */
public class Constants {
    public static final String lineSepHexStr = "10"; // line seperator 0x10
    public static final String lineSepAsciiStr = StringUtil.hexToAsciiString(lineSepHexStr);
    public static final String colSepHexStr = "01"; // col seperator 0x01
    public static final String colSepAsciiStr = StringUtil.hexToAsciiString(colSepHexStr);
    public static final String strSepChar = ",";
}
