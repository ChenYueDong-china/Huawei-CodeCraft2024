package com.huawei.codecraft;

/**
 * 常量
 */
public class Constants {

    //机器人超参数
    public static double SAME_TARGET_REWARD_FACTOR = 5; //动态保险因子，防止老是切换目标工作台
    public static double DISAPPEAR_REWARD_FACTOR = 1.5; //消失因子


    //船只超参数
    //任意时刻可以切换泊位 or 到达泊位或者目标点或者在移动到目标点才能切泊位
    public static double BOAT_DYNAMIC_SAME_TARGET_FACTOR = 1;//有可能0更好
    public static int BOAT_PREDICT_DISTANCE = 20;//预测距离,待测试
    public static int BOAT_AVOID_DISTANCE = 10;//避让距离
    public static int BOAT_FIND_PATH_DEEP = 2;//避让距离
    public static int BOAT_AVOID_CANDIDATE_SIZE = 25;//避让候选集

    //购买超参数
    public static int INIT_ROBOT_COUNT = 8; //初始化
    public static int INIT_BOAT_COUNT = 1;
    public static int MIN_ROBOT_COUNT = 8; //最小的
    public static int MIN_BOAT_COUNT = 1;
    public static double BUY_ROBOT_FACTOR = 1;//动态的，0.5-1.5为好，越大越不容易买机器人
    public static double BUY_BOAT_FACTOR = 1;//动态的，0.5-1.5为好，越大越不容易买船,暂时不买船


    //调调超参数，打不过的话
    public static final boolean GET_LAST_ONE = false;//是否拿倒数第一


    public static final int SELL_POINT_MAX_SEARCH_DEEP = 800;    // 只搜800深度以内的点，一旦有某个方向到达800
    // ，其他三个方向最大为400，加快速度
    public static final int BERTH_MAX_ROBOT_SEARCH_DEEP = 1600;    // 只搜800深度以内的点，一旦有某个方向到达800
    public static final int BERTH_MAX_BOAT_SEARCH_DEEP = 800;    // 只搜800深度以内的点，一旦有某个方向到达800


    public static final int FPS = 50;                   // 每秒运行帧数
    public static final int GAME_FRAME = 5 * 60 * FPS;
    public static final int WORKBENCH_EXIST_TIME = 20 * FPS;//1000帧

    public static final int MAP_FILE_ROW_NUMS = 800;    // 地图文件行数
    public static final int MAP_FILE_COL_NUMS = 800;    // 地图文件列数

    public static final int MAP_DISCRETE_WIDTH = 2 * MAP_FILE_COL_NUMS + 1;    // 离散列
    public static final int MAP_DISCRETE_HEIGHT = 2 * MAP_FILE_ROW_NUMS + 1;    // 离散行
    public static final int ROBOT_PRICE = 2000;    // 离散行
    public static final int BOAT_PRICE = 8000;    // 离散行
    public static final int BOAT_LENGTH = 3;    // 离散行
    public static final int BOAT_WIDTH = 2;    // 离散行


    public static int MAX_ROBOTS_PER_PLAYER = 100;    // 每个玩家的泊位
    public static int MAX_BERTH_PER_PLAYER = 50;    // 每个玩家的泊位
    public static int MAX_BOATS_PER_PLAYER = 50;    // 每个玩家的泊位
    public static int ROBOTS_PER_PLAYER = 0;    // 每个玩家的机器人数
    public static int BERTH_PER_PLAYER = 0;    // 每个玩家的泊位
    public static int BOATS_PER_PLAYER = 0;    // 每个玩家的船

    public static final int BERTH_HEIGHT = 4;    // 每个玩家的船
    public static final int BERTH_WIDTH = 4;    // 每个玩家的船
    public static int frameId;
    public static boolean normal_init_success = true;//dij超时说明没有正常初始化
    public static final int BERTH_CHANGE_TIME = 10 * FPS; //切换泊位需要时间


    public static int boatWaitTime = 0;


}
