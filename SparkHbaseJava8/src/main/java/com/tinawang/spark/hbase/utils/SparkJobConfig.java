package com.tinawang.spark.hbase.utils;

import lombok.extern.log4j.Log4j;
import org.apache.spark.SparkConf;

import java.util.Properties;

/**
 * Created by wangting26 on 2017/9/18.
 */

@Log4j
public class SparkJobConfig {

    private String confFile;

    public SparkJobConfig(){

    }

    public SparkJobConfig(String _confFile){
        this.confFile = _confFile;
    }

    public SparkConf getConfig(){
        log.info("Config file name is:" + confFile);
        Properties prop = PropsUtil.loadProps(confFile);
        SparkConf sparkConf = new SparkConf();
        sparkConf.set("spark.executor.cores",prop.getProperty("spark.executor.cores"));
        sparkConf.set("spark.executor.instances",prop.getProperty("spark.executor.instances"));
        sparkConf.set("spark.executor.memory",prop.getProperty("spark.executor.memory"));
        return sparkConf;
    }


}
