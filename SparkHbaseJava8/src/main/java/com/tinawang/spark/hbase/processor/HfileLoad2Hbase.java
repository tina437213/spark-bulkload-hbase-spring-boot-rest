package com.tinawang.spark.hbase.processor;

import com.tinawang.spark.hbase.tableDes.ITable;
import com.tinawang.spark.hbase.utils.HbaseConfig;
import com.tinawang.spark.hbase.utils.HbaseKerberos;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat2;
import org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.spark.api.java.JavaPairRDD;

import java.io.IOException;
import java.io.Serializable;
import java.security.PrivilegedExceptionAction;

/**
 * Created by wangting26 on 2017/9/19.
 */
@Slf4j
public class HfileLoad2Hbase implements Serializable,IHfileLoad2Hbase<JavaPairRDD<ImmutableBytesWritable, KeyValue>> {

    private HbaseKerberos hbaseKerberos;
    private ITable table;
    private JavaPairRDD<ImmutableBytesWritable, KeyValue> hfileRdd;

    public HfileLoad2Hbase(HbaseKerberos _hbaseKerberos,ITable _table,JavaPairRDD<ImmutableBytesWritable, KeyValue> _hfileRdd){
        this.hbaseKerberos = _hbaseKerberos;
        this.table = _table;
        this.hfileRdd = _hfileRdd;
    }

    /**
     * step1: write hfileRDD to hfile
     * step2: create job and bulk load file to hbase table
     */
    @Override
    public void bulkLoad(){
        try {
            final Configuration config = HbaseConfig.getConfig();
            log.info("created hadoop config and set hbase related xml");
            UserGroupInformation.setConfiguration(config);
            String keyUser = hbaseKerberos.getKeyUser();
            String keytabPath = hbaseKerberos.getKeytabPath();
            log.info("UserGroupInformation kerberos auth");
            UserGroupInformation ugi =
                    UserGroupInformation.loginUserFromKeytabAndReturnUGI(keyUser,keytabPath);
            UserGroupInformation.setLoginUser(ugi);
            log.info("created UserGroupInformation login obj");
            log.info("begin write rdd to hfile");
            ugi.doAs(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    String tableName = table.getNamespace() + ":" + table.getTableName();
                    HTable htable = new HTable(new JobConf(config), Bytes.toBytes(tableName));
                    //byte[][] startKeys = table.getStartKeys();
                    String hfilePath = table.getHfilePathPrefix()
                            + "/" + System.currentTimeMillis();
                    hfileRdd.saveAsNewAPIHadoopFile(hfilePath,ImmutableBytesWritable.class,
                            KeyValue.class,HFileOutputFormat2.class,config);
                    log.info("end write rdd to hfile");

                    log.info("begin to bulkload hfile and create job");
                    config.set(TableOutputFormat.OUTPUT_TABLE,tableName);
                    final Job job = Job.getInstance();
                    job.setMapOutputKeyClass(ImmutableBytesWritable.class);
                    job.setMapOutputValueClass(KeyValue.class);
                    HFileOutputFormat2.configureIncrementalLoad(job,htable);
                    // 利用bulk load hfile,将hfile写入hbase
                    LoadIncrementalHFiles bulkLoader = new LoadIncrementalHFiles(config);
                    bulkLoader.doBulkLoad(new Path(hfilePath),htable);
                    log.info("end to bulkload hfile");
                    htable.close();

                    return null;
                }
            });
        }catch (InterruptedException e) {
            log.error("hfile bulkload to habse run error",e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("hfile bulkload to habse io error",e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
