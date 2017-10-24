package com.tinawang.spark.hbase.tableDes;

import org.apache.spark.Partitioner;
import scala.Tuple2;

/**
 * Created by wangting26 on 2017/9/1.
 */
public abstract class TablePartion extends Partitioner{

    private static final long serialVersionUID = -4133333213242432848L;

    private final int numPartitions;

    public TablePartion(int partions){
        assert (partions > 0);
        this.numPartitions = partions;
    }


    @Override
    public int numPartitions(){
        return numPartitions;
    }
}
