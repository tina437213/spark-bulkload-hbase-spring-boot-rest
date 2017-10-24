package com.tinawang.spark.hbase.tableDes;

import scala.Tuple2;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by wangting26 on 2017/9/1.
 */
public class TupleComparator implements Serializable,Comparator<Tuple2<String,String>> {

    private static final long serialVersionUID = 12382943439484934L;

    public static final TupleComparator INSTANCE = new TupleComparator();

    private TupleComparator(){

    }

    @Override
    public int compare(Tuple2<String,String> tup1,Tuple2<String,String> tup2){
        if(tup1._1().compareTo(tup2._1()) == 0){
            return tup1._2().compareTo(tup2._2());
        }else{
            return tup1._1().compareTo(tup2._1());
        }
    }


}
