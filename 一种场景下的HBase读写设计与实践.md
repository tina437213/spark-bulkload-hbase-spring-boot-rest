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

​	不同的业务场景及数据特点确定数目的方式不一样，我个人认为应该综合考虑数据量大小和集群大小等因素。比如check表大小约为11G，测试集群大小为10台机器，hbase.hregion.max.filesize=3G（当region的大小超过这个数时，将拆分为2个），所以初始化时尽量使得一个region的大小为1~2G（不会一上来就split），region数据分到11G/2G=6个，但为了充分利用集群资源，本文中check表划分为10个分区。如果数据量为100G，且不断增长，集群情况不变，则region数目增大到100G/2G=50个左右较合适。Hbase check表建表语句如下：

```
create 'tinawang:check',
{ NAME => 'f', COMPRESSION => 'SNAPPY',DATA_BLOCK_ENCODING => 'FAST_DIFF',BLOOMFILTER=>'ROW'},
{SPLITS => [ '1','2','3', ‘4’,’5’,’6’,’7’,’8’,’9’]}
```

