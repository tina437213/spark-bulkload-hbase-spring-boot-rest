package com.tinawang.hbase.web.service;

import com.tinawang.hbase.web.domain.Check;
import com.tinawang.hbase.web.domain.response.Response;
import com.tinawang.hbase.web.utils.HbaseUtil;
import com.tinawang.hbase.web.utils.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.MD5Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangting26 on 2017/8/15.
 */
@Slf4j
@Service
public class CheckQueryService {

    public static String CHECK_TABLE = "tinawang:check";
    public static byte[] CF_BYTE = Bytes.toBytes("f");

    @Autowired
    private HbaseUtil hbaseUtil;

    public Response findByCheckId(String checkId){
        // 注：rowkey生成规则
        // rowkey = checkId.hashCode() % region_num + MD5Hash.getHashAsHex(checkId) (定长)+ ID;

        log.info("================================================");
        log.info("begin process findByCheckId request");

        HConnection conn = null;
        HTableInterface checkTable = null;
        List<Check> checkTest = null;
        try{
            conn = hbaseUtil.getConnection();
            checkTable = hbaseUtil.getTable(conn,CHECK_TABLE);

            String salt = StringUtils.leftPad(Integer.toString(Math.abs(checkId.hashCode() % 3)),1,"0");
            String checkIdHash = MD5Hash.getMD5AsHex(Bytes.toBytes(checkId));//MD5 Hash散列，定长

            String str = new StringBuilder().append(salt).append(checkIdHash).toString();
            log.info("scan query key is " + str);
            Scan scan = new Scan();
            scan.setStartRow(Bytes.toBytes(str)); //scan 此时不生效
            log.info("start row key is " + str);
            byte[] stopRow = Bytes.toBytes(str);
            stopRow[stopRow.length-1]++;
            scan.setStopRow(stopRow);
            log.info("stop row key is " + Bytes.toString(stopRow));
            scan.addFamily(CF_BYTE);

            checkTest = new ArrayList<>();
            ResultScanner rs = checkTable.getScanner(scan);
            /*int num = 0;*/
            Class<?> class1 = Class.forName("com.tinawang.hbase.web.domain.Check");

            for(Result r : rs){
                Object obj = class1.newInstance();
                for(Cell cell: r.listCells()){
                    Field field = class1.getDeclaredField(Bytes.toString(CellUtil.cloneQualifier(cell)));
                    field.setAccessible(true);
                    field.set(obj,Bytes.toString(CellUtil.cloneValue(cell)));
                }
                checkTest.add((Check) obj);
            }
        }catch (Exception e){
            log.error("query error {}", e);
            return ResponseUtil.error("-1","系统查询异常");
        }finally {
            if(checkTable != null){
                try{
                    checkTable.close();
                }catch (IOException e) {
                    log.error("close check table error {}", e);
                }
            }
        }
        log.info("data length is " + checkTest.size());
        log.info("================================================");
        return ResponseUtil.success(checkTest);
    }


}
