package com.tinawang.spark.hbase.processor;

import com.tinawang.spark.hbase.tableDes.ITable;
import com.tinawang.spark.hbase.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import scala.Tuple2;

import java.io.Serializable;

/**
 * Created by wangting26 on 2017/9/19.
 */
@Slf4j
public class SparkReadFileRDD implements Serializable, IReadFileRDD<JavaRDD<String>>{

    //private transient JavaSparkContext sc;
    private String batchDate;
    private ITable table;

    /*public SparkReadFileRDD(JavaSparkContext _sc,String _batchDate,ITable _table){
        this.sc = _sc;
        this.batchDate = _batchDate;
        this.table = _table;
    }*/
    public SparkReadFileRDD(String _batchDate,ITable _table){
        this.batchDate = _batchDate;
        this.table = _table;
    }

    @Override
    public JavaRDD<String> textFile2RDD(JavaSparkContext sc){ //泛型
        log.info("begin read hdfs file to pair rdd");
        Configuration hadoopConfig = new Configuration(sc.hadoopConfiguration());
        hadoopConfig.set("textinputformat.record.delimiter", Constants.lineSepAsciiStr);

        // TextInputFormat,KeyValueTextInputFormat,SequenceFileInputFormat..
        // Text,LongWritable,IntWriteable,ByteWriteable
        // read input text file line content into Value obj Text,with specified line seperating character "0x10"
        String path = this.table.getInputPathPrefix() + "/" + batchDate;

        JavaRDD<String> textRDD = sc.newAPIHadoopFile(
                path, TextInputFormat.class, LongWritable.class, Text.class, hadoopConfig)
                .map( t2 -> t2._2.toString()); //(Function<Tuple2<LongWritable, Text>, String>)

        return textRDD;
    }



}
