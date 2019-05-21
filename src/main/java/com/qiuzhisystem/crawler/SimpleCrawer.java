package com.qiuzhisystem.crawler;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class SimpleCrawer {
	public static void main(String[] args) {
		String url = "https://www.baidu.com/";
		CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse closeableHttpResponse = null;
		try {
			closeableHttpResponse = closeableHttpClient.execute(httpGet);
			HttpEntity entity = closeableHttpResponse.getEntity();
			String webContent = EntityUtils.toString(entity, "utf-8");
			System.out.println(webContent);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
