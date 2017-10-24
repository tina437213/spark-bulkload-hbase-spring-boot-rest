package com.tinawang.spark.hbase.tableDes;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by wangting26 on 2017/9/19.
 */
public interface ITable extends Serializable {
    public String getTableName() ;

    public String getNamespace();

    public String getInputPathPrefix();

    public String getHfilePathPrefix();

    public String getUniqCol();

    public int getPreRegions();

    public String[] getQueryKey() ;

    public Map<String, Integer> getBeanHashMap();

    public byte[] getCfBytes();

    public String generateRowkey(String[] parts);

}
