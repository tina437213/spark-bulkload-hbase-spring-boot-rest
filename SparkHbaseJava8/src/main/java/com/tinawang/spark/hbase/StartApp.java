package com.tinawang.spark.hbase;

import com.tinawang.spark.hbase.job.JobExecutor;
import com.tinawang.spark.hbase.utils.SparkJobConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

/**
 * Created by wangting26 on 2017/9/15.
 */
@Slf4j
public class StartApp {

    public static void main(String[] args) {
        // 处理入参
        String batchDate = args[0];//means the batch date(data_date)
        String jobClass = "com.tinawang.spark.hbase.job." + args[1]; // eg, SparkWriteHbaseob
        String jobName = args[2];//get spark conf files

        log.info("batchDate is: {}",batchDate);
        log.info("jobClass is:{}",jobClass);
        log.info("jobName is:{}",jobName);
        log.info("begin load spark config {},like executor,memory of the current app job", jobName+".conf");
        SparkJobConfig config = new SparkJobConfig(jobName + ".conf");
        SparkConf sparkConf = config.getConfig();
        JavaSparkContext sc = new JavaSparkContext(sparkConf);

        try {
            JobExecutor executor = new JobExecutor(sc);
            executor.exe(jobClass, batchDate, jobName);
        } catch (Exception e) {
            log.error("job failure {}", e);
            System.exit(1);
        } finally {
            sc.stop();
        }


    }

}
