package com.tinawang.hbase.web.domain.request;

import lombok.Data;

/**
 * Created by wangting26 on 2017/8/14.
 */
@Data
public class RequestOpinion {
    private RequestCommon req_comm;
    private String buss_no;
    private String business_type;
}
