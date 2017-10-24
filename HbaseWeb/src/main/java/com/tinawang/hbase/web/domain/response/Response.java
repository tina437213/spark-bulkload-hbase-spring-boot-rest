package com.tinawang.hbase.web.domain.response;

import lombok.Data;

/**
 * Created by wangting26 on 2017/8/15.
 */
@Data
public class Response<T> {
    private String rtn_sts;     //响应状态
    private String rtn_code;    //返回编码
    private String rtn_msg;     //返回信息
    private String sys_code;    //系统编码
    private T data;
}
