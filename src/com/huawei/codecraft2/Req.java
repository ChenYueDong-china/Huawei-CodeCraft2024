package com.huawei.codecraft2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
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

    public static int sendRequest(String question) {
        String url = "";
        String appCode = "";
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
        httpPost.setHeader("X-Apig-AppCode", appCode);

        Map<String, Object> body = new HashMap<>();
        body.put("prompt", question);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonString = objectMapper.writeValueAsString(body);
            StringEntity entity = new StringEntity(jsonString, ContentType.create("application/json", StandardCharsets.UTF_8));
            httpPost.setEntity(entity);
            CloseableHttpClient client = HttpClients.custom().build();
            CloseableHttpResponse responseBody = client.execute(httpPost);
            Map responseMap = new ObjectMapper().readValue(EntityUtils.toString(responseBody.getEntity()), Map.class);
            String text = (String) responseMap.get("text");
            // add some additional practice here
            return 0;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

}
