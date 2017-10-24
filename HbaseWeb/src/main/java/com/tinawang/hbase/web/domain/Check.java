package com.tinawang.hbase.web.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by wangting26 on 2017/8/15.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Check {
    private String id;                           //主键ID
    private String check_id;                     //申请检查表主键ID
    private String rule_no;                      //规则编号
    private String index_value;                  //指标值
    private String rule_type;                    //类型
    private String hit_condition;                //命中条件(是否通过
    private String rule_desc;                    //规则描述
    private String hit_ind;                      //是否命中
    private String artificial_ver_result;        //人工核查结果
    private String cause_code;                   //原因代码
    private String opinin;                       //意见
    private String create_by;                    //创建者
    private String create_date;                  //创建日期
    private String update_by;                    //更新者
    private String update_date;                  //更新日期
    private String remarks;                      //备注
    private String del_flag;                     //删除标志位
    private String trunc_no;                     //乐观锁
}
