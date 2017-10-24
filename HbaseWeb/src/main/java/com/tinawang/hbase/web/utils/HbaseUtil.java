package com.tinawang.hbase.web.utils;

import com.tinawang.hbase.web.config.HbaseConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.security.UserGroupInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Tina on 2017/8/7 0007.
 */
@Slf4j
@Data
@Configuration
public class HbaseUtil {

    @Autowired
    private  HbaseConfig hbaseConfig;
    public    org.apache.hadoop.conf.Configuration config;

    @PostConstruct
    public void init(){
        System.setProperty("java.security.krb5.conf", hbaseConfig.getKrb5conf());
        config = HBaseConfiguration.create();
        config.set("hadoop.security.authentication", hbaseConfig.getSecurity());
        config.set("zookeeper.znode.parent", hbaseConfig.getZkparent());
        config.addResource(new Path(hbaseConfig.getCoresite()));
        //config.addResource(new Path(hbaseConfig.getHdfssite()));
        config.addResource(new Path(hbaseConfig.getHbasesite()));
        try {
            UserGroupInformation.setConfiguration(config);
            UserGroupInformation.loginUserFromKeytab(hbaseConfig.getUser(), hbaseConfig.getKeypath());
        } catch (IOException e) {
            log.error("Hbase kerberos init authentication error: {}",e.getMessage());
        }
    }

    /**
     * we use HconnectionManager to get connection on hbase 0.98
     * you should use ConnectionFactory after hbase 1.0x
     * @return
     */
    public HConnection getConnection(){
        HConnection conn = null;
        ExecutorService executor = Executors.newFixedThreadPool(10); //default=1
        try {
            conn = HConnectionManager.createConnection(config);
        } catch (IOException e) {
            log.error("get hbase connection error: {}",e);
        }
        return conn;
    }

    /**
     * when process will be finished, close connection
     * */
    public void closeConnection(HConnection conn){
        if(conn != null){
            try {
                conn.close();
            } catch (IOException e) {
                log.error("close hbase connection error: {}",e.getMessage());
            }
        }
    }

    public HTableInterface getTable(HConnection conn,String tableName){
        HTableInterface table = null;
        try {
            table = conn.getTable(tableName);
        } catch (IOException e) {
            log.error("get hbase table error: {}",e.getMessage());
        }
        return table;
    }

    /**
     * when each hbase table data manipulation operations are complete
     * each thread should close corresponding HTable but not the object
     * so that it can be reused by other threads
     * @param table
     */
    public void closeTable(HTableInterface table){
        if(table != null){
            try {
                table.close();
            } catch (IOException e) {
                log.error("close hbase table error: {}",e.getMessage());
            }
        }
    }

}
