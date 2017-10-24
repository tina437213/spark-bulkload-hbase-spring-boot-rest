package com.tinawang.hbase.web.utils;

import com.tinawang.hbase.web.domain.response.Response;

/**
 * Created by wangting26 on 2017/8/15.
 */
public class ResponseUtil {
    public static Response success(Object object){
        Response response = new Response();
        response.setRtn_sts("S");
        response.setRtn_code("0");
        response.setRtn_msg("交易成功");
        response.setSys_code("475");
        response.setData(object);
        return response;
    }

    public static Response success(){
        return success(null);
    }

    public static Response error(String rtnCode, String msg){
        Response response = new Response();
        response.setRtn_sts("E");
        response.setRtn_code(rtnCode);
        response.setRtn_msg(msg);
        response.setSys_code("475");
        return response;
    }
}
