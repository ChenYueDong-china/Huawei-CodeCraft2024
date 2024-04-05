package com.huawei.codecraft;

/**
 * 常量
 */
public class Constants {


    public static final int BOAT_PREDICT_DISTANCE = 20;//预测距离
    public static final int BOAT_AVOID_DISTANCE = 10;//避让距离

    //调调超参数，打不过的话
    public static final boolean GET_LAST_ONE = false;//是否拿倒数第一
    public static final double SAME_TARGET_REWARD_FACTOR = 5; //动态保险因子，防止老是切换目标工作台
    public static final double DISAPPEAR_REWARD_FACTOR = 1.5; //消失因子
    public static final BoatDecisionType boatDecisionType = BoatDecisionType.DECISION_ON_ORIGIN_BERTH;
    public static final boolean BOAT_NO_WAIT = false;//船最开始是否不等待

    public static final int FPS = 50;                   // 每秒运行帧数
    public static final int GAME_FRAME = 5 * 60 * FPS;
    public static final int WORKBENCH_EXIST_TIME = 20 * FPS;//1000帧

    public static final int MAP_FILE_ROW_NUMS = 200;    // 地图文件行数
    public static final int MAP_FILE_COL_NUMS = 200;    // 地图文件列数

    public static final int MAP_DISCRETE_WIDTH = 2 * MAP_FILE_COL_NUMS + 1;    // 离散列
    public static final int MAP_DISCRETE_HEIGHT = 2 * MAP_FILE_ROW_NUMS + 1;    // 离散行
    public static final int ROBOT_PRICE = 2000;    // 离散行
    public static final int BOAT_PRICE = 8000;    // 离散行
    public static final int BOAT_LENGTH = 3;    // 离散行
    public static final int BOAT_WIDTH = 2;    // 离散行


    public static int ROBOTS_PER_PLAYER = 0;    // 每个玩家的机器人数
    public static int MAX_ROBOTS_PER_PLAYER = 50;    // 每个玩家的泊位
    public static int MAX_BERTH_PER_PLAYER = 20;    // 每个玩家的泊位
    public static int MAX_BOATS_PER_PLAYER = 20;    // 每个玩家的泊位
    public static int BERTH_PER_PLAYER = 0;    // 每个玩家的泊位
    public static int BOATS_PER_PLAYER = 0;    // 每个玩家的船

    public static final int BERTH_HEIGHT = 4;    // 每个玩家的船
    public static final int BERTH_WIDTH = 4;    // 每个玩家的船
    public static int frameId;
    public static final int BERTH_CHANGE_TIME = 10 * FPS; //切换泊位需要时间


    public static int boatWaitTime = 0;


}
