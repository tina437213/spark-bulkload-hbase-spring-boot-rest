package com.tinawang.spark.hbase.processor;

import org.apache.spark.api.java.JavaSparkContext;

/**
 * Created by wangting26 on 2017/9/20.
 */
public interface IReadFileRDD<T> {
    public T textFile2RDD(JavaSparkContext sc);
}
