package com.qiuzhisystem.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 属性配置文件工具类
 * @author 12952
 *
 */
public class PropertiesUtil {
	/**
	 * 通过key获取value
	 * @param key
	 * @return
	 */
	public static String getValue(String key) {
		Properties prop = new Properties();
		InputStream in = new PropertiesUtil().getClass().getResourceAsStream("/crawler.properties");
		try {
			prop.load(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return prop.getProperty(key);
	}
}
