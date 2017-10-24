package com.tinawang.spark.hbase.processor;

import com.tinawang.spark.hbase.tableDes.ITable;
import com.tinawang.spark.hbase.tableDes.TablePartion;
import com.tinawang.spark.hbase.tableDes.TupleComparator;
import com.tinawang.spark.hbase.utils.Constants;
import com.tinawang.spark.hbase.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.MD5Hash;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple2;
import scala.util.parsing.combinator.testing.Str;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by wangting26 on 2017/9/18.
 */
@Slf4j
public class SparkHfileRDD implements Serializable,ITransRDD<JavaPairRDD<ImmutableBytesWritable, KeyValue>>{
    private ITable table;
    private JavaRDD<String> fileRDD;

    public SparkHfileRDD(){

    }

    public SparkHfileRDD(ITable _table, JavaRDD<String> _fileRDD){
        this.table = _table;
        this.fileRDD = _fileRDD;
    }


    private JavaPairRDD<Tuple2<String,String>,String> getRowKeyColPairRDD(JavaRDD<String> inputRDD){
        final Map<String,Integer> map = this.table.getBeanHashMap();

        JavaRDD<List<Tuple2<Tuple2<String,String>,String>>> inputListRDD = inputRDD.map(
                (Function<String, List<Tuple2<Tuple2<String, String>, String>>>) line -> {
                    List<Tuple2<Tuple2<String,String>,String>> arrayList =
                            new ArrayList<>();
                    // split line by specified col seperating character , '0x01'
                    String[] parts = line.split(Constants.colSepAsciiStr);
                    /*
                     * calc the rowkey, ensure uniqueness by id
                     * byte[] rowKeyByte = Bytes.add(Bytes.toBytes(salt),Bytes.toBytes(checkIdHash),Bytes.toBytes(id));
                     * salt = checkId hash % preRegion
                     * checkIdHash = MD5Hash.getMD5AsHex(checkId)
                     */

                    String rowKey = generateRowkey(parts);
                    log.info("rowkey is {}" , rowKey);
                    Iterator<Map.Entry<String,Integer>> iterator =
                            map.entrySet().iterator();
                    while(iterator.hasNext()){
                        Map.Entry<String,Integer> entry = iterator.next();
                        Tuple2<Tuple2<String,String>,String> tuple22 =
                                new Tuple2<>(
                                        new Tuple2<>(rowKey,entry.getKey())
                                        ,parts[entry.getValue()].toString());
                        arrayList.add(tuple22);
                    }
                    return arrayList;
                });

        JavaPairRDD<Tuple2<String,String>,String> pairRDD = inputListRDD.flatMapToPair(
                (PairFlatMapFunction<List<Tuple2<Tuple2<String, String>, String>>, Tuple2<String, String>, String>) tuple2s -> tuple2s.iterator());

        return pairRDD;
    }

    private JavaPairRDD<Tuple2<String,String>,String> sortPairRDD(JavaPairRDD<Tuple2<String, String>, String> pairRDD){
        int partitions = this.table.getPreRegions();
        String className = "com.tinawang.spark.hbase.tableDes." + StringUtil.captureName(table.getTableName()) + "Partion";
        Class cls = null;
        TablePartion tp = null;
        try {
            cls = Class.forName(className);
            tp = (TablePartion) cls.getDeclaredConstructor(int.class).newInstance(partitions);
        }
        catch (ClassNotFoundException e) {
            log.error("class not found {}", e);
        } catch (Exception e) {
            log.error("create table partion instance failure {}", e);
        }
        JavaPairRDD<Tuple2<String,String>,String> sortedPairRDD =
                pairRDD.repartitionAndSortWithinPartitions(
                        tp, TupleComparator.INSTANCE);
        return sortedPairRDD;
    }

    private JavaPairRDD<ImmutableBytesWritable, KeyValue> getHfileRdd(JavaPairRDD<Tuple2<String, String>, String> sortedPairRDD) {
        final ITable tab = this.table;
        final JavaPairRDD<ImmutableBytesWritable, KeyValue> hfileRdd =
                sortedPairRDD.mapToPair(
                        (PairFunction<Tuple2<Tuple2<String, String>, String>, ImmutableBytesWritable, KeyValue>) tuple2s -> {
                            byte[] rowkey_byte = Bytes.toBytes(tuple2s._1()._1());
                            byte[] qua_byte = Bytes.toBytes(tuple2s._1()._2());
                            byte[] value_byte = Bytes.toBytes(tuple2s._2());
                            KeyValue keyValue = new KeyValue(rowkey_byte,tab.getCfBytes(), qua_byte, value_byte);
                            return new Tuple2<>(
                                    new ImmutableBytesWritable(rowkey_byte), keyValue);
                        });
        return hfileRdd;
    }

    private String generateRowkey(String[] parts){
        StringBuilder sb = new StringBuilder();
        for(String col:this.table.getQueryKey()){
            sb.append(parts[this.table.getBeanHashMap().get(col)]);
        }
        String queryKey = sb.toString();
        String salt = StringUtils.leftPad(Integer.toString(Math.abs(queryKey.hashCode() % this.table.getPreRegions())),1,"0");
        String queryKeyHash = MD5Hash.getMD5AsHex(Bytes.toBytes(queryKey));//MD5 Hash散列，定长,32位
        String uniqId = parts[this.table.getBeanHashMap().get(this.table.getUniqCol())]; //id原样保留
        byte[] rowKeyByte = Bytes.add(Bytes.toBytes(salt),Bytes.toBytes(queryKeyHash),Bytes.toBytes(uniqId));
        String rowKey = Bytes.toString(rowKeyByte);
        return rowKey;
    }

    public JavaPairRDD<ImmutableBytesWritable, KeyValue> transRDD(){
        JavaPairRDD<Tuple2<String,String>,String> pairRDD = getRowKeyColPairRDD(fileRDD);
        JavaPairRDD<Tuple2<String,String>,String> sortedPairRDD = sortPairRDD(pairRDD);
        JavaPairRDD<ImmutableBytesWritable, KeyValue> hfileRDD = getHfileRdd(sortedPairRDD);
        return hfileRDD;
    }



}
