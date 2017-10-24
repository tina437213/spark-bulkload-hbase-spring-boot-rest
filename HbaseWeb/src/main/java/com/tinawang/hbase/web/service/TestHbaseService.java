package com.tinawang.hbase.web.service;

import com.tinawang.hbase.web.utils.HbaseUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * Created by Tina on 2017/8/7 0007.
 */
@Slf4j
@Service
public class TestHbaseService {

    @Autowired
    private HbaseUtil hbaseUtil;
    private HBaseAdmin admin;

    @PostConstruct
    public void init() throws IOException {
        log.info("hbase config is {}", hbaseUtil.config);
        admin = new HBaseAdmin(hbaseUtil.config);
        log.info("create admin successfully");
    }

    public String getHbaseTableList() {
        StringBuilder tableListStr = new StringBuilder();
        try {
            HTableDescriptor hTableDescriptors[] = admin.listTableDescriptorsByNamespace("tinawang");
            log.info("table list of namespace tinawang is:");
            for(HTableDescriptor hTableDescriptor :hTableDescriptors){
                tableListStr.append(hTableDescriptor.getNameAsString()).append(System.getProperty("line.separator"));
                log.info(hTableDescriptor.getNameAsString());
            }
        } catch (IOException e) {
            log.error("get table list under namespace error {}",e.getMessage());
            return "get table list error";
        }

        return tableListStr.toString();

    }

    @PreDestroy
    public void  destroy() throws IOException {
        admin.close();
        log.info("close admin successfully");
    }

}
