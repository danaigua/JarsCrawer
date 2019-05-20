package com.qiuzhisystem.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
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
	public static int total = 0;
	
 	/**
	 * 解析网页内容
	 * @param webPageContent
	 */
	public static void parseWebPage(String webPageContent, String realPath) {
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
			parseUrl();
		}
	}
	/**
	 * 解析网页请求
	 * @param url
	 */
	public static void parseUrl() {
//		System.out.println("url:" + url);
		while(waitForCrawlerUrls.size() > 0) {
			String url = waitForCrawlerUrls.poll();//摘取，先进先出
			logger.info("执行解析url：" + url);
			CloseableHttpClient httpClient = HttpClients.createDefault();//创建httpClient实例，可关闭
			//反射
			HttpGet httpGet = new HttpGet(url);//通过反射获取httpGet实例
			CloseableHttpResponse response = null;//可关闭的HttpResponse，节省资源
			try {
				response = httpClient.execute(httpGet);
				HttpEntity entity = response.getEntity();//获取返回实体
				if("text/html".equals(entity.getContentType().getValue())) {//过滤掉其他不需要的软件
					String webPageContent = EntityUtils.toString(entity, "utf-8");
					System.out.println("网页内容" + webPageContent);
					//提取url
					parseWebPage(webPageContent, url);
				}
			} catch (ClientProtocolException e) {
				logger.error("ClientProtocolException", e);
				//出现异常重新添加
				addUrl(url,"由于异常");
			} catch (IOException e) {
				logger.error("IOException", e);
				addUrl(url,"由于异常");
			}finally {
				if(response != null) {
					try {
						response.close();
					} catch (IOException e) {
						logger.error("IOException", e);
					}
				}
				try {
					httpClient.close();
				} catch (IOException e) {
					logger.error("IOException", e);
				}
			}
			
			try {
				Thread.sleep(3000);//线程休息两秒
				System.out.println("线程休息三秒");
			} catch (InterruptedException e) {
				logger.error("InterruptedException", e);
			}
		}
	}
	
	private static void init() {
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
		addUrl("http://central.maven.org/maven2/HTTPClient/HTTPClient/", "初始化");
	}
	
	
	public static void main(String[] args) {
		logger.info("开始执行爬虫任务");
		init();
	}

}
