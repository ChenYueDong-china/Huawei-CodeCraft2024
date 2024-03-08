package com.huawei.codecraft;

/**
 * 常量
 */
public class Constants {
    public static final int FPS = 50;                   // 每秒运行帧数
    public static final int GAME_FRAME = 5 * 60 * FPS;
    public static final int WORKBENCH_EXIST_TIME = 20 * FPS;//1000帧

    public static final int MAP_FILE_ROW_NUMS = 200;    // 地图文件行数
    public static final int MAP_FILE_COL_NUMS = 200;    // 地图文件列数

    public static final int MAP_DISCRETE_WIDTH = 2 * MAP_FILE_COL_NUMS + 1;    // 离散列
    public static final int MAP_DISCRETE_HEIGHT = 2 * MAP_FILE_ROW_NUMS + 1;    // 离散行

    public static final int ROBOTS_PER_PLAYER = 10;    // 每个玩家的机器人数
    public static final int BERTH_PER_PLAYER = 10;    // 每个玩家的泊位
    public static final int BOATS_PER_PLAYER = 5;    // 每个玩家的船

    public static final int BERTH_HEIGHT = 4;    // 每个玩家的船
    public static final int BERTH_WIDTH = 4;    // 每个玩家的船
    public static int frameId;

}
