package com.qiuzhisystem.utils;

import java.sql.Connection;
import java.sql.DriverManager;
/**
 * 数据库连接
 * @author 12952
 *
 */
public class DbUtil {
//	public static void main(String[] args) {
//		DbUtil dbUtil = new DbUtil();
//		try {
//			dbUtil.getConn();
//			System.out.println("数据库已连接");
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}finally {
//			
//		}
//	}
	/**
	 * 获取数据库连接
	 * @return
	 * @throws Exception
	 */
	public Connection getConn() throws Exception{
		Class.forName(PropertiesUtil.getValue("jdbcName"));
		Connection con = DriverManager.getConnection(PropertiesUtil.getValue("dbUrl"),PropertiesUtil.getValue("dbUserName"),PropertiesUtil.getValue("dbPassword"));
		return con;
	}
	/**
	 * 关闭数据库
	 * @param con
	 * @throws Exception
	 */
	public void closeCon(Connection con) throws Exception{
		con.close();
	}
}
