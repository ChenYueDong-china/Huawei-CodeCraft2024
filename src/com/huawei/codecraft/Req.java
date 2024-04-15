package com.huawei.codecraft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Req {

    public static CloseableHttpClient client;

    public static void init() {
        RequestConfig.Builder requestBuilder = RequestConfig.custom()
                .setConnectTimeout(3000) // 连接超时3秒
                .setSocketTimeout(5000);  // 数据读取超时5秒
        client = HttpClients.custom().setDefaultRequestConfig(requestBuilder.build()).build();
    }


    public static String sendRequest(String question) {
        String url = "https://infer-app-modelarts-cn-southwest-2.myhuaweicloud.com/v1/infers/760efa2e-3801-4844-aa02-1dc4511248db";
        String appCode = "02073deccf6c4a6696f26d7f78c2d22a13168b61f77b45829f9d2404e32d9bfa";
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
        httpPost.setHeader("X-Apig-AppCode", appCode);
        Map<String, Object> body = new HashMap<>();
        question += " Answer no more than one word";
        body.put("prompt", question);
        body.put("temperature", 0.0);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonString = objectMapper.writeValueAsString(body);
            StringEntity entity = new StringEntity(jsonString, ContentType.create("application/json", StandardCharsets.UTF_8));
            httpPost.setEntity(entity);
            CloseableHttpResponse responseBody = client.execute(httpPost);
            Map responseMap = new ObjectMapper().readValue(EntityUtils.toString(responseBody.getEntity()), Map.class);
            String text = (String) responseMap.get("text");
            // add some additional practice here
            return text;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

}
