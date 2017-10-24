package com.tinawang.spark.hbase.job;

import com.tinawang.spark.hbase.processor.*;
import com.tinawang.spark.hbase.tableDes.ITable;
import com.tinawang.spark.hbase.tableDes.Table;
import com.tinawang.spark.hbase.utils.HbaseKerberos;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.VoidFunction;
import scala.Tuple2;
import scala.util.parsing.combinator.testing.Str;

import java.io.Serializable;

/**
 * Created by wangting26 on 2017/9/18.
 */
@Slf4j
public class SparkWriteHbaseJob implements IJob{

    private JavaSparkContext sc;
    private String batchDate;
    private String jobName;

    public SparkWriteHbaseJob(JavaSparkContext _sc,String _batchDate,String _jobName){
        this.sc = _sc;
        this.batchDate = _batchDate;
        this.jobName = _jobName;
    }

    public void run(){
        log.info("begin run SparkWriteHbaseJob");
        // read hdfs file to rdd
        log.info("load Table prop info from {}", jobName + ".properties");
        ITable table = new Table(jobName + ".properties");
        log.info("tableName is:\t{}",table.getTableName());
        log.info("namespace is:\t{}",table.getNamespace());
        log.info("table cf is:\t{}",table.getCfBytes());
        log.info("table preRegions is:\t{}",table.getPreRegions());
        log.info("table uniqCol is:\t{}",table.getUniqCol());
        log.info("table queryKey is:\t{}",table.getQueryKey());
        log.info("table inputPathPrefix is:\t{}",table.getInputPathPrefix());
        log.info("table hfilePathPrefix is:\t{}",table.getHfilePathPrefix());
        log.info("table cols is:\t{}",table.getBeanHashMap());
        IReadFileRDD readFileRDD = new SparkReadFileRDD(batchDate,table);
        JavaRDD<String> fileRDD =  (JavaRDD<String>) readFileRDD.textFile2RDD(sc);

        // transfer rdd to hfile rdd format
        ITransRDD iTransRDD = new SparkHfileRDD(table,fileRDD);
        JavaPairRDD<ImmutableBytesWritable, KeyValue> hfileRDD = (JavaPairRDD<ImmutableBytesWritable, KeyValue>) iTransRDD.transRDD();

        // bulk load hfile rdd to hbase
        log.info("load kerberos info from kerberos.conf");
        HbaseKerberos kerberos = new HbaseKerberos("kerberos.conf");//kerberos auth
        IHfileLoad2Hbase load2Hbase = new HfileLoad2Hbase(kerberos,table,hfileRDD);
        load2Hbase.bulkLoad();

    }


}
