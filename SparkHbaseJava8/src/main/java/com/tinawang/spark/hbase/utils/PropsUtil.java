package com.tinawang.spark.hbase.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by wangting26 on 2017/9/15.
 */
public final class PropsUtil {
    private static final Logger LOG = LoggerFactory.getLogger(PropsUtil.class);

    public static Properties loadProps(String fileName){
        Properties props =  null;
        InputStream is = null;

        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
            if(is == null){
                throw new FileNotFoundException(fileName+" file is not found");
            }
            props = new Properties();
            props.load(is);
        }catch (IOException e) {
            LOG.error("load properties file failure", e);
        }finally {
            if(is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    LOG.error("close input stream failure", e);
                }
            }
        }
        return props;
    }


}
