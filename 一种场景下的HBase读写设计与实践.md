# 一种场景下的HBase读写设计与实践

## 一、背景介绍

​	本项目主要解决check和opinion2张历史数据表（历史数据是指当业务发生过程中的完整中间流程和结果数据）的在线查询。原实现基于Oracle提供存储查询服务，随着数据量的不断增加，在写入和读取过程中面临性能问题，且历史数据仅供业务查询参考，并不影响实际流程，从系统结构上来说，放在业务链条上游比较重。本项目将其置于下游数据处理hadoop分布式平台来实现此需求。下面列一些具体的需求指标：

```
1、 数据量：目前check表的累计数据量为5000w+行，11GB；opinion表的累计数据量为3亿+，约100GB。每日增量约为每张表50万+行，只做insert，不做update。

2、 查询要求：check表的主键为id（Oracle全局id），查询键为check_id，一个check_id对应多条记录，所以需返回对应记录的list； opinion表的主键也是id，查询键是bussiness_no和buss_type，同理返回list。单笔查询返回List大小约50条以下，查询频率为100笔/天左右，查询响应时间2s。
```

## 二、技术选型

​	从数据量及查询要求来看，分布式平台上具备大数据量存储，且提供实时查询能力的组件首选HBase。根据需求做了初步的调研和评估后，大致确定HBase作为主要存储组件。将需求拆解为写入和读取HBase两部分。

读取HBase相对来说方案比较确定，基本根据需求设计RowKey，然后根据HBase提供的丰富API（get，scan等）来读取数据，满足性能要求即可。

写入HBase的方法大致有以下几种：

> 1、 Java调用HBase原生API，HTable.add(List(Put))。
>
> 2、 MapReduce作业，使用 TableOutputFormat 作为输出。
>
> 3、 Bulk Load，先将数据按照HBase的内部数据格式生成持久化的 HFile 文件，然后复制到合适的位置并通知 RegionServer ，即完成海量数据的入库。其中生成Hfile这一步可以选择MapReduce或Spark。

本文采用第3种方式，Spark + Bulk Load 写入HBase。该方法相对其他2种方式有以下优势：

> ① BulkLoad不会写WAL，也不会产生flush以及split。
>
> ②如果我们大量调用PUT接口插入数据，可能会导致大量的GC操作。除了影响性能之外，严重时甚至可能会对HBase节点的稳定性造成影响，采用BulkLoad无此顾虑。
>
> ③过程中没有大量的接口调用消耗性能。
>
> ④可以利用spark 强大的计算能力。

图示如下：

