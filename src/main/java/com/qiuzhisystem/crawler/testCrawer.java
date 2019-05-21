package com.qiuzhisystem.crawler;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class testCrawer {
	public static void main(String[] args) {
		String url = "https://www.iqiyi.com/";
		CloseableHttpClient httpClient = HttpClients.createDefault();
		CloseableHttpResponse httpResponse = null;
		HttpEntity entity = null;
		HttpGet httpGet = new HttpGet(url);//request
		try {
			httpResponse = httpClient.execute(httpGet);
			entity = httpResponse.getEntity();
			String webContent = EntityUtils.toString(entity, "utf-8");
			System.out.println(webContent);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
