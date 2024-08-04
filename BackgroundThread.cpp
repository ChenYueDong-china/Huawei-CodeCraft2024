/*
 * Description: 背景线程类定义，背景线程负责在后台不停的询问大模型就行，反正比去年简单就对了
 *   主线程只需要查询即可，这样可避免主线程出现跳帧的情况。
 * Date: 2024-04-18
 */

#include "BackgroundThread.h"
#include "Utils.h"

using std::make_unique;
using std::thread;

void BackgroundThread::init() {
    const std::string API_URL = "https://infer-app-modelarts-cn-southwest-2.myhuaweicloud.com/v1/infers/760efa2e-3801-4844-aa02-1dc4511248db";
    const std::string CONTENT_TYPE = "Content-Type: application/json;charset=utf-8";
    const std::string AUTH_TOKEN = "X-Apig-AppCode:02073deccf6c4a6696f26d7f78c2d22a13168b61f77b45829f9d2404e32d9bfa";
    curl = curl_easy_init();
    // url
    curl_easy_setopt(curl, CURLOPT_URL, API_URL.c_str());
    // 请求方式
    curl_easy_setopt(curl, CURLOPT_POST, 1L);

    // 设置连接超时为5秒
    curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, 5L);

    // 设置读取超时为10秒
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 10L);

    // 头
    headers = curl_slist_append(headers, CONTENT_TYPE.c_str());
    headers = curl_slist_append(headers, AUTH_TOKEN.c_str());
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

    //指定证书
//    curl_easy_setopt(curl, CURLOPT_CAINFO, "./curl-ca-bundle.crt");//线上注释掉
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0);
    //取消验证也行，线上注释掉

    // 回调
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeCallback);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);


    //开线程
    m_thread = make_unique<thread>(&BackgroundThread::threadMain, this);
    m_thread->detach();//这样子进程才能死掉，不让ctrl+c怎么都杀不死进程，让他自己去检测结束
}


void BackgroundThread::threadMain() {
    long long int l = runTime();
    int lastFrameId = 0;
    int count = 0;
    while (!m_exited) {
        bool hasQuestion = true;
        m_lock.lock();
        if (input.empty()) {
            hasQuestion = false;
        }
        m_lock.unlock();
        if (!hasQuestion) {
            // not update
            long long int e = runTime();
            int curFrameId = frameId;
            if (curFrameId == lastFrameId) {
                count++;
            } else {
                count = 0;
            }
            lastFrameId = curFrameId;
            if ((e - l) > 1000 * 25 * 60 || count > 1000 * 60) {
                //超过25分钟结束，60秒没改变frameId结束
                m_exited = true;
                break;
            }
            std::this_thread::sleep_for(std::chrono::milliseconds(1));
            continue;
        }
        m_working = true;
        m_lock.lock();
        Question question = input.front();
        input.pop();
        m_lock.unlock();
        //询问问题
        int ans = sendRequest(question.qus);
        m_lock.lock();
        output[question.id] = ans;
        m_lock.unlock();
        m_working = false;
    }

}


BackgroundThread &BackgroundThread::Instance() {
    static BackgroundThread obj;
    return obj;
}

size_t BackgroundThread::writeCallback(char *ptr, size_t size, size_t nmemb, std::string *data) {
    data->append(ptr, size * nmemb);
    return size * nmemb;
}

int BackgroundThread::sendRequest(string question) {
    question += " Answer no more than one word";
    //实测topk比贪心更快
    string total = R"({"prompt":")" + question + R"(","top_k":1})";
//    string total ="{\"prompt\":\""  + question +"{\",\"temperature\":0}";
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, total.c_str());
    long long int l1 = runTime();
    CURLcode res = curl_easy_perform(curl);
    long long int l2 = runTime();
    // 检查错误
    if (res != CURLE_OK) {
        printError("error in send http" + to_string(res));
        return 0;
    }
    if (response.length() <= 9 + 2) {
        printError("error in response" + response);
        return 0;
    }
    string s = response.substr(9);
    response.clear();
    s = s.substr(0, s.length() - 2);
    printError(
            "frame:" + to_string(frameId) + ",question:" + question + ",LLMAns" + s + ",runTime:" + to_string(l2 - l1));
    for (char i: s) {
        if (i == 'A' || i == 'a') {
            return 0;
        }
        if (i == 'B' || i == 'b') {
            return 1;
        }
        if (i == 'C' || i == 'c') {
            return 2;
        }
        if (i == 'D' || i == 'd') {
            return 3;
        }
    }
    return 0;
}

void BackgroundThread::exitThread() {
    m_exited = true;
//    if (m_thread) {
//        m_thread->join();//会杀不死进程
//    }
    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
}

bool BackgroundThread::isWorking() const {
    return m_working;
}

int BackgroundThread::sendQuestion(string question) {
    Question q{questionId, std::move(question)};
    m_lock.lock();
    input.push(q);
    m_lock.unlock();
    return questionId++;
}

int BackgroundThread::getAnswer(int id) {
    assert (id != -1);
    int ans = -1;
    m_lock.lock();
    if (output.find(id) != output.end()) {
        ans = output[id];
        output.erase(ans);
    }
    m_lock.unlock();
    return ans;
}
