package com.tinawang.hbase.web.domain.request;

import lombok.Data;

/**
 * Created by wangting26 on 2017/8/14.
 */
@Data
public class RequestCommon {
    private String acct_bch;      //账务机构号
    private String buss_bch;  		//营业机构号
    private String tx_user;   		//交易柜员
    private String tx_dt;     		//交易日期
    private String tx_tm;     		//交易时间
    private String chnl_tx_no;		//渠道流水号
    private String sys_code;  		//系统编码
    private String chnl_code; 		//渠道
    private String tx_code;       //交易行为
}
