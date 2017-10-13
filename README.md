# spark-bulkload-hbase-spring-boot-rest
This project compose of two parts: 1) write, spark job to write to hbase using bulk load to; 2)read, rest api reading from hbase base on spring boot frame
## Prerequisites
  ```
  JDK:  1.8
  Hadoop: 2.7
  HBase:  0.98-1.1.2
  Spark:  2.0+
  Sqoop:  1.4.6
  ```
## Usage
### config
SparkHbase->conf->table.conf(eg: my table is check, check.conf)
```
  spark.executor.cores=4
  spark.executor.instances=3
  spark.executor.memory=6G
```
SparkHbase->conf->table.properties(eg:check.properties)
```
  tableName=check
  namespace=tinawang
  cf=f
  preRegions=3
  uniqCol=id
  queryKey=check_id
  inputPathPrefix=/user/tina/check
  hfilePathPrefix=/user/tina/hfile/check
  cols=id,check_id,rule_no,rule_type,rule_desc
```
SparkHbase->conf->kerberos.conf(if your hadoop cluster opend security authentication with kerberos)
```
  hbase.keyuser=tina@GNUHPC.ORG
  hbase.keytab=tina.keytab
```
### Deploy
SparkHbase
```
```
you will get an zip package indicated below 

HbaseWeb
```
```
you will get an jar package, and copy it somewhere, then exe the following command

## Example
## Documentation
