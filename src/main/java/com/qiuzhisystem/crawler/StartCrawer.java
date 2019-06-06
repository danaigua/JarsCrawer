package com.qiuzhisystem.crawler;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.qiuzhisystem.utils.DbUtil;

/**
 * 爬虫起始类
 * @author 12952
 *
 */
public class StartCrawer {
	//初始化日志类
	private static Logger logger = Logger.getLogger(StartCrawer.class);
	//需要过滤的url
	public static String[] excludeUrl = new String[] {".pom", ".xml", ".md5", ".sha1", ".asc", ".gz", ".zip", "../"};
	public static Queue<String> waitForCrawlerUrls = new LinkedList<String>();//等待爬取url
	private static int total = 0;
	
	private static boolean exeFlag = true;
	
	private static Connection con = null;
 	/**
	 * 解析网页内容
	 * @param webPageContent
	 */
	public static void  parseWebPage(String webPageContent, String realPath) {
		if("".equals(webPageContent)) {
			return;
		}
		//Jsoup框架
		Document doc =  Jsoup.parse(webPageContent);
		Elements links = doc.select("a");//查找所有a标签
		//把所有的超链接封装成Elements对象
		for(int i = 0; i < links.size(); i++) {
			Element link = links.get(i);
			String url = link.attr("href");
			System.out.println("提取的url：" + (realPath + url));
			//过滤没用的url
			boolean f = true;
			for(int j = 0; j<excludeUrl.length; j++) {
				if(url.endsWith(excludeUrl[j])) {
					f = false;
					break;
				}
			}
			if(f) {
				//我们需要的url
				//分两种情况:1,目标地址 2，目录，要继续添加到的爬虫队列的
				if(url.endsWith(".jar")) {
					total++;
					logger.info("发现第"+ total +"个目标" + (realPath + url));
					String sql = "select * from t_jar where name=?";
					try {
						PreparedStatement pstmt = con.prepareStatement(sql);
						pstmt.setString(1, url);
						ResultSet rs = pstmt.executeQuery();
						if(rs.next()) {
							logger.info("["+url+"]数据库已存在");
							continue;
						}else {
							String sql2 = "insert into t_jar values(?, ?, ?, now(), ?, 0, 0, 0, 0)";
							PreparedStatement pstmt2 = con.prepareStatement(sql2);
							pstmt2.setString(1, UUID.randomUUID().toString());//uuid
							pstmt2.setString(2, url);//文件名称
							pstmt2.setString(3, (realPath + url));//路径
							if(url.endsWith("javadoc.jar")) {
								pstmt2.setString(4, "javadoc");
							}else if(url.endsWith("sources.jar")){
								pstmt2.setString(4, "source");
							}else {
								pstmt2.setString(4, "jar");
							}
							if(pstmt2.executeUpdate()==1) {
								logger.info("成功插入数据库");
							}else {
								logger.info("插入数据库失败");
							}
						}
					} catch (SQLException e) {
						logger.error("SQLException",e);
					}
				}else {
					logger.info("爬虫url队列新增url："+ (realPath + url));
					addUrl(realPath + url, "解析网页");
				}
			}
		}
		
		
	}
	//添加url到爬虫队列，假如队列中存在就不添加
	private static void addUrl(String url, String info) {
		if(url == null || "".equals(url)) {
			return;
		}
		if(!waitForCrawlerUrls.contains(url)) {
			waitForCrawlerUrls.add(url);//加入到爬虫队列里面
			logger.info("["+info+"]" + url + "添加到爬虫队列");
//			parseUrl();
		}
	}
	/**
	 * 解析网页请求
	 * @param url
	 */
	public static void parseUrl() {
//		定义一个线程池 	Executors  java内置线程池
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		while(exeFlag) {
			if(waitForCrawlerUrls.size() > 0) {
				executorService.execute(new Runnable() {
					public void run() {
						String url = waitForCrawlerUrls.poll();//摘取，先进先出
						if(url == null || "".equals(url)) {
							return;
						}
						logger.info("执行解析url：" + url);
						RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(100000)//设置读取时间
											  .setConnectTimeout(50000)//设置连接超时时间
											  .build();
						CloseableHttpClient httpClient = HttpClients.createDefault();//创建httpClient实例，可关闭
						//反射
						HttpGet httpGet = new HttpGet(url);//通过反射获取httpGet实例
						httpGet.setConfig(requestConfig);
						CloseableHttpResponse response = null;//可关闭的HttpResponse，节省资源
						try {
							response = httpClient.execute(httpGet);
						} catch (ClientProtocolException e) {
							logger.error("ClientProtocolException", e);
							//出现异常重新添加
							addUrl(url,"由于异常");
						} catch (IOException e) {
							logger.error("IOException", e);
							addUrl(url,"由于异常");
						}
						if(response != null) {
							HttpEntity entity = response.getEntity();//获取返回实体
							if("text/html".equals(entity.getContentType().getValue())) {//过滤掉其他不需要的软件
								String webPageContent = null;
								try {
									webPageContent = EntityUtils.toString(entity, "utf-8");
									logger.info("网页内容" + webPageContent);
									//提取url
									parseWebPage(webPageContent, url);
								} catch (IOException e) {
									logger.error("IOException", e);
									addUrl(url,"由于异常");
								}
							} 
							try {
								response.close();
							} catch (IOException e) {
								logger.error("IOException", e);
								addUrl(url,"由于异常");
							}
						}else {
							logger.info("连接超时");
							addUrl(url,"由于异常");
						}
					}
				});
			}else {
				//如果活动线程为0，说明所有线程都已经没有在工作，则关闭线程
				if(((ThreadPoolExecutor)executorService).getActiveCount() == 0) {
					executorService.shutdown();//释放所有的资源
					exeFlag=false;
					logger.info("爬虫任务完成");
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error("线程休眠报错",e);
			}
		}
	}
	
	private static void init() {
		DbUtil dbUtil = new DbUtil();
		try {
			con = dbUtil.getConn();
			logger.info("数据库连接成功");
		} catch (Exception e1) {
			e1.printStackTrace();
			logger.info("数据库连接失败");
		}
		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		logger.info("读取爬虫配置文件");
		try {
			String str = null;
			fis = new FileInputStream("c:\\crawler.txt");
			isr = new InputStreamReader(fis);
			//带缓存
			br = new BufferedReader(isr);
			while((str = br.readLine())!=null) {
				addUrl(str, "初始化");
			}
			
		} catch (FileNotFoundException e) {
			logger.error("FileNotFoundException", e);
		} catch (IOException e) {
			logger.error("IOException", e);
		}finally {
			try {
				//先打开的最后关
				br.close();
				isr.close();
				fis.close();
				
				
			} catch (IOException e) {
				logger.error("IOException", e);
			}
		}
		logger.info("完成读取爬虫配置文件");
		parseUrl();
	}
	
	
	public static void main(String[] args) {
		logger.info("开始执行爬虫任务");
		init();
	}

}
