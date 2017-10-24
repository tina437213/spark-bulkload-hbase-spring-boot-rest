package com.tinawang.hbase.web.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Created by Tina on 2017/8/8 0008.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Opinion {
    private String id;
    private String buss_no;
    private String current_queue;
    private String target_queue;
    private String submit_user;
    private String submit_time;
    private String submit_opinion;
    private String submit_apply_summary;
    private String business_type;
    private String approval_opinion;
    private String appr_result;
    private String reason;
    private String create_by;
    private String create_date;
    private String update_by;
    private String update_date;
    private String remarks;
    private String del_flag;
    private String trunc_no;
}
