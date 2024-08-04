#pragma once


//初始化超参数
const int SELL_POINT_MAX_SEARCH_DEEP = 800;    // 只搜800深度以内的点，
const int BERTH_MAX_BOAT_SEARCH_DEEP = 800;    // 只搜800深度以内的点，
const int PRECIOUS_WORKBENCH_MAX_SEARCH_DEEP = 300;    // 贵重物品搜远一点
const int COMMON_WORKBENCH_MAX_SEARCH_DEEP = 100;    //普通物品搜索深度
const int ROBOT_FOR_WORKBENCH_HEURISTIC_SIZE = 100;    //物品启发cs保存大小，不改
const int PRECIOUS_WORKBENCH_BOUNDARY = 1000;    // 珍贵物品边界，不改
const int PROFIT_ZOOM_FACTOR = 1000;    //不该，剪枝的
const int SIMPLE_ROBOT_PATH_LENGTH = 50;    // 机器人保存路径长度
const int SIMPLE_BOAT_PATH_LENGTH = 50;    // 船保存路径长度


//机器人超参数
static bool IS_ONLINE = true;   // 寻路最多多多少帧
const double SAME_TARGET_REWARD_FACTOR = 0.5;//机器人决策2
static bool OTHER_ROBOT_DAMPING = true;   // 决策1
static bool ONLY_BUY_PRECIOUS_WORKBENCH = false;   // 决策1参数
const double OTHER_ROBOT_ADD_TIME_FACTOR = 0.5;   // 决策1参数
const double ROBOT_ANTI_JITTER = 0.9;   // 决策1参数
const int ROBOT_DIRECTION_AVOID_OTHER_TRIGGER_THRESHOLD = 5;   //认为对面卡死阈值
const int ROBOT_AVOID_OTHER_TRIGGER_THRESHOLD = 2;   // 避让对面触发阈值
const int ROBOT_AVOID_OTHER_DURATION = 10;   // 让10帧，
const int ROBOT_AVOID_OTHER_DEEP = 6;   // 寻路最多加6深度


//船超参数
const double BOAT_CHANGE_FACTOR = 0.9;   //船决策1参数
const double BOAT_SAME_TARGET_FACTOR = 1;   //船决策2、3参数
const int BOAT_CHANGE_BERTH_MIN_NUM_DIFF = 2;   //传如果在泊位上，最低需要多少数量他才能开始奇幻泊位
const int BOAT_AVOID_OTHER_DEEP = 15;   //船避让其他人最多增加到深度
const int BOAT_FLASH_NO_MOVE_TIME = 20;   //20帧不动直接闪现
const int BOAT_AVOID_OTHER_TRIGGER_THRESHOLD = 3;   //对面超过3帧不动，我直接让，超过6帧他让不了，我也直接让
const int BOAT_AVOID_OTHER_DURATION = 10;   //持续避让帧数
const int BOAT_PREDICT_DISTANCE = 20;//预测距离
const int BOAT_AVOID_DISTANCE = 10;//避让距离
const int BOAT_FIND_PATH_DEEP = 2;//避让距离
const int BOAT_AVOID_CANDIDATE_SIZE = 25;//避让候选集


//购买超参数
const int INIT_ROBOT_TYPE0_COUNT = 7;//初始0号机器人个数
const int INIT_ROBOT_TYPE1_COUNT = 1;//初始1号机器人个数
const int INIT_BOAT_COUNT = 1;//初始船个数
const int MIN_ROBOT_TYPE0_COUNT = 7;//最小0号机器人个数
const int MIN_ROBOT_TYPE1_COUNT = 1;//最小1号机器人个数
const int MIN_BOAT_COUNT = 1;//最少船个数
const double BUY_ROBOT_FACTOR = 5;//越大越不容易买机器人,5-20左右好吧大概，太小机器人会贼多
const double BUY_BOAT_FACTOR = 5;//越大越不容易买船,5-20左右好吧大概
const int FUTURE_BUY_ROBOT_TYPE = 1;//未来买的机器人种类，最好1，因为控制船很慢，0或者1
const int FUTURE_BUY_BOAT_TYPE = 0;//未来买的机器人种类，最好1，因为控制船很慢，0或者1




static bool GET_LAST_ONE = false;//是否拿倒数第一

static bool BOAT_NO_WAIT = false;//船最开始是否不等待


const int FPS = 20;                   // 每秒运行帧数
const int GAME_FRAME = 5 * 200 * FPS;
const int WORKBENCH_EXIST_TIME = 1000;//1000帧
const int MAP_FILE_ROW_NUMS = 800;    // 地图文件行数
const int MAP_FILE_COL_NUMS = 800;    // 地图文件列数
const int MAP_DISCRETE_WIDTH = 2 * MAP_FILE_COL_NUMS + 1;    // 离散列
const int MAP_DISCRETE_HEIGHT = 2 * MAP_FILE_ROW_NUMS + 1;    // 离散行
const int ROBOT_PRICE = 2000;   // 机器人价格
const int ROBOT_TYPE1_PRICE = 3000;   // 机器人价格
const int BOAT_PRICE = 8000;    // 船价格
const int BOAT_TYPE1_PRICE = 20000;    // 船价格
const int BOAT_LENGTH = 3;    // 船长
const int BOAT_WIDTH = 2;    // 船宽

extern int ROBOTS_PER_PLAYER;    // 每个玩家的机器人数
extern int PENDING_ROBOT_NUMS;    // 每个玩家的机器人数
const int MAX_ROBOTS_PER_PLAYER = 500;    // 每个玩家的泊位
extern int BERTH_PER_PLAYER;    // 每个玩家的泊位
const int MAX_BERTH_PER_PLAYER = 100;    // 每个玩家的泊位
extern int BOATS_PER_PLAYER;    // 每个玩家的船
const int MAX_BOATS_PER_PLAYER = 200;    // 每个玩家的泊位

const int BERTH_HEIGHT = 4;    // 每个玩家的船
const int BERTH_WIDTH = 4;    // 每个玩家的船
const int BERTH_CHANGE_TIME = 10 * FPS;

const int INT_INF = 0x7f7f7f7f;
const short SHORT_INF = 0x7f7f;
const int DIR_LENGTH = 4;
extern int frameId;
extern int boatWaitTime;