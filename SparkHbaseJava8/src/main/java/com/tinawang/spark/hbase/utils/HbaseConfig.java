package com.tinawang.spark.hbase.utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

/**
 * Created by wangting26 on 2017/9/20.
 */
public class HbaseConfig {

    public static Configuration getConfig(){
        Configuration config = new Configuration();
        config.addResource(Thread.currentThread().getContextClassLoader().getResource("hbase-site.xml"));
        config.addResource(Thread.currentThread().getContextClassLoader().getResource("core-site.xml"));
        config = HBaseConfiguration.create(config);
        return config;
    }
}
