package com.kobekun.spark.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

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
    @After
    public void tearDown(){
        try {
            conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
