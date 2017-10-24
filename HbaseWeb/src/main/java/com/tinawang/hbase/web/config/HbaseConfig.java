package com.tinawang.hbase.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by Tina on 2017/8/9 0009.
 */

@Data
@Component
@ConfigurationProperties(prefix = "hbaseenv")
public class HbaseConfig {

    private String zkparent;
    private String security;
    private String krb5conf;
    private String coresite;
    private String hdfssite;
    private String hbasesite;
    private String user;
    private String keypath;

}
