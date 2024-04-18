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
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static com.huawei.codecraft.Utils.*;

public class BackgroundThread {
    boolean m_exited = false;
    boolean m_working = false;

    static int questionId = 0;
    Thread m_thread;

    ReentrantLock m_lock = new ReentrantLock(false);
    private static final BackgroundThread obj = new BackgroundThread();

    private static CloseableHttpClient client;


    private BackgroundThread() {
    }

    public static BackgroundThread Instance() {
        return obj;
    }

    public int sendQuestion(String question) {
        Question q = new Question(questionId, question);
        m_lock.lock();
        input.add(q);
        m_lock.unlock();
        return questionId++;
    }

    public int getAnswer(int questionId) {
        assert questionId != -1;
        int ans = -1;
        m_lock.lock();
        if (output.containsKey(questionId)) {
            ans = output.get(questionId);
            output.remove(ans);
        }
        m_lock.unlock();
        return ans;
    }


    public static class Question {
        public int id;
        public String qus;

        public Question(int id, String qus) {
            this.id = id;
            this.qus = qus;
        }
    }


    public static Queue<Question> input = new ArrayDeque<>();
    public static HashMap<Integer, Integer> output = new HashMap<>();


    public void init() {
        RequestConfig.Builder requestBuilder = RequestConfig.custom()
                .setConnectTimeout(3000) // 连接超时3秒
                .setSocketTimeout(10000);  // 数据读取超时10秒
        client = HttpClients.custom().setDefaultRequestConfig(requestBuilder.build()).build();
        m_thread = new Thread(this::ThreadMain, "BackgroundThread");
        m_thread.start();
    }

    private void ThreadMain() {
        while (!m_exited) {
            boolean hasQuestion = true;
            m_lock.lock();
            if (input.isEmpty()) {
                hasQuestion = false;
            }
            m_lock.unlock();
            if (!hasQuestion) {
                // not update
                try {
                    //noinspection BusyWait
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            m_working = true;
            m_lock.lock();
            Question question = input.poll();
            m_lock.unlock();
            //询问问题
            assert question != null;
            int ans = 0;
            try {
                ans = sendRequest(question.qus);
            } catch (Exception e) {
                printError("error in sendRequest:" + e);
            }
            m_lock.lock();
            output.put(question.id, ans);
            m_lock.unlock();
            m_working = false;
        }
    }


    @SuppressWarnings("all")
    private static int sendRequest(String question) {
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
            Object object = responseMap.get("text");
            if (object == null) {
                printError("error in send message");
                return 0;
            }
            String text = (String) object;
            printDebug("question:" + question + ",LLMAns:" + text);
            // add some additional practice here
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == 'A' || text.charAt(i) == 'a') {
                    return 0;
                }
                if (text.charAt(i) == 'B' || text.charAt(i) == 'b') {
                    return 1;
                }
                if (text.charAt(i) == 'C' || text.charAt(i) == 'c') {
                    return 2;
                }
                if (text.charAt(i) == 'D' || text.charAt(i) == 'd') {
                    return 3;
                }
            }
        } catch (JsonProcessingException e) {
            printError(e.toString());
        } catch (IOException e) {
            printError(e.toString());
        }
        return 0;
    }


    void exitThread() throws InterruptedException, IOException {
        m_exited = true;
        if (m_thread != null) {
            m_thread.join();
        }
        client.close();
    }

    boolean IsWorking() {
        return m_working;
    }


}
