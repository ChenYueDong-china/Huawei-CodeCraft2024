/*
 * Author: 假装是我
 * Date: 2024-04-18
 */

#pragma once
#include <vector>
#include <thread>
#include <mutex>
#include <queue>
#include <string>
#include <unordered_map>
#include "curl/curl.h"
#include <memory>
using std::string;
using std::queue;
using std::unordered_map;

// 背景线程类，负责在后台计算激光扫描的地图数据、最短路等
class BackgroundThread {
private:
    BackgroundThread() = default;   // 单例模式

public:

    void init();

    // 该函数用于始终多线程之间投递最新的雷达数据,以及获取最新的雷达数据
    // 如果一个已投递数据没有被另一个线程获取，那么会被第二次投递替换为更新的

    void exitThread();

    bool isWorking() const;

    static BackgroundThread &Instance();

    int sendQuestion(string question);

    int getAnswer(int id);

private:
    struct curl_slist *headers{};
    int questionId = 0;
    CURL *curl{};
    string response;
    struct Question {
        int id;
        string qus;
    };


    void threadMain();

    int sendRequest(string question);

    static size_t writeCallback(char *ptr, size_t size, size_t nmemb, std::string *data);


    queue<Question> input;
    unordered_map<int, int> output;

    bool m_exited{false};
    bool m_working{false};

    std::unique_ptr<std::thread> m_thread;
    std::mutex m_lock;

};
