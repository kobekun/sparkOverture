package com.kobekun.spark.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HbaseApp {

    Connection conn = null;
    Table table = null;
    Admin admin = null;

    String tableName = "kobekun_hbase_java_api";

    @Before
    public void setUp(){
        Configuration conf = new Configuration();
        conf.set("hbase.rootdir","hdfs://hadoop001:8020/hbase");
        conf.set("hbase.zookeeper.quorum","hadoop001:2181");

        try {
            conn = ConnectionFactory.createConnection(conf);
            admin = conn.getAdmin();

            Assert.assertNotNull(conn);
            Assert.assertNotNull(admin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getConnection(){

    }

    @Test
    public void createTable() throws IOException {

        TableName table = TableName.valueOf(tableName);

        if(admin.tableExists(table)){
            System.out.println(tableName + "已经存在");
        }else{
            HTableDescriptor descriptor = new HTableDescriptor(table);
            descriptor.addFamily(new HColumnDescriptor("info"));
            descriptor.addFamily(new HColumnDescriptor("address"));
            admin.createTable(descriptor);

            System.out.println(tableName + "表创建成功");
        }
    }

    @Test
    public void queryTableInfos() throws IOException {

        HTableDescriptor[] tables = admin.listTables();

        if(tables.length > 0){

            for(HTableDescriptor table : tables){
                System.out.println(table.getNameAsString());

                HColumnDescriptor[] columnDescriptors = table.getColumnFamilies();
                for(HColumnDescriptor columnDescriptor: columnDescriptors){
                    System.out.println("\t" + columnDescriptor.getNameAsString());
                }
            }
        }
    }

    @Test
    public void testPut() throws IOException {

        table = conn.getTable(TableName.valueOf(tableName));

        Put put = new Put(Bytes.toBytes("kobekun"));

        //通过put设置cf、qualifier、value
        put.addColumn(Bytes.toBytes("info"),Bytes.toBytes("age"),
                Bytes.toBytes("26"));
        put.addColumn(Bytes.toBytes("info"),Bytes.toBytes("birthday"),
                Bytes.toBytes("1993-07-22"));
        put.addColumn(Bytes.toBytes("info"),Bytes.toBytes("company"),
                Bytes.toBytes("baidu"));
        put.addColumn(Bytes.toBytes("address"),Bytes.toBytes("country"),
                Bytes.toBytes("cn"));
        put.addColumn(Bytes.toBytes("address"),Bytes.toBytes("province"),
                Bytes.toBytes("qinghai"));
        put.addColumn(Bytes.toBytes("address"),Bytes.toBytes("city"),
                Bytes.toBytes("geermu"));

        table.put(put);

        List<Put> puts = new ArrayList<>();

        Put put1 = new Put(Bytes.toBytes("yangsidan"));
        put1.addColumn(Bytes.toBytes("info"),Bytes.toBytes("age"),
                Bytes.toBytes("27"));
        put1.addColumn(Bytes.toBytes("info"),Bytes.toBytes("birthday"),
                Bytes.toBytes("1991-06-12"));
        put1.addColumn(Bytes.toBytes("info"),Bytes.toBytes("company"),
                Bytes.toBytes("huawei"));
        put1.addColumn(Bytes.toBytes("address"),Bytes.toBytes("country"),
                Bytes.toBytes("cn"));
        put1.addColumn(Bytes.toBytes("address"),Bytes.toBytes("province"),
                Bytes.toBytes("yunnan"));
        put1.addColumn(Bytes.toBytes("address"),Bytes.toBytes("city"),
                Bytes.toBytes("dali"));

        Put put2 = new Put(Bytes.toBytes("leibian"));
        put2.addColumn(Bytes.toBytes("info"),Bytes.toBytes("age"),
                Bytes.toBytes("26"));
        put2.addColumn(Bytes.toBytes("info"),Bytes.toBytes("birthday"),
                Bytes.toBytes("1992-09-13"));
        put2.addColumn(Bytes.toBytes("info"),Bytes.toBytes("company"),
                Bytes.toBytes("ali"));
        put2.addColumn(Bytes.toBytes("address"),Bytes.toBytes("country"),
                Bytes.toBytes("cn"));
        put2.addColumn(Bytes.toBytes("address"),Bytes.toBytes("province"),
                Bytes.toBytes("guizhou"));
        put2.addColumn(Bytes.toBytes("address"),Bytes.toBytes("city"),
                Bytes.toBytes("guiyang"));

        Put put3 = new Put(Bytes.toBytes("xi"));
        put3.addColumn(Bytes.toBytes("info"),Bytes.toBytes("age"),
                Bytes.toBytes("27"));
        put3.addColumn(Bytes.toBytes("info"),Bytes.toBytes("birthday"),
                Bytes.toBytes("1991-07-29"));
        put3.addColumn(Bytes.toBytes("info"),Bytes.toBytes("company"),
                Bytes.toBytes("tencent"));
        put3.addColumn(Bytes.toBytes("address"),Bytes.toBytes("country"),
                Bytes.toBytes("cn"));
        put3.addColumn(Bytes.toBytes("address"),Bytes.toBytes("province"),
                Bytes.toBytes("guangdong"));
        put3.addColumn(Bytes.toBytes("address"),Bytes.toBytes("city"),
                Bytes.toBytes("shenzhen"));

        puts.add(put1);
        puts.add(put2);
        puts.add(put3);

        table.put(puts);

        System.out.println(tableName + "表数据插入成功");
    }

    //有就更新，没有就update
    @Test
    public void testUpdate() throws IOException {
        table = conn.getTable(TableName.valueOf(tableName));

        Put put = new Put(Bytes.toBytes("xi"));
        put.addColumn(Bytes.toBytes("info"),Bytes.toBytes("company"),
                Bytes.toBytes("PDD"));

        table.put(put);
        System.out.println(tableName + "表数据修改成功");
    }

    //获取打印rowkey等信息
    @Test
    public void testGet01() throws IOException {
        table = conn.getTable(TableName.valueOf(tableName));
        Get get = new Get("yangsidan".getBytes());
//        get.addColumn(Bytes.toBytes("info"),Bytes.toBytes("age"));

        Result result = table.get(get);
        printResult(result);
    }

    //打印result,注意此处不能有 @Test
    public void printResult(Result result){

        for(Cell cell : result.rawCells()){

            System.out.println(Bytes.toString(result.getRow()) + "\t"
                    + Bytes.toString(CellUtil.cloneFamily(cell)) + "\t"
                    + Bytes.toString(CellUtil.cloneQualifier(cell)) + "\t"
                    + Bytes.toString(CellUtil.cloneValue(cell)) + "\t"
                    + cell.getTimestamp());
        }

    }

    //全表扫描
    @Test
    public void testScan01() throws IOException {
        table = conn.getTable(TableName.valueOf(tableName));

        Scan scan = new Scan();
//        scan.addFamily(Bytes.toBytes("info"));
        scan.addColumn(Bytes.toBytes("info"),Bytes.toBytes("company"));

//        Scan scan = new Scan(Bytes.toBytes("leibian")); // >=rowkey索引
//        Scan scan = new Scan(new Get(Bytes.toBytes("leibian")));
//        Scan scan = new Scan(Bytes.toBytes("leibian"),Bytes.toBytes("yangsidan"));  // 包前不包后
        ResultScanner rs = table.getScanner(scan);
//        ResultScanner rs = table.getScanner(Bytes.toBytes("info"),Bytes.toBytes("company"));
        for(Result result : rs){
           printResult(result);
            System.out.println("************************");
        }

    }

    @Test
    public void testFilter() throws IOException {

        table = conn.getTable(TableName.valueOf(tableName));

        Scan scan = new Scan();
//        String reg = "^*dan";
//        Filter filter = new RowFilter(CompareFilter.CompareOp.EQUAL,
//                new RegexStringComparator(reg));

//        Filter filter = new PrefixFilter(Bytes.toBytes("k"));
//        scan.setFilter(filter);

        FilterList filters = new FilterList(FilterList.Operator.MUST_PASS_ONE); //满足一个即可

        Filter filter1 = new PrefixFilter(Bytes.toBytes("l"));
        Filter filter2 = new PrefixFilter(Bytes.toBytes("y"));

        filters.addFilter(filter1);
        filters.addFilter(filter2);

        scan.setFilter(filters);

        ResultScanner rs = table.getScanner(scan);
        for(Result result : rs){
            printResult(result);
            System.out.println("************************");
        }

    }
    @After
    public void tearDown(){
        try {
            conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
