#pragma once

#include <unordered_map>
#include <unordered_set>
#include <list>
#include <stack>
#include <queue>
#include <array>
#include "WorkBench.h"
#include "Berth.h"
#include "Boat.h"
#include "BoatUtils.h"
#include "BoatSellPoint.h"
#include "Robot.h"
#include "RobotUtils.h"
#include "RobotAction.h"
#include "SimpleBoat.h"
#include "SimpleRobot.h"
#include <random>

using std::unordered_set;

struct Strategy {
    std::default_random_engine e{666};
    //是否拿倒数第一参数
    int lastSelectRobotId = -1;
    int lastSelectWorkbenchId = -1;
    int lastSelectBerthId = -1;
    int lastValueThreshold = 1;
    bool lastRobotDoAction = false;

    int money;
    int robotMaxCount;
    long long int robotStartDecisionBuyTime;
    GameMap gameMap;
    vector<pair<int, int>> valueList;
    int workbenchId = 0;
    unordered_map<int, Workbench> workbenches;
    unordered_set<int> workbenchesLock;
    unordered_set<int> berthLock;
    unordered_set<int> workbenchesPermanentLock;
//    Robot robots[ROBOTS_PER_PLAYER];
    vector<Robot> robots;
    queue<Robot> pendingRobots;//想买没买到的
    unordered_set<int> robotLock[MAX_ROBOTS_PER_PLAYER];
//    Berth berths[BERTH_PER_PLAYER];
    vector<Berth> berths;
//    Boat boats[BOATS_PER_PLAYER];
    vector<Boat> boats;
    queue<Boat> pendingBoats;//想买没买到的

    vector<SimpleRobot> totalRobots;
    vector<SimpleBoat> totalBoats;

    int jumpCount;
    //boat的闪现位置，闪现泊位不用算
    vector<Point> boatFlashCandidates;
    PointWithDirection boatFlashMainChannelPoint[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];


    vector<Point> robotPurchasePoint;
    vector<int> robotPurchaseCount;
    unordered_set<int> robotLastFrameSelectIds;
    vector<Point> boatPurchasePoint;
    vector<BoatSellPoint> boatSellPoints;
    int boatCapacity;
    int boatCapacity1;

    int totalWorkbenchLoopDistance = 0;
    int totalValidWorkBenchCount = 0;
    int totalValidBerthCount = 0;
    double avgBerthLoopDistance = -1;//平均工作台循环距离
    double avgWorkBenchLoopDistance = -1;//平均货物循环距离
    double estimateMaxRobotCount = 0;//代码估计的
    double estimateMaxBoatCount = 0;

    int curFrameDiff = 1;
    bool isMap1 = false;
    bool isMap2 = false;
    bool isMap3 = false;


    vector<Point> robotsPredictPath[MAX_ROBOTS_PER_PLAYER];
    vector<Point> robotsAvoidOtherPoints[MAX_ROBOTS_PER_PLAYER];
    vector<Point> boatsAvoidOtherPoints[MAX_BOATS_PER_PLAYER];

    int totalValue;
    int goodAvgValue;

    int pullScore = 0;//总的卖的货物价值
    int totalPullGoodsValues = 0;//总的卖的货物价值
    int totalPullGoodsCount = 0;//总的卖的货物价值
    double avgPullGoodsValue = 150;//总的卖的货物价值
    double avgPullGoodsTime = 15;//10个泊位平均卖货时间

    void init();

    void mainLoop();

    PointWithDirection getBoatFlashDeptPoint(Point point);

private:
    struct BoatProfitAndCount {
        double profit;
        int count;
        int updateTime;
    };

    void dispatch();

    void boatDoAction();

    bool boatGreedyBuy1(int goodsNumList[MAX_BERTH_PER_PLAYER]);

    bool boatGreedyBuy2(int goodsNumList[MAX_BERTH_PER_PLAYER]);

    bool boatGreedyBuy3(int goodsNumList[MAX_BERTH_PER_PLAYER]);



    void
    decisionBoatBuy(Boat &boat, Berth &berth, BoatSellPoint &boatSellPoint, int goodsNumList[MAX_BERTH_PER_PLAYER]);

    void robotDoAction();


    bool robotGreedyBuy1();
    bool robotGreedyBuy2();


    bool robotGreedySell();

    void assignRobot(Workbench *workbench, Berth &berth, Robot &robot, RobotAction action);

    int getFastestCollectTime(int goodArriveTime, Berth &berth);

    bool input();

    static void finish();

    static bool readOK();


    int robotMinToWorkbenchDistance(Robot &robot, Workbench &workbench);


    void sortRobots(Robot **tmpRobots);

    void updateMap();

    void boatUpdateFlashPoint(int i, int j);

    void sortBoats(Boat *tmpBoats[]);

    bool boatGreedySell();

    int boatMinToBerthDistance(Boat &boat, Berth &berth);


    void robotAndBoatBuy();

    bool buyBoat(int type);

    void buyRobot(int type);

    int getBestBoatBuyPos();

    int getBestBuyRobotPos();

    static int boatMinBerthToBerthDistance(Berth &fromBerth, Berth &toBerth);

    int boatGetMinSellPointIndex(Berth &berth);

    vector<Point>
    robotToBerthHeuristic(Robot &robot, Berth &berth, const vector<vector<Point>> &otherPaths = {},
                          bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS] = nullptr);

    vector<Point>
    robotToWorkBenchHeuristic(Robot &robot, Workbench &workbench, const vector<vector<Point>> &otherPaths = {},
                              bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS] = nullptr);

    vector<PointWithDirection> boatToAnyPoint(Boat &boat, PointWithDirection target);

    vector<PointWithDirection>
    boatToSellPointHeuristic(Boat &boat, BoatSellPoint &boatSellPoint,
                             const vector<vector<PointWithDirection>> &otherPaths,
                             const vector<int> &otherIds, bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]);

    vector<PointWithDirection>
    boatToBerthHeuristic(Boat &boat, Berth &berth, const vector<vector<PointWithDirection>> &otherPaths,
                         const vector<int> &otherIds,
                         bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]);
};
