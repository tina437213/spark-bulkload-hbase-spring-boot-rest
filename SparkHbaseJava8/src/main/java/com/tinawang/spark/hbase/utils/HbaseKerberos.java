package com.tinawang.spark.hbase.utils;

import java.util.Properties;

/**
 * Created by wangting26 on 2017/9/20.
 */
public class HbaseKerberos {

    private String confFile;
    private String keyUser;
    private String keytabPath;

    public HbaseKerberos(String _confFile){
        this.confFile = _confFile;
        Properties prop = PropsUtil.loadProps(confFile);
        this.keyUser = prop.getProperty("hbase.keyuser");
        this.keytabPath = (Thread.currentThread().getContextClassLoader().getResource(prop.getProperty("hbase.keytab"))).getPath();
    }

    public String getKeyUser() {
        return keyUser;
    }

    public String getKeytabPath() {
        return keytabPath;
    }





}
