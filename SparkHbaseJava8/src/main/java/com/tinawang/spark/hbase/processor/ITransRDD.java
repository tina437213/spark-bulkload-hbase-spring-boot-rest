package com.tinawang.spark.hbase.processor;

import java.io.Serializable;

/**
 * Created by wangting26 on 2017/9/18.
 */
public interface ITransRDD<T> {
    public T transRDD();
}