![](https://raw.githubusercontent.com/tina437213/spark-bulkload-hbase-spring-boot-rest/master/img/spark-hbase-rest.png)

## 三、设计

### 3.1 环境信息

```
Hadoop 2.5-2.7
HBase 0.98.6
Spark 2.0.0-2.1.1
Sqoop 1.4.6
```

### 3.2 表设计

​	本段的重点在于讨论HBase表的设计，其中RowKey是最重要的部分。为了方便说明问题，我们先来看看数据格式。以下以check举例，opinion同理。

​	check表（原表字段有18个，为方便描述，本文截选5个字段示意）

| id                               | check_id   | rule_no | rule_type | rule_desc |
| -------------------------------- | ---------- | ------- | --------- | --------- |
| 59C1A1FDCBBE01ECE053C5013807B6BE | A208848994 | 0001    | ABCD      | 规则01      |
| 59C1A1FDCBBF01ECE053C5013807B6BE | A208848994 | 0002    | CDBA      | 规则03      |
| 59C1A1FDCBC001ECE053C5013807B6BE | A208848994 | 0002    | CDBB      | 规则02      |

​	如上图所示，主键为id，32位字母和数字随机组成，业务查询字段check_id为不定长字段（不超过32位），字母和数字组成，同一check_id可能对应多条记录，其他为相关业务字段。众所周知，HBase是基于RowKey提供查询，且要求RowKey是唯一的。RowKey的设计主要考虑的是数据将怎样被访问。初步来看，我们有2种设计方法。

> ① 拆成2张表，一张表id作为RowKey，列为check表对应的各列；另一张表为索引表，RowKey为check_id，每一列对应一个id。查询时，先找到check_id对应的id list，然后根据id找到对应的记录。均为HBase的get操作。
>
> ②将本需求可看成是一个范围查询，而不是单条查询。将check_id作为RowKey的前缀，后面跟id。查询时设置Scan的startRow和stopRow，找到对应的记录list。

​	第一种方法优点是表结构简单，RowKey容易设计，缺点为1）数据写入时，一行原始数据需要写入到2张表，且索引表写入前需要先扫描该RowKey是否存在，如果存在，则加入一列，否则新建一行，2）读取的时候，即便是采用List<get>,也至少需要读取2次表。第二种设计方法，RowKey设计较为复杂，但是写入和读取都是一次性的。综合考虑，我们采用第二种设计方法。

### 3.3 RowKey设计

#### 3.3.1 热点问题

hbase 中的行是以 RowKey 的字典序排序的，其热点问题通常发生在大量的客户端直接访问集群的一个或极少数节点。默认情况下，在开始建表时，表只会有一个region，并随着region增大而拆分成更多的region，这些region才能分布在多个regionserver上从而使负载均分。对于我们的业务需求，存量数据已经较大，因此有必要在一开始就将HBase的负载均摊到每个regionserver，即做pre-split。常见的防治热点的方法为加盐，hash，自增部分（如时间戳）翻转等。

#### 3.3.2 RowKey设计

> Step1：确定预分区数目，创建HBase Table

不同的业务场景及数据特点确定数目的方式不一样，我个人认为应该综合考虑数据量大小和集群大小等因素。比如check表大小约为11G，测试集群大小为10台机器，hbase.hregion.max.filesize=3G（当region的大小超过这个数时，将拆分为2个），所以初始化时尽量使得一个region的大小为1~2G（不会一上来就split），region数据分到11G/2G=6个，但为了充分利用集群资源，本文中check表划分为10个分区。如果数据量为100G，且不断增长，集群情况不变，则region数目增大到100G/2G=50个左右较合适。Hbase check表建表语句如下：

```
create 'tinawang:check',
{ NAME => 'f', COMPRESSION => 'SNAPPY',DATA_BLOCK_ENCODING => 'FAST_DIFF',BLOOMFILTER=>'ROW'},
{SPLITS => [ '1','2','3', ‘4’,’5’,’6’,’7’,’8’,’9’]}
```

其中，Column Family =‘f’，越短越好。

COMPRESSION => 'SNAPPY'，hbase支持3种压缩LZO, GZIP and Snappy。GZIP压缩率高，但是耗CPU。后两者差不多，Snappy稍微胜出一点，cpu消耗的比GZIP少。一般在IO和CPU均衡下，选择Snappy。

DATA_BLOCK_ENCODING => 'FAST_DIFF'，本案例中RowKey较为接近，通过以下命令查看key长度相对value较长。

```
./hbase org.apache.hadoop.hbase.io.hfile.HFile -m -f  /hyperbase1/data/tinawang/check/a661f0f95598662a53b3d8b1ae469fdf/f/a5fefc880f87492d908672e1634f2eed_SeqId_2_
```

![](https://raw.githubusercontent.com/tina437213/spark-bulkload-hbase-spring-boot-rest/master/img/hfile.png)

> Step2：RowKey组成

**Salt**

让数据均衡的分布到各个Region上，结合pre-split，我们对查询键即check表的check_id求hashcode值，然后modulus(numRegions) 作为前缀，注意补齐数据。
`StringUtils.leftPad(Integer.toString(Math.abs(check_id.hashCode() % numRegion)),1,’0’)`
说明：如果数据量达上百G以上，则numRegions自然到2位数，则salt也为2位。

**Hash**

因为check_id本身是不定长的字符数字串，为使数据散列化，方便RowKey查询和比较，我们对check_id采用SHA1散列化，并使之32位定长化。

``` 
MD5Hash.getMD5AsHex(Bytes.toBytes(check_id))
```

**唯一性**

以上salt+hash作为RowKey前缀，加上check表的主键id来保障RowKey唯一性。综上，check表的RowKey设计如下：（check_id=A208849559）

| Salt（即Region 分区号，1位） | Hash（check_id）， 32位              | Id ，32位                          |
| -------------------- | -------------------------------- | -------------------------------- |
| 7                    | 7c9498b4a83974da56b252122b9752bf | 56B63AB98C2E00B4E053C501380709AD |

为增强可读性，中间还可以加上自定义的分割符，如’+’,’|’等。

`7+7c9498b4a83974da56b252122b9752bf+56B63AB98C2E00B4E053C501380709AD`

以上设计能保证对每次查询而言，其salt+hash前缀值是确定的，并且落在同一个region中。需要说明的是HBase中check表的各列同数据源Oracle中check表的各列存储。

### 3.4 WEB查询设计

RowKey设计与查询息息相关，查询方式决定RowKey设计，反之基于以上RowKey设计，查询时通过设置Scan的[startRow，stopRow],即可完成扫描。以查询check_id=A208849559为例，根据RowKey的设计原则，对其进行salt+hash计算，得前缀。

```
startRow = 7+7c9498b4a83974da56b252122b9752bf
stopRow = 7+7c9498b4a83974da56b252122b9752bg
```



## 四、代码实现关键流程

### 4.1 Spark write to HBase

**Step0: prepare work**

因为是从上游系统承接的业务数据，存量数据采用sqoop抽到hdfs；增量数据每日以文件的形式从ftp站点获取。因为业务数据字段中包含一些换行符，且sqoop1.4.6目前只支持单字节，所以本文选择’0x01’作为列分隔符，’0x10’作为行分隔符。

**Step1: Spark read hdfs text file** 

``` 
Configuration hadoopConfig = new Configuration(sc.hadoopConfiguration());
hadoopConfig.set("textinputformat.record.delimiter", Constants.lineSepAsciiStr);
// read input file line into Value obj Text,with specified line seperating character "0x10"
String path = this.table.getInputPathPrefix() + "/" + batchDate;
JavaRDD<String> textRDD = sc.newAPIHadoopFile(
                path, TextInputFormat.class, LongWritable.class, Text.class, hadoopConfig)
                .map( t2 -> t2._2.toString());
```

SparkContext.textfile()默认行分隔符为”\n”，此处我们用“0x10”，需要在Configuration中配置。应用配置，我们调用newAPIHadoopFile方法来读取hdfs文件，返回JavaPairRDD<LongWritable, Text>，其中LongWritable和Text分别为hadoop中的Long类型和String类型（所有hadoop数据类型和java的数据类型都很相像，除了它们是针对网络序列化而做的特殊优化）。我们需要的数据文件放在pairRDD的value中，即Text指代。为后续处理方便，可将JavaPairRDD<LongWritable, Text>转换为JavaRDD< String >。

**Step2: Transfer and sort RDD**  

①  将avaRDD< String>转换成JavaPairRDD<Tuple2<String,String>,String>，其中参数依次表示为，RowKey，col，value。做这样转换是因为HBase的基本原理是基于RowKey排序的，并且当采用bulk load方式将数据写入多个预分区（region）时，要求spark各partition的数据是有序的，RowKey，column family（cf），col name均需要有序。在本案例中因为只有一个列簇，所有将RowKey和col组织出来。请注意原本数据库中的一行记录，n列，此时会被拆成n行。

② 基于JavaPairRDD<Tuple2<String,String>,String>进行RowKey，col的二次排序。如果不做排序，会报以下异常：

java.io.IOException: Added a key notlexically larger than previous key

③ 将数据组织成HFile要求的JavaPairRDD<ImmutableBytesWritable,KeyValue> hfileRDD。

**Step3：create hfile and bulk load to HBase **  

①主要调用saveAsNewAPIHadoopFile方法：

``` 
hfileRdd.saveAsNewAPIHadoopFile(hfilePath,ImmutableBytesWritable.class,
                           KeyValue.class,HFileOutputFormat2.class,config);
```

② hfilebulk load to HBase

```
final Job job = Job.getInstance();
job.setMapOutputKeyClass(ImmutableBytesWritable.class);
job.setMapOutputValueClass(KeyValue.class);
HFileOutputFormat2.configureIncrementalLoad(job,htable);
LoadIncrementalHFiles bulkLoader = newLoadIncrementalHFiles(config);
bulkLoader.doBulkLoad(newPath(hfilePath),htable);
```

注：如果集群开启了kerberos，step4需要放置在ugi.doAs（）方法中，在进行如下验证后实现

```
UserGroupInformation ugi =
                   UserGroupInformation.loginUserFromKeytabAndReturnUGI(keyUser,keytabPath);
            UserGroupInformation.setLoginUser(ugi);
```

访问hbase集群的60010端口web，可以看到region分布情况。

![](https://raw.githubusercontent.com/tina437213/spark-bulkload-hbase-spring-boot-rest/master/img/hbase-region.png)

### 4.2 Read from HBase

本文基于spring boot框架来开发web端访问hbase内数据。

#### 4.2.1 use connection pool(使用连接池)

创建连接是一个比较重的操作，在实际hbase工程中，我们引入连接池来共享zk连接，meta信息缓存，region
server和master的连接。

```
HConnection connection = HConnectionManager.createConnection(config);
HTableInterface table = connection.getTable("table1");
try {
   // Use the table as needed, for a single operation and a single thread
 } finally {
   table.close();
}
```

也可以通过以下方法，覆盖默认线程池。

```
HConnection createConnection(org.apache.hadoop.conf.Configuration conf,
                           ExecutorService pool);
```

#### 4.2.2 process query

**Step1: 根据查询条件，确定RowKey前缀**

根据3.3 RowKey设计介绍，hbase的写和读都遵循该设计规则。此处我们采用相同的方法，将web调用方传入的查询条件，转化成对应的RowKey前缀。例如，查询check表传递过来的check_id=A208849559，生成前缀7+7c9498b4a83974da56b252122b9752bf。

**Step2：确定scan范围**

A208849559对应的查询结果数据即在RowKey前缀为7+7c9498b4a83974da56b252122b9752bf对应的RowKey及value中。

```
scan.setStartRow(Bytes.toBytes(rowkey_pre)); //scan, 7+7c9498b4a83974da56b252122b9752bf
byte[] stopRow = Bytes.toBytes(rowkey_pre);
stopRow[stopRow.length-1]++;
scan.setStopRow(stopRow);// 7+7c9498b4a83974da56b252122b9752bg
```

**Step3：查询结果组成返回对象**

遍历ResultScanner对象，将每一行对应的数据封装成table entity，组成list返回。

## 五、测试

从原始数据中随机抓取1000个check_id，用于模拟测试，连续发起3次请求数为2000（200个线程并发，循环10次），平均响应时间为51ms，错误率为0。

![](https://raw.githubusercontent.com/tina437213/spark-bulkload-hbase-spring-boot-rest/master/img/hbase-jmeter-test.png)

![](https://raw.githubusercontent.com/tina437213/spark-bulkload-hbase-spring-boot-rest/master/img/hbase-request-balance.png)

如上图，经历N次累计测试后，各个region上的Requests数较为接近，符合负载均衡设计之初。

## 六、踩坑记录

**1、kerberos认证问题**

如果集群开启了安全认证，那么在进行spark提交作业以及访问hbase时，均需要进行kerberos认证。

本文采用yarn cluster模式，像提交普通作业一样，可能会报以下错误。

```
ERROR StartApp: job failure,
java.lang.NullPointerException
        at com.tinawang.spark.hbase.utils.HbaseKerberos.<init>(HbaseKerberos.java:18)
        at com.tinawang.spark.hbase.job.SparkWriteHbaseJob.run(SparkWriteHbaseJob.java:60)
```

定位到HbaseKerberos.java:18，代码如下：

```
this.keytabPath = (Thread.currentThread().getContextClassLoader().getResource(prop.getProperty("hbase.keytab"))).getPath();
```

这是因为executor在进行hbase连接时，需要重新认证，通过--keytab上传的tina.keytab并未被hbase认证程序块获取到，所以认证的keytab文件需要另外通过--files上传。示意如下

```
--keytab /path/tina.keytab \
--principal tina@GNUHPC.ORG \
--files "/path/tina.keytab.hbase"
```

其中tina.keytab.hbase是将tina.keytab复制并重命名而得。因为spark不允许同一个文件重复上传。

**2、序列化**

```
org.apache.spark.SparkException: Task not serializable
        at org.apache.spark.util.ClosureCleaner$.ensureSerializable(ClosureCleaner.scala:298)
        at org.apache.spark.util.ClosureCleaner$.org$apache$spark$util$ClosureCleaner$$clean(ClosureCleaner.scala:288)
        at org.apache.spark.util.ClosureCleaner$.clean(ClosureCleaner.scala:108)
        at org.apache.spark.SparkContext.clean(SparkContext.scala:2101)
        at org.apache.spark.rdd.RDD$$anonfun$map$1.apply(RDD.scala:370)
        at org.apache.spark.rdd.RDD$$anonfun$map$1.apply(RDD.scala:369)
        ...
org.apache.spark.deploy.yarn.ApplicationMaster$$anon$2.run(ApplicationMaster.scala:637)
Caused by: java.io.NotSerializableException: org.apache.spark.api.java.JavaSparkContext
Serialization stack:
        - object not serializable (class: org.apache.spark.api.java.JavaSparkContext, value: org.apache.spark.api.java.JavaSparkContext@24a16d8c)
        - field (class: com.tinawang.spark.hbase.processor.SparkReadFileRDD, name: sc, type: class org.apache.spark.api.java.JavaSparkContext)
...
```

解决方法一：

如果sc作为类的成员变量，在方法中被引用，则加transient关键字，使其不被序列化。

```
private transient JavaSparkContext sc;
```

解决方法二：

将sc作为方法参数传递，同时使涉及RDD操作的类implements Serializable。 代码中采用第二种方法。详见代码。

**3、批量请求测试**

```
Exception in thread "http-nio-8091-Acceptor-0" java.lang.NoClassDefFoundError: org/apache/tomcat/util/ExceptionUtils
或者
Exception in thread "http-nio-8091-exec-34" java.lang.NoClassDefFoundError: ch/qos/logback/classic/spi/ThrowableProxy
```

查看下面issue以及一次排查问题的过程，可能是open file超过限制。

> <https://github.com/spring-projects/spring-boot/issues/1106>
>
> <http://mp.weixin.qq.com/s/34GVlaYDOdY1OQ9eZs-iXg>

使用ulimit-a  查看每个用户默认打开的文件数为1024。

在系统文件/etc/security/limits.conf中修改这个数量限制，

在文件中加入内容：

```
* soft nofile 65536 
* hard nofile 65536
```

## 参考文献

> <http://hbase.apache.org/book.html#perf.writing>
>
> <http://www.opencore.com/blog/2016/10/efficient-bulk-load-of-hbase-using-spark/>
>
> <http://hbasefly.com/2016/03/23/hbase_writer/>
>
> <https://github.com/spring-projects/spring-boot/issues/1106>
>
> <http://mp.weixin.qq.com/s/34GVlaYDOdY1OQ9eZs-iXg>

