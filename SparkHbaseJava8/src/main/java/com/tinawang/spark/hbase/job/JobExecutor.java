package com.tinawang.spark.hbase.job;

import org.apache.spark.api.java.JavaSparkContext;

/**
 * Created by wangting26 on 2017/9/18.
 */
public class JobExecutor {

    private JavaSparkContext sc;

    public JobExecutor(JavaSparkContext sparkContext) {
        sc = sparkContext;
    }

    public void exe(String jobClass, String batchDate, String jobName) throws Exception {
        Class jobclass = Class.forName(jobClass);
        IJob obj = (IJob) jobclass.getDeclaredConstructor(JavaSparkContext.class,String.class,String.class).newInstance(sc,batchDate,jobName);
        obj.run();
    }


}
