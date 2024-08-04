
#include "Strategy.h"
#include "BackgroundThread.h"


using namespace std;
void testLLM() {
    BackgroundThread::Instance().init();
    int id1 = BackgroundThread::Instance().sendQuestion(
            "下列哪种材质常用于制作快递袋，具有防水和防撕裂的特点？ A. 塑料 B. 纸板 C. 布料 D. 金属");
    int id2 = BackgroundThread::Instance().sendQuestion(
            "下列哪种材质常用于快递包装中的填充物，具有轻便和环保的特点？ A. 塑料颗粒 B. 纸屑 C. 硬纸板 D. 木屑");
    int id3 = BackgroundThread::Instance().sendQuestion(
            "下列哪种材质常用于制作快递封箱胶带，具有良好的粘性和耐用性？ A. 透明胶带 B. 纸质胶带 C. 塑料胶带 D. 布质胶带");
    int id4 = BackgroundThread::Instance().sendQuestion(
            "使用GPS跟踪技术在电子产品长途运输中的主要目的是什么？ A. 控制环境温湿度 B. 实时跟踪货物位置 C. 减少运输成本 D. 自动装卸货物");
    int id5 = BackgroundThread::Instance().sendQuestion(
            "海上运输中，哪项措施不适用于防止货物受潮？ A. 使用密封集装箱 B. 在集装箱内放置干燥剂 C. 保持货物直接暴露在空气中 D. 使用防潮包装材料");
    std::this_thread::sleep_for(std::chrono::milliseconds(1));
    if (BackgroundThread::Instance().isWorking()) {
        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    }
    printError(to_string(BackgroundThread::Instance().getAnswer(id1)));
    printError(to_string(BackgroundThread::Instance().getAnswer(id2)));
    printError(to_string(BackgroundThread::Instance().getAnswer(id3)));
    printError(to_string(BackgroundThread::Instance().getAnswer(id4)));
    printError(to_string(BackgroundThread::Instance().getAnswer(id5)));
    BackgroundThread::Instance().exitThread();
}
//todPointWithDirection boatFlashMainChannelPoint[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]; 后面不能加花括号，不知道啥问题
//#include<windows.h>
int main() {
//    testLLM();
//    SetConsoleOutputCP ( CP_UTF8 ) ;
//    freopen("in.txt", "r", stdin);
    static Strategy strategy;
    strategy.init();
    strategy.mainLoop();
}

