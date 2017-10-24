package com.tinawang.spark.hbase.tableDes;

import com.tinawang.spark.hbase.utils.PropsUtil;
import com.tinawang.spark.hbase.utils.StringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.MD5Hash;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Created by wangting26 on 2017/9/15.
 */
@Data
@Slf4j
public class Table implements ITable,Serializable{
    private String tableName;
    private String namespace;
    private String inputPathPrefix;
    private String hfilePathPrefix;
    private String uniqCol;
    private int preRegions;
    private String[] queryKey;
    private Map<String,Integer> beanHashMap;
    private byte[] cfBytes;

    public Table(){

    }
    public Table(String fileName){
        Properties prop = PropsUtil.loadProps(fileName);
        this.tableName = prop.getProperty("tableName");
        this.namespace = prop.getProperty("namespace");
        this.inputPathPrefix = prop.getProperty("inputPathPrefix");
        this.hfilePathPrefix = prop.getProperty("hfilePathPrefix");
        this.uniqCol = prop.getProperty("uniqCol");
        this.preRegions = Integer.parseInt(prop.getProperty("preRegions"));
        this.queryKey = prop.getProperty("queryKey").split(",");
        this.beanHashMap = StringUtil.str2Map(prop.getProperty("cols"));
        this.cfBytes = Bytes.toBytes(prop.getProperty("cf"));
    }


    public String generateRowkey(String[] parts){
        StringBuilder sb = new StringBuilder();

        for(String col:queryKey){
            log.info("col is " + col);
            sb.append(parts[this.beanHashMap.get(col)]);
        }
        String queryKey = sb.toString();
        String salt = StringUtils.leftPad(Integer.toString(Math.abs(queryKey.hashCode() % 10)),1,"0");
        String queryKeyHash = MD5Hash.getMD5AsHex(Bytes.toBytes(queryKey));//MD5 Hash散列，定长,32位
        String uniqId = parts[beanHashMap.get(uniqCol)]; //id原样保留
        byte[] rowKeyByte = Bytes.add(Bytes.toBytes(salt),Bytes.toBytes(queryKeyHash),Bytes.toBytes(uniqId));
        String rowKey = Bytes.toString(rowKeyByte);
        return rowKey;
    }


}
