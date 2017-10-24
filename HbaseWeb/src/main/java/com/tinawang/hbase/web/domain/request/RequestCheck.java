package com.tinawang.hbase.web.domain.request;

import lombok.Data;

/**
 * Created by wangting26 on 2017/8/15.
 */
@Data
public class RequestCheck {
    private RequestCommon req_comm;
    private String check_id;
}
