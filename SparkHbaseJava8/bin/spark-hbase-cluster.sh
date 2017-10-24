#!/bin/bash
source ~/.bashrc

batchDate=$1
jobClass=$2
jobName=$3

BIN_PATH=$(cd $(dirname $0); pwd)
APP_HOME=$(dirname $BIN_PATH)
LIB_HOME=${APP_HOME}/lib
CONF_HOME=${APP_HOME}/conf
HADOOP_HOME=/usr/hdp/2.6.1.0-129

export PATH=$PATH:/app/tdh/hadoop-clients/base/bin
CLASSPATH=.:${APP_HOME}/lib:${APP_HOME}/jar

export jarslist="$(echo ${LIB_HOME}/*|sed 's/ /,/g')"
export spark_jar=${APP_HOME}/jar/spark-hbase-1.0-SNAPSHOT.jar
export keytabpath=/home/tina/project/kerberos/tina.keytab
export sparkprincipal=tina@GNUHPC.ORG
export filelist="${HADOOP_HOME}/hbase/conf/hbase-site.xml,${HADOOP_HOME}/hadoop/conf/core-site.xml,${CONF_HOME}/kerberos.conf,/home/tina/project/kerberos/tina.keytab.hbase,${CONF_HOME}/${jobName}.conf,${CONF_HOME}/${jobName}.properties"

${SPARK_HOME}/bin/spark-submit --master yarn \
--deploy-mode cluster \
--queue default \
--name spark-hbase \
--class com.tinawang.spark.hbase.StartApp \
--files ${filelist} \
--keytab ${keytabpath} \
--principal ${sparkprincipal} \
--jars ${jarslist} \
${spark_jar} ${batchDate} ${jobClass} ${jobName}













