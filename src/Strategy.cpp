#include "Strategy.h"
#include "BackgroundThread.h"
#include <algorithm>

using std::max;
using std::sort;

void Strategy::init() {
    long long int startTime = runTime();
    char mapData[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
    for (auto &data: mapData) {
        fgets(data, MAP_FILE_COL_NUMS + 10, stdin);
        printMost(string(data));
        char OK[256];
        fgets(OK, MAP_FILE_COL_NUMS + 10, stdin);
        printMost(OK);
        finish();
    }
    long long int l0 = runTime();
    int boatSellCount = 0;
    for (auto &i: mapData) {
        for (char j: i) {
            if (j == 'T') {
                boatSellCount++;
            }
        }
    }
    boatSellPoints.reserve(boatSellCount);
    for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
        for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
            if (mapData[i][j] == 'R') {
                robotPurchasePoint.emplace_back(i, j);
                robotPurchaseCount.emplace_back(0);
            } else if (mapData[i][j] == 'S') {
                boatPurchasePoint.emplace_back(i, j);
            } else if (mapData[i][j] == 'T') {
                boatSellPoints.emplace_back(Point(i, j));
            }
        }
    }
    long long int l1 = runTime();
    gameMap.setMap(mapData);
    BackgroundThread::Instance().init();
    int questionId = BackgroundThread::Instance().sendQuestion(
            "下列哪种材质常用于快递包装中的填充物，具有轻便和环保的特点？ A. 塑料颗粒 B. 纸屑 C. 硬纸板 D. 木屑");

    for (int i = 0; i < boatSellPoints.size(); i++) {
        boatSellPoints[i].init(i, gameMap);
    }
    long long int l2 = runTime();
//    printError("boatRunningTime:" + to_string(l2 - l1) + " ," + to_string(l1 - l0));
    BERTH_PER_PLAYER = getIntInput();
    //码头

    berths.resize(BERTH_PER_PLAYER);
    for (Berth &berth: berths) {
        scanf("%d %d %d %d", &berth.id, &berth.corePoint.x, &berth.corePoint.y,
              &berth.loadingSpeed);
        printMost(to_string(berth.id) + " "
                  + to_string(berth.corePoint.x) + " "
                  + to_string(berth.corePoint.y) + " " +
                  to_string(berth.loadingSpeed));
        berth.init(gameMap);
        int minSellDistance = SHORT_INF;
        for (BoatSellPoint &boatSellPoint: boatSellPoints) {
            if (boatSellPoint.getMinDistance(berth.corePoint, berth.coreDirection) < minSellDistance) {
                minSellDistance = boatSellPoint.getMinDistance(berth.corePoint, berth.coreDirection);
                berth.minSellPointId = boatSellPoint.id;
            }
        }
        berth.minSellDistance = minSellDistance;
    }
    scanf("%d", &boatCapacity);
    scanf("%d", &boatCapacity1);
    printMost(to_string(boatCapacity) + " " + to_string(boatCapacity1));
//    boatCapacity = getIntInput();
    //闪现点
//    boatFlashCandidates.clear();
//    for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
//        for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
//            for (int k = 0; k < (sizeof DIR / sizeof DIR[0]) / 2; k++) {
//                if (gameMap.boatIsAllInMainChannel(Point(i, j), k)) {
//                    boatFlashCandidates.emplace_back(i, j);
//                    break;
//                }
//            }
//        }
//    }
//    for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
//        for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
//            if (gameMap.boatCanReach(i, j)) {
//                boatUpdateFlashPoint(i, j);
//            } else {
//                boatFlashMainChannelPoint[i][j] = {{-1, -1}, -1};
//            }
//        }
//    }


    //更新berth循环距离
    int totalBerthLoopDistance = 0;
    for (Berth &berth: berths) {
        if (berth.minSellDistance != SHORT_INF) {
            totalBerthLoopDistance += 2 * berth.minSellDistance + (int) ceil(1.0 * boatCapacity / berth.loadingSpeed);
            totalValidBerthCount++;
        }
    }
    avgBerthLoopDistance = 1.0 * totalBerthLoopDistance / max(1, totalValidBerthCount);
    int answer = BackgroundThread::Instance().getAnswer(questionId);
    workbenches.reserve(1000);
    readOK();
    finish();
    long long int endTime = runTime();
    printError("initTime:" + to_string(endTime - startTime) + "," +
               to_string(answer));
}

void Strategy::mainLoop() {

    while (input()) {
        //todo 测试多机器人避让，测试多船避让，测试买船，测试最优的决策
        long long l1 = runTime();
        dispatch();
        long long l2 = runTime();
        if (l2 - l1 > 10) {
            printError("frameId:" + to_string(frameId) + ",time:" + to_string(l2 - l1));
        }
        //先抢最低的船id
        finish();
        if (frameId == 1) {
            for (auto &workbenchPair: workbenches) {
                Workbench &workbench = workbenchPair.second;
                workbench.updateDij(gameMap);
            }
        }
    }

    int sellCount = 0;
    for (const auto &item: boats) {
        sellCount += item.sellCount;
    }
    printError(
            "jumpTime:" + to_string(jumpCount) + ",buyRobotCount:" + to_string(robots.size()) + ",buyBoatCount:"
            + to_string(boats.size()) + ",totalValue:"
            + to_string(totalValue) + ",pullValue:"
            + to_string(pullScore) + ",score:" + to_string(money) + ",boatSellCount:" + to_string(sellCount));

    BackgroundThread::Instance().exitThread();
}

void Strategy::finish() {
    puts("OK");
    fflush(stdout);
}

bool Strategy::input() {

    int tmp = frameId;
    int ret = scanf("%d %d", &frameId, &money);
    if (ret == EOF)return false;
    printMost(to_string(frameId) + " " + to_string(money));
    curFrameDiff = frameId - tmp;
    if (tmp + 1 != frameId) {
        jumpCount += frameId - tmp - 1;
        printError("jumpStartFrame:" + to_string(tmp) + ",jumpEndFrame:"
                   + to_string(frameId) + ",jumpCount:" + to_string(frameId - tmp - 1));
    }
    for (auto &berth: berths) {
        berth.lastBoatGlobalId = berth.curBoatGlobalId;
    }

    //新增工作台
    int num = getIntInput();
    vector<int> deleteIds;//满足为0的删除
    vector<Point> deletePos;//满足为0的删除
    vector<int> deleteValue;//满足为0的删除
    long long int l1 = runTime();
    for (auto &entry: workbenches) {
        Workbench &workbench = entry.second;
        workbench.remainTime -= curFrameDiff;//跳帧也要减过去
        if (workbench.remainTime <= 0) {
            assert(gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] == workbench.id);
            deleteIds.push_back(gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]);
            deletePos.push_back(workbench.pos);
            deleteValue.push_back(workbenches[gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]].value);
            gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] = -1;//说明删除了
        }
    }

    for (int i = 1; i <= num; i++) {
        static Workbench workbench(workbenchId);
        workbench.id = workbenchId;
        scanf("%d %d %d", &workbench.pos.x, &workbench.pos.y, &workbench.value);
        printMost(to_string(workbench.pos.x) + " " + to_string(workbench.pos.y) + " " + to_string(workbench.value));
        if (workbench.value == 0) {
            //被别人拿了，或者消失
            if (gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] != -1) {
                deleteIds.push_back(gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]);
                deletePos.push_back(workbench.pos);
                deleteValue.push_back(workbenches[gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]].value);
            }
            gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] = -1;
            //回收
            continue;
        }
        workbench.remainTime = workbench.value > PRECIOUS_WORKBENCH_BOUNDARY ? GAME_FRAME : WORKBENCH_EXIST_TIME;
        //最小卖距离
        int minDistance = SHORT_INF;
        for (Berth &berth: berths) {
            if (berth.getRobotMinDistance(workbench.pos) < minDistance) {
                minDistance = berth.getRobotMinDistance(workbench.pos);
                workbench.minSellBerthId = berth.id;
                workbench.minSellDistance = minDistance;
            }
        }
        if (minDistance == SHORT_INF) {
            if (gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] != -1) {
                deleteIds.push_back(gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]);
                deletePos.push_back(workbench.pos);
                deleteValue.push_back(workbenches[gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]].value);
            }//认为没有物品
            gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] = -1;
            workbenchId++;//还是给他一个id，但是被我过滤掉了
            totalValue += workbench.value;
            continue;//过滤掉不可达的
        }
        if (frameId != 1) {
            workbench.updateDij(gameMap);
        }
        totalValidWorkBenchCount++;
        totalWorkbenchLoopDistance += 2 * workbench.minSellDistance;//买了卖
        totalValue += workbench.value;
        workbenches.emplace(workbenchId, workbench);
        //掉帧导致的消失
        if (gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] != -1) {
            deleteIds.push_back(gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]);
            deletePos.push_back(workbench.pos);
            deleteValue.push_back(workbenches[gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]].value);
        }
        gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] = workbenchId;
        workbenchId++;
        goodAvgValue = totalValue / workbenchId;
    }
    avgWorkBenchLoopDistance = 1.0 * (double) totalWorkbenchLoopDistance / (double) max(1, totalValidWorkBenchCount);
    for (int id: deleteIds) {
        workbenches.erase(id);
    }
    long long int l2 = runTime();
    printDebug("frame:" + to_string(frameId) + ",inputDijTime:" + to_string(l2 - l1));

    workbenchesLock.clear();//动态需要解锁
    //场上机器人
    int totalRobotsCount = getIntInput();
    while (totalRobots.size() < totalRobotsCount) {
        totalRobots.emplace_back();
    }
    for (int i = 0; i < totalRobotsCount; i++) {
        SimpleRobot &simpleRobot = totalRobots[i];
        simpleRobot.input();
        //5帧不动且是贵重物品
        if (simpleRobot.noMoveTime >= ROBOT_DIRECTION_AVOID_OTHER_TRIGGER_THRESHOLD
            && !simpleRobot.belongToMe && simpleRobot.num < simpleRobot.maxNum) {
            //5帧不动，很可能对面进入了pending状态，此时自己暂时放弃这个工作台
            int id = gameMap.curWorkbenchId[simpleRobot.p.x][simpleRobot.p.y];
            if (id != -1 && workbenches[id].value > PRECIOUS_WORKBENCH_BOUNDARY) {
                //对面机器人在处于检索问答环节，自己先走了,对面一直不动我也没法走
                workbenchesLock.emplace(id);
            }
        }
        //20帧不动且有物品，说明对面应该是卡死了，这个位置所有工作台锁死
        if (simpleRobot.noMoveTime > FPS * 2 && !simpleRobot.belongToMe) {
            //5帧不动，很可能对面进入了pending状态，此时自己暂时放弃这个工作台，对面超过100帧不动,选择这个目标的我直接换
            int id = gameMap.curWorkbenchId[simpleRobot.p.x][simpleRobot.p.y];
            if (id != -1) {
                //对面机器人在处于检索问答环节，自己先走了,对面一直不动我也没法走
                workbenchesLock.emplace(id);//暂时锁死这个工作台，先走
            }
        }
        if (simpleRobot.num == 2) {
            simpleRobot.type = 1;
            simpleRobot.maxNum = 2;
        }
        if (simpleRobot.num != simpleRobot.lastNum) {
            Point lastP = simpleRobot.lastP;
            Point curP = simpleRobot.p;
            if (simpleRobot.num < simpleRobot.lastNum && !simpleRobot.goodList.empty()) {
                //卸货
                while (!simpleRobot.goodList.empty()) {
                    int value = simpleRobot.goodList.back();
                    simpleRobot.goodList.pop_back();
                    int berthId = gameMap.getBelongToBerthId(lastP);
                    if (berthId == -1) {
                        berthId = gameMap.getBelongToBerthId(curP);
                    }
                    if (berthId == -1) {
                        short minDistance = SHORT_INF;
                        for (Berth &berth: berths) {
                            if (berth.getRobotMinDistance(simpleRobot.p) < minDistance) {
                                berthId = berth.id;
                            }
                        }
                    }
                    if (berthId == -1) {
                        printError("error no find berth");
                        continue;//不卸货了
                    }
                    if (simpleRobot.belongToMe) {
                        totalPullGoodsCount++;
                        totalPullGoodsValues += value;
                        avgPullGoodsValue = 1.0 * totalPullGoodsValues / totalPullGoodsCount;
                        pullScore += value;
                    }
                    berths[berthId].goods.push(value);
                    berths[berthId].goodsNums++;
                    berths[berthId].totalGoodsNums++;
                }
                if (simpleRobot.num > 0) {
                    simpleRobot.goodList.push_back(50);
                }
            } else {
                //装货
                //移动前拿的货，or 移动后拿的货
                bool find = false;
                for (int j = 0; j < deletePos.size(); j++) {
                    Point point = deletePos[j];
                    //至少应该有一个点能匹配上
                    if (point == lastP || point == curP) {
                        simpleRobot.goodList.push_back(deleteValue[j]);
                        find = true;
                        break;
                    }
                }
                if (!find) {
                    //假设拿了50的价值
                    simpleRobot.goodList.push_back(50);
                }
                if (simpleRobot.goodList.size() < simpleRobot.num) {
                    simpleRobot.goodList.push_back(50);
                }
            }

        }

        //强制一样
        if (simpleRobot.goodList.size() != simpleRobot.num) {
            printError("error list count no equal num");
            if (simpleRobot.goodList.size() < simpleRobot.num) {
                int load = simpleRobot.num - int(simpleRobot.goodList.size());
                for (int j = 0; j < load; j++) {
                    simpleRobot.goodList.push_back(50);
                }
            } else {
                int deLoad = int(simpleRobot.goodList.size()) - simpleRobot.num;
                for (int j = 0; j < deLoad; j++) {
                    simpleRobot.goodList.pop_back();
                }
            }
        }
    }
    //场上船
    int totalBoatsCount = getIntInput();
    while (totalBoats.size() < totalBoatsCount) {
        totalBoats.emplace_back();
    }
    for (int i = 0; i < totalBoatsCount; i++) {
        SimpleBoat &simpleBoat = totalBoats[i];
        simpleBoat.input();
        if (simpleBoat.num > boatCapacity) {
            simpleBoat.type == 1;
            simpleBoat.capacity = boatCapacity1;
        }
        if (simpleBoat.type == 0) {
            simpleBoat.capacity = boatCapacity;
        }
        //解锁泊位
        for (Berth &berth: berths) {
            if (simpleBoat.status != 0 &&
                simpleBoat.corePoint == berth.corePoint
                && simpleBoat.direction == berth.coreDirection
                && berth.curBoatGlobalId == -1) {
                //此时我不能发berth指令，因为会导致慢,有人了，则不管
                berth.curBoatGlobalId = simpleBoat.id;
            }
            if (berth.curBoatGlobalId == simpleBoat.id) {
                if (!(simpleBoat.corePoint == berth.corePoint
                      && //不在泊位上，一定是离开了泊位
                      simpleBoat.direction == berth.coreDirection)) {
                    berth.curBoatGlobalId = -1;
                }
                //行驶状态也没到达泊位
                if (simpleBoat.status == 0) {
                    berth.curBoatGlobalId = -1;
                }
                if (simpleBoat.status != 2 && simpleBoat.lastStatus == 2) {
                    berth.curBoatGlobalId = -1;
                }
            }
        }
        for (SimpleBoat &boat: totalBoats) {
            //进入装货状态的一定是到达泊位的
            if (boat.status == 2) {
                int berthId = gameMap.getBelongToBerthId(boat.corePoint);
                assert (berthId != -1);
                berths[berthId].curBoatGlobalId = boat.id;//一定进入泊位
            }
        }

        if (simpleBoat.lastNum < simpleBoat.num) {
            //在装货
            int berthId = gameMap.getBelongToBerthId(simpleBoat.corePoint);
            if (berthId == -1) {
                short minDistance = SHORT_INF;
                for (Berth &berth: berths) {
                    if (berth.getBoatMinDistance(simpleBoat.corePoint, simpleBoat.direction) < minDistance) {
                        minDistance = berth.getBoatMinDistance(simpleBoat.corePoint, simpleBoat.direction);
                        berthId = berth.id;
                    }
                }
            }
            if (berthId == -1) {
                printError("error in boat load");
                continue;//不装货了
            }

            int load = simpleBoat.num - simpleBoat.lastNum;
            Berth &berth = berths[berthId];
            assert(berth.goodsNums == berth.goods.size());
            load = min(berth.goodsNums, load);
            berth.goodsNums -= load;
            for (int j = 0; j < load; j++) {
                assert(!berth.goods.empty());
                int poll = berth.goods.front();
                berth.goods.pop();
                simpleBoat.value += poll;
            }
        }
    }
    for (SimpleBoat &boat: totalBoats) {
        //进入装货状态的一定是到达泊位的
        if (boat.status == 2) {
            int berthId = gameMap.getBelongToBerthId(boat.corePoint);
            assert (berthId != -1);
            berths[berthId].curBoatGlobalId = boat.id;//一定进入泊位
        }
    }

    //我方机器人
    ROBOTS_PER_PLAYER = getIntInput();
    if (robots.size() < ROBOTS_PER_PLAYER) {
        int size = int(robots.size());
        for (int i = size; i < ROBOTS_PER_PLAYER; i++) {
            if (pendingRobots.empty()) {
                printError("error no pending robots");
                exit(-1);
            }
            robots.push_back(pendingRobots.front());
            pendingRobots.pop();
        }
    }
    //机器人
    for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
        int id;
        scanf("%d", &id);
        totalRobots[id].belongToMe = true;
        Robot &robot = robots[i];
        robot.id = i;
        robot.globalId = id;
        robot.input(totalRobots[id]);
    }
    PENDING_ROBOT_NUMS = getIntInput();
    unordered_set<int> pendingIds;
    for (int i = 0; i < PENDING_ROBOT_NUMS; i++) {
        int id;
        scanf("%d", &id);
        std::string question;
        char str_temp[1024];//中文3字符，至少500
        fgets(str_temp, 1024 + 10, stdin);
        question += str_temp;
        question.pop_back();//去掉换行符
        Robot &robot = robots[id];
        if (!robot.isPending) {
            //上一帧不处于答题状态，则发送题目给背景线程回答
            robot.questionId = BackgroundThread::Instance().sendQuestion(question);
            robot.question = question;
            robot.ans = -1;
        } else {
            //上一帧处于答题状态，检查是否有答案了;
            robot.ans = BackgroundThread::Instance().getAnswer(robot.questionId);
        }
        pendingIds.emplace(robot.id);
        robot.isPending = true;
    }
    if (robots.size() > ROBOTS_PER_PLAYER) {
        int size = int(robots.size());
        for (int i = ROBOTS_PER_PLAYER; i < size; i++) {
            pendingRobots.push(robots[i]);
        }
        for (int i = ROBOTS_PER_PLAYER; i < size; i++) {
            robots.pop_back();
        }
        printError("error,no buy robot");
    }
    for (Robot &robot: robots) {//回答完毕的重置为false
        if (pendingIds.find(robot.id) == pendingIds.end() && robot.isPending) {
            //回答完毕，锁死
            workbenchesPermanentLock.insert(robot.targetWorkBenchId);
            robot.isPending = false;
        }
    }


    BOATS_PER_PLAYER = getIntInput();
    if (boats.size() < BOATS_PER_PLAYER) {
        int size = int(boats.size());
        for (int i = size; i < BOATS_PER_PLAYER; i++) {
            if (pendingBoats.empty()) {
                printError("error no pending boats");
                exit(-1);
            }
            boats.push_back(pendingBoats.front());
            pendingBoats.pop();
        }
    }
    //船
    for (int i = 0; i < BOATS_PER_PLAYER; i++) {
        int id;
        scanf("%d", &id);
        totalBoats[id].belongToMe = true;
        Boat &boat = boats[i];
        boat.id = i;
        boat.globalId = id;
        totalBoats[id].localId = i;
        boat.input(totalBoats[id]);
    }
    while (boats.size() > BOATS_PER_PLAYER) {
        boats.pop_back();
        printError("error,no buy boats");
    }
    berthLock.clear();
    for (auto &berth: berths) {
        if (berth.lastBoatGlobalId == berth.curBoatGlobalId) {
            berth.curBoatIdNoChangeTime++;
        } else {
            berth.curBoatIdNoChangeTime = 0;
        }
        if (berth.curBoatGlobalId != -1 && berth.curBoatIdNoChangeTime >= FPS * 10 &&
            berth.goodsNums * 2 > boatCapacity) {
            //占了两百帧还不懂，且此时还多了一半的货物
            berthLock.emplace(berth.id);//锁死泊位
        }
    }

    readOK();

    return true;
}


void Strategy::dispatch() {

    robotDoAction();
    boatDoAction();
    long long int l1 = runTime();
    robotAndBoatBuy();
    long long int l2 = runTime();
    printError("frame:" + to_string(frameId) + ",buyTime:" + to_string(l2 - l1));


//地图一测试避让寻路
//        if(frameId==1){
//            for (int i = 0; i < 1; i++) {
//                printf("lboat %d %d\n", boatPurchasePoint[0].x, boatPurchasePoint[0].y);
//                boats.emplace_back(this, boatCapacity);
//                printf("lboat %d %d\n", boatPurchasePoint[1].x, boatPurchasePoint[1].y);
//                boats.emplace_back(this, boatCapacity);
//            }
//        }
//        if (frameId > 1 && frameId < 500) {
//            Boat &boat1 = boats[0];
//            boat1.path = boatToPoint(boat1, {{111, 98}, -1}, 9999);
//            Boat &boat2 = boats[1];
//            boat2.path = boatToPoint(boat2, {{49, 120}, -1}, 9999);
//            //不然肯定dij不对
//            vector<vector<PointWithDirection>> otherPaths;
//            vector<int> otherIds;
//            otherPaths.push_back(boat1.path);
//            otherIds.push_back(0);
//            if (BoatUtils::boatCheckCrash(gameMap, boat2.id, boat2.path, otherPaths, otherIds, INT_INF) != -1) {
//                //撞了
//                boat2.path = boatToPoint(boat2, {{49, 120}, -1}, 9999, otherPaths, otherIds);
//
//            }
//            boat1.finish();
//            boat2.finish();

}

bool Strategy::readOK() {
    char OK[256];
    scanf("%s", OK);
    printMost(OK);
    if (strcmp(OK, "OK") != 0) {
        fprintf(stderr, "check OK err: %s\n", OK);
        abort();
    }
    return true;
}

void Strategy::boatDoAction() {
    //船只选择回家
    long long int l1 = runTime();
    while (true) {
        if (!boatGreedySell()) {
            break;
        }  //决策
    }
    //船只贪心去买卖，跟机器人做一样的决策
    //先保存一下泊位的价值列表
    //1当前价值列表+机器人运过来的未来价值列表

    int goodsNumList[BERTH_PER_PLAYER];//货物开始来泊位的最早时间
    for (int i = 0; i < BERTH_PER_PLAYER; i++) {
        goodsNumList[i] = berths[i].goodsNums;
    }
    for (const auto &berth: berths) {
        //别人占领的
        if (berth.curBoatGlobalId != -1) {
            SimpleBoat &boat = totalBoats[berth.curBoatGlobalId];
            if (boat.belongToMe) {
                continue;
            }
            int needCount = max(0, boat.capacity - boat.num);
            goodsNumList[berth.id] -= needCount;
        }
    }
    for (Robot &robot: robots) {
        if (!robot.assigned) {
            continue;
        }
        if (robot.targetBerthId != -1) {
            goodsNumList[robot.targetBerthId]++;
        }
    }


    //贪心切换找一个泊位
    while (true) {
        if (!boatGreedyBuy2(goodsNumList)) {
            break;
        }  //决策
    }

//移动
    //选择路径，碰撞避免
    Boat *tmpBoats[BOATS_PER_PLAYER];
    for (int i = 0; i < BOATS_PER_PLAYER; i++) {
        tmpBoats[i] = &boats[i];
    }
    sortBoats(tmpBoats);
    //1.选择路径,和修复路径
    vector<vector<PointWithDirection>> otherPaths;
    long long int l2 = runTime();
    vector<int> otherIds;
    otherPaths.reserve(BOATS_PER_PLAYER);
    otherIds.reserve(BOATS_PER_PLAYER);
    long long int startTime = runTime();
    for (Boat *tmpBoat: tmpBoats) {
        Boat &boat = *tmpBoat;
        if (!boat.assigned) {
            continue;
        }
        //后面可以考虑启发式搜，目前强搜
        if (boat.carry) {
            //卖
            boat.path = boatSellPoints[boat.targetSellId].boatMoveFrom(boat.corePoint,
                                                                       boat.direction, boat.remainRecoveryTime);
        } else {
            //买
            boat.path = berths[boat.targetBerthId].boatMoveFrom(boat.corePoint,
                                                                boat.direction, boat.remainRecoveryTime, false);
            assert(!boat.path.empty());
            //目标有船，且自己里目标就差5帧了，此时直接搜到目标，后面再闪现
            Berth &berth = berths[boat.targetBerthId];
            //目标有船，且自己里目标就差5帧了，此时直接搜到目标，后面再闪现
            if (berth.curBoatGlobalId != -1 &&
                berth.curBoatGlobalId != boat.globalId) {
                for (int i = 0; i < min((int) boat.path.size(), 5); i++) {
                    PointWithDirection next = boat.path[i];
                    if (next.point == berth.corePoint && boat.corePoint != berth.corePoint) {
                        //有船且不是你,先去核心点等着闪现
                        vector<PointWithDirection> toCorePath = boatToAnyPoint(boat,
                                                                               PointWithDirection(berth.corePoint, -1));
                        if (!toCorePath.empty()) {
                            boat.path = std::move(toCorePath);
                        }
                        break;
                    }
                }
            }
        }


        //2.避让队友
        //检查是否与前面机器人相撞，如果是，则重新搜一条到目标点的路径，极端情况，去到物品消失不考虑
        if (BoatUtils::boatCheckCrash(gameMap, boat.id, boat.path, otherPaths, otherIds, INT_INF) != -1) {
            //100帧之内撞才开始寻路
            vector<PointWithDirection> avoidPath;
            if (boat.carry) {
                //to sellPoint
                avoidPath = boatToSellPointHeuristic(boat, boatSellPoints[boat.targetSellId], otherPaths, otherIds,
                                                     nullptr);
            } else {
                //to berth
                avoidPath = boatToBerthHeuristic(boat, berths[boat.targetBerthId], otherPaths, otherIds, nullptr);
            }
            if (!avoidPath.empty()) {
                boat.path = std::move(avoidPath);
            }
        }


        //3.避让对方
        if (boat.avoidOtherTime > 0) {
            vector<Point> avoidOtherPoints = boatsAvoidOtherPoints[boat.id];
            if (avoidOtherPoints.empty()) {
                printError("error in avoid OtherTime");
            }
            for (const Point &avoidPoint: avoidOtherPoints) {
                gameMap.commonConflictPoints[avoidPoint.x][avoidPoint.y] = true;
            }
            vector<PointWithDirection> avoidPath;
            if (boat.carry) {
                //to sellPoint
                avoidPath = boatToSellPointHeuristic(boat, boatSellPoints[boat.targetSellId], otherPaths, otherIds,
                                                     gameMap.commonConflictPoints);
            } else {
                //to berth
                avoidPath = boatToBerthHeuristic(boat, berths[boat.targetBerthId], otherPaths, otherIds,
                                                 gameMap.commonConflictPoints);
            }
            if (!avoidPath.empty()) {
                boat.path = std::move(avoidPath);
            }
            for (const Point &avoidPoint: avoidOtherPoints) {
                gameMap.commonConflictPoints[avoidPoint.x][avoidPoint.y] = false;
            }
        }

        long long int endTime = runTime();
        if (IS_ONLINE && (endTime - startTime >= 4)) {
            //避让队友耗时
            otherPaths.clear();
            otherIds.clear();
            continue;
        }
        otherPaths.push_back(boat.path);
        otherIds.push_back(boat.id);
    }

    long long int l3 = runTime();
    otherPaths.clear();
    otherIds.clear();
    for (int i = 0; i < BOATS_PER_PLAYER; i++) {
        Boat &boat = *tmpBoats[i];
        vector<PointWithDirection> myPath;
        if (boat.assigned) {
            for (int j = 0; j < min(BOAT_PREDICT_DISTANCE, (int) boat.path.size()); j++) {
                myPath.push_back(boat.path[j]);
            }
        } else {
            //没assign
            for (int j = 0; j < BOAT_PREDICT_DISTANCE; j++) {
                myPath.emplace_back(boat.corePoint, boat.direction);
            }
            boat.path = myPath;
        }
        int crashId = BoatUtils::boatCheckCrash(gameMap, boat.id, myPath, otherPaths, otherIds, BOAT_AVOID_DISTANCE);
        if (crashId != -1) {
            boats[crashId].beConflicted = FPS;
        }
        if (crashId != -1 && boat.status != 1) {
            //此时可以避让，则开始避让
            //避让
            vector<Point> conflictPoints;
            vector<Point> noResultPoints;
            for (vector<PointWithDirection> &otherPath: otherPaths) {
                //别人所在位置锁住
                PointWithDirection pointWithDirection = otherPath[0];
                vector<Point> points = GameMap::getBoatPoints(pointWithDirection.point, pointWithDirection.direction);
                for (Point &point: points) {
                    if (!gameMap.isBoatMainChannel(point.x, point.y)) {
                        conflictPoints.push_back(point);
                    }
                }
            }
            for (int j = 0; j < otherPaths.size(); j++) {
                if (otherIds[j] == crashId) {
                    //撞到那个人的预测路径不是结果点
                    vector<PointWithDirection> pointWithDirections = otherPaths[j];
                    for (PointWithDirection pointWithDirection: pointWithDirections) {
                        vector<Point> points = GameMap::getBoatPoints(pointWithDirection.point,
                                                                      pointWithDirection.direction);
                        for (Point point: points) {
                            if (!gameMap.isBoatMainChannel(point.x, point.y)) {
                                noResultPoints.push_back(point);
                            }
                        }
                    }
                }
            }
            for (const Point &point: conflictPoints) {
                gameMap.commonConflictPoints[point.x][point.y] = true;
            }
            for (const Point &point: noResultPoints) {
                gameMap.commonNoResultPoints[point.x][point.y] = true;
            }
            vector<PointWithDirection> result = BoatUtils::boatGetSafePoints(gameMap, gameMap.boatCommonCs, myPath[0],
                                                                             gameMap.commonConflictPoints,
                                                                             gameMap.commonNoResultPoints,
                                                                             BOAT_AVOID_CANDIDATE_SIZE);
            for (const Point &point: conflictPoints) {
                gameMap.commonConflictPoints[point.x][point.y] = false;
            }
            for (const Point &point: noResultPoints) {
                gameMap.commonNoResultPoints[point.x][point.y] = false;
            }
            if (!result.empty()) {
                PointWithDirection *selectPoint = nullptr;
                double minDistance = INT_INF;
                for (PointWithDirection &pointWithDirection: result) {
                    double curDis = (gameMap.boatCommonCs[pointWithDirection.point.x]
                    [pointWithDirection.point.y][pointWithDirection.direction] >> 2);
                    //到目标距离
                    if (boat.assigned) {
                        double toTargetDistance;
                        if (boat.carry) {
                            //买
                            toTargetDistance = boatSellPoints[boat.targetSellId].getMinDistance(
                                    pointWithDirection.point, pointWithDirection.direction);
                        } else {
                            toTargetDistance = berths[boat.targetBerthId].getBoatMinDistance(
                                    pointWithDirection.point, pointWithDirection.direction);
                        }
                        curDis = curDis + toTargetDistance / 2;
                    }
                    if (curDis < minDistance) {
                        selectPoint = &pointWithDirection;
                        minDistance = curDis;
                    }
                }
                if (selectPoint != nullptr) {
                    int oneDistance = (gameMap.boatCommonCs[selectPoint->point.x]
                    [selectPoint->point.y][selectPoint->direction] >> 2);
                    if (boat.assigned) {
                        if (boat.carry) {
                            //买
                            oneDistance += boatSellPoints[boat.targetSellId].getMinDistance(selectPoint->point,
                                                                                            selectPoint->direction);
                        } else {
                            oneDistance += berths[boat.targetBerthId].getBoatMinDistance(selectPoint->point,
                                                                                         selectPoint->direction);
                        }
                    }
                    //计算闪现需要时间
                    PointWithDirection mid = getBoatFlashDeptPoint(boat.corePoint);
                    //计算到达时间
                    int waitTime = 1 + abs(boat.corePoint.x - mid.point.x) + abs(boat.corePoint.y - mid.point.y);
                    int twoDistance = waitTime;
                    if (waitTime == 1) {//闪现原地,一定不选
                        twoDistance += MAP_FILE_ROW_NUMS * MAP_FILE_COL_NUMS;
                    }
                    if (boat.assigned) {
                        if (boat.carry) {
                            //买
                            twoDistance += boatSellPoints[boat.targetSellId].getMinDistance(mid.point,
                                                                                            mid.direction);
                        } else {
                            twoDistance += berths[boat.targetSellId].getBoatMinDistance(mid.point, mid.direction);
                        }
                    }

                    if (twoDistance <= oneDistance) {
                        //闪现避让
                        myPath.clear();
                        myPath.emplace_back(boat.corePoint, boat.direction);
                        for (int j = 0; j < waitTime; j++) {
                            myPath.push_back(mid);
                        }
                    } else {
                        //正常避让
                        myPath = BoatUtils::backTrackPath(gameMap, *selectPoint, gameMap.boatCommonCs, 0);
                        if (myPath.size() == 1) {
                            myPath.push_back(myPath[0]);
                        }
                        while (myPath.size() > BOAT_PREDICT_DISTANCE) {
                            myPath.pop_back();
                        }
                    }
                    boat.avoid = true;
                }
            } else {
                //无路可走，可以尝试提高优先级，不然就
                if (boat.forcePri > 2) {
                    //闪现避让
                    //计算闪现需要时间
                    printError("no path can go, flash");
                    PointWithDirection mid = getBoatFlashDeptPoint(boat.corePoint);
                    //计算到达时间
                    int waitTime = 1 + abs(boat.corePoint.x - mid.point.x) + abs(boat.corePoint.y - mid.point.y);
                    myPath.clear();
                    myPath.emplace_back(boat.corePoint, boat.direction);
                    for (int j = 0; j < waitTime; j++) {
                        myPath.push_back(mid);
                    }
                    boat.avoid = true;
                } else {
                    boat.beConflicted = 2 * FPS;
                    boat.forcePri += 1;
                    otherPaths.clear();
                    otherIds.clear();
                    sortBoats(tmpBoats);
                    i = -1;
                    continue;
                }
            }
        }
        //高优先级的撞到你，则不动
        PointWithDirection cur = myPath[0];
        PointWithDirection next = myPath[1];
        //不是整个船在主隧道上，且自己不动，则需要别人检查是否这个帧id小于他，或者id大于这一帧撞你移动后,都是同一个位置
        if (cur == next && !gameMap.boatIsAllInMainChannel(cur.point, cur.direction)) {
            for (int j = 0; j < otherPaths.size(); j++) {
                assert(otherPaths[j].size() >= 2);
                PointWithDirection myStart = myPath[0];
                assert(myStart == myPath[1]);
                PointWithDirection otherNext = otherPaths[j][1];
                if (BoatUtils::boatCheckCrash(gameMap, otherNext, myStart)) {
                    PointWithDirection otherStart = otherPaths[j][0];
                    otherPaths[j].clear();
                    for (int k = 0; k < 2; k++) {
                        otherPaths[j].push_back(otherStart);
                    }
                    boats[otherIds[j]].avoid = true;
                }
            }
        }
        otherPaths.push_back(myPath);
        otherIds.push_back(boat.id);
    }
    for (int i = 0; i < otherPaths.size(); i++) {
        int id = otherIds[i];//改成避让路径
        if (boats[id].avoid) {
            boats[id].path = std::move(otherPaths[i]);
        }
    }
    otherPaths.clear();
    otherIds.clear();
    for (Boat &boat: boats) {
        if (boat.noMoveTime == 0) {
            continue;
        }
        if (boat.path.size() < 2) {
            printError("error boat path size <2");
            continue;
        }
        PointWithDirection next = boat.path[1];
        if (next.point == boat.corePoint && next.direction == boat.direction) {
            continue;
        }
        //下一个点,一定要是移动的点,闪现啥的不算
        int index = -1;
        for (int i = 0; i < 3; i++) {
            PointWithDirection nextPoint = BoatUtils::getNextPoint({boat.corePoint, boat.direction}, i);
            if (nextPoint == next) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            continue;
        }
        //1.超过一定时间动不了，比较大
        if (boat.noMoveTime > BOAT_FLASH_NO_MOVE_TIME) {
            //直接闪现避让
            printError("error no path to avoid other ,flash avoid");
            PointWithDirection mid = getBoatFlashDeptPoint(boat.corePoint);
            //计算到达时间
            int waitTime = 1 + abs(boat.corePoint.x - mid.point.x) + abs(boat.corePoint.y - mid.point.y);
            boat.path.clear();
            boat.path.emplace_back(boat.corePoint, boat.direction);
            for (int j = 0; j < waitTime; j++) {
                boat.path.push_back(mid);
            }
            continue;
        }
        //2.超过一定时间自己动不了，或者自己的下一个位置撞到一个不动的人，说明其他人一定没在避让，自己得让
        bool avoidOther = false;

        for (const SimpleBoat &other: totalBoats) {
            if (other.belongToMe) {
                continue;
            }
            //撞到一个超过五帧不动的人,
            if (other.noMoveTime > 5 && BoatUtils::boatCheckCrash(gameMap, next, other.pointWithDirection)) {
                avoidOther = true;
                break;
            }
        }

        if (boat.noMoveTime >= BOAT_AVOID_OTHER_TRIGGER_THRESHOLD && !avoidOther) {
            bool otherAvoid = false;
            if (boat.noMoveTime < 2 * BOAT_AVOID_OTHER_TRIGGER_THRESHOLD) {
                //超过6帧直接让，对面让不了
                for (SimpleBoat &other: totalBoats) {
                    if (other.belongToMe) {
                        continue;
                    }
                    //撞到一个还在动得人，说明对面可能在让,自己稍微等一下
                    if (other.noMoveTime == 0
                        && BoatUtils::boatCheckCrash(gameMap,
                                                     next, other.pointWithDirection)) {
                        otherAvoid = true;
                        break;
                    }
                }
            }
            if (!otherAvoid) {
                avoidOther = true;
            }
        }
        if (!avoidOther) {
            //对面超过10帧没动，或者自己没动超过2帧，则避让
            continue;
        }
        //3个方向，只要撞到一个就锁


        vector<Point> curPoints = GameMap::getBoatPoints(boat.corePoint, boat.direction);
        for (const Point &point: curPoints) {
            gameMap.commonConflictPoints[point.x][point.y] = true;
        }
        vector<Point> avoidPoints;
        vector<Point> nextPoints = GameMap::getBoatPoints(next.point, next.direction);
        for (Point &point: nextPoints) {
            if (!gameMap.commonConflictPoints[point.x][point.y]
                && !gameMap.isBoatMainChannel(point.x, point.y)) {
                avoidPoints.push_back(point);
            }
        }
        for (int i = 0; i < 3; i++) {
            PointWithDirection nextPoint = BoatUtils::getNextPoint(
                    {boat.corePoint, boat.direction}, i);
            //是否撞到别人,不动超过五帧或者下一个方向撞到
            for (SimpleBoat &simpleBoat: totalBoats) {
                if (simpleBoat.belongToMe) {
                    continue;
                }
                if (i != index && simpleBoat.noMoveTime < 5) {
                    continue;
                }
                if (!BoatUtils::boatCheckCrash(gameMap, simpleBoat.pointWithDirection, nextPoint)) {
                    continue;
                }
                vector<Point> candidates = GameMap::getBoatPoints(simpleBoat.corePoint, simpleBoat.direction);
                for (Point point: candidates) {
                    if (!gameMap.commonConflictPoints[point.x][point.y]
                        && !gameMap.isBoatMainChannel(point.x, point.y)) {
                        avoidPoints.push_back(point);
                    }
                }
            }
        }
        for (const Point &point: curPoints) {
            gameMap.commonConflictPoints[point.x][point.y] = false;
        }
        //加入避让路径进去
        for (const Point &avoidPoint: avoidPoints) {
            bool contain = false;
            for (Point point2: boatsAvoidOtherPoints[boat.id]) {
                if (avoidPoint == point2) {
                    contain = true;
                    break;
                }
            }
            if (!contain) {
                //加入进去
                boatsAvoidOtherPoints[boat.id].push_back(avoidPoint);
            }
        }
        for (const Point &point2: boatsAvoidOtherPoints[boat.id]) {
            gameMap.commonConflictPoints[point2.x][point2.y] = true;
        }
        //保持目标不变，重新寻路,启发式搜
        vector<PointWithDirection> avoidPath;
        if (boat.assigned) {
            if (boat.carry) {
                //to sellPoint，不避让队友，减少时间
                avoidPath = boatToSellPointHeuristic(boat, boatSellPoints[boat.targetSellId], {}, {},
                                                     gameMap.commonConflictPoints);
            } else {
                //to berth
                avoidPath = boatToBerthHeuristic(boat, berths[boat.targetBerthId], {}, {},
                                                 gameMap.commonConflictPoints);
            }
            if (!avoidPath.empty()) {
                boat.path = avoidPath;
            }
        }
        if (avoidPath.empty()) {
            //随机找一个位置去避让，没办法了
            printError("frame:" + to_string(frameId) + "boatId:" + to_string(boat.id) + ",can not avoid suiji");
            index = int(e() % 3);
            PointWithDirection curPoint = boat.path[0];
            PointWithDirection nextPoint = BoatUtils::getNextPoint(boat.path[0], index);
            while (!gameMap.boatCanReach(nextPoint.point, nextPoint.direction)) {
                index = int(e() % 3);
                nextPoint = BoatUtils::getNextPoint(boat.path[0], index);
            }
            boat.path.clear();
            boat.path.push_back(curPoint);
            boat.path.push_back(nextPoint);
        }
        //回退
        for (const Point &point2: boatsAvoidOtherPoints[boat.id]) {
            gameMap.commonConflictPoints[point2.x][point2.y] = false;
        }
        //避让
        boat.avoidOtherTime = BOAT_AVOID_OTHER_DURATION;
    }

    for (Boat &boat: boats) {
        boat.finish();
        if (boat.avoidOtherTime-- < 0) {
            //清空锁死点
            boatsAvoidOtherPoints[boat.id].clear();
        }
        if (boat.beConflicted-- < 0 && boat.forcePri != 0) {
            boat.forcePri = 0;
        }
    }
    long long int l4 = runTime();
    printError("frame:" + to_string(frameId) + ",boatDicision:" + to_string(l2 - l1) + ",path:" + to_string(l3 - l2) +
               ",other:" + to_string(l4 - l3));
}

//如果在回家，则估计下一个目标，如果不是，决策当前目标
bool Strategy::boatGreedyBuy1(int goodsNumList[MAX_BERTH_PER_PLAYER]) {

    for (Berth &berth: berths) {
        //停靠在泊位上了
        if (berth.curBoatGlobalId != -1
            && berth.goodsNums > 0 && totalBoats[berth.curBoatGlobalId].belongToMe
            && !boats[totalBoats[berth.curBoatGlobalId].localId].buyAssign
            && !boats[totalBoats[berth.curBoatGlobalId].localId].carry) {
            //没去卖
            Boat &boat = boats[totalBoats[berth.curBoatGlobalId].localId];
            decisionBoatBuy(boat, berth, boatSellPoints[boat.targetSellId], goodsNumList);
            return true;
        }
    }
    Boat *bestBoat = nullptr;
    Berth *bestBerth = nullptr;
    BoatSellPoint *bestSellPoint = nullptr;
    double bestProfit = -GAME_FRAME;

    for (int i = 0; i < berths.size(); i++) {
        Berth &berth = berths[i];
        if (berthLock.count(berth.id)) {
            continue;
        }
        int berthGoodNum = goodsNumList[i];
        if (berth.minSellPointId == -1) {
            continue;
        }
        //找最近的船
        Boat *selectBoat = nullptr;
        int minBuyDistance = INT_INF;
        for (Boat &boat: boats) {
            if (boat.buyAssign) {
                continue;
            }
            if (!berth.boatCanReach(boat.corePoint, boat.direction)) {
                continue;
            }
            int distance = boatMinToBerthDistance(boat, berth);
            if (distance < minBuyDistance) {
                minBuyDistance = distance;
                selectBoat = &boat;
            }
        }
        if (selectBoat == nullptr) {
            continue;
        }
        int minSellDistance = berth.minSellDistance;
        if (frameId + minBuyDistance + minSellDistance >= GAME_FRAME) {
            continue;//不做决策
        }
        //价值是船上的个数加泊位个数
        int needCount = selectBoat->carry ? selectBoat->capacity : selectBoat->capacity - selectBoat->num;
        int value = min(needCount, berthGoodNum);
        double profit;
        if (value > 0) {
            profit = 1.0 * value / (double) (minBuyDistance + minSellDistance);
        } else {
            profit = value + 1.0 / (minBuyDistance + minSellDistance);
        }
        if (!selectBoat->carry
            && selectBoat->targetBerthId != berth.id
            && selectBoat->targetBerthId != -1 &&
            berths[selectBoat->targetBerthId].curBoatGlobalId
            != selectBoat->globalId) {
            //没到泊位切换目标降低点价值
            profit *= BOAT_CHANGE_FACTOR;
        }
        if (profit > bestProfit) {
            bestProfit = profit;
            bestBoat = selectBoat;
            bestBerth = &berth;
            bestSellPoint = &boatSellPoints[berth.minSellPointId];
        }
    }
    if (bestBoat == nullptr)
        return false;
    //已经在某个泊位上，没超过一定的数量差不切换泊位？
    if (!bestBoat->carry && bestBoat->targetBerthId != -1 &&
        berths[bestBoat->targetBerthId].curBoatGlobalId == bestBoat->globalId &&
        bestBerth->id != bestBoat->targetBerthId &&
        goodsNumList[bestBerth->id] < BOAT_CHANGE_BERTH_MIN_NUM_DIFF) {
        bestBerth = &berths[bestBoat->targetBerthId];
    }
    decisionBoatBuy(*bestBoat, *bestBerth, *bestSellPoint, goodsNumList);
    return true;
}

bool Strategy::boatGreedyBuy2(int goodsNumList[MAX_BERTH_PER_PLAYER]) {
    for (Berth &berth: berths) {
        //停靠在泊位上了
        if (berth.curBoatGlobalId != -1
            && berth.goodsNums > 0 && totalBoats[berth.curBoatGlobalId].belongToMe
            && !boats[totalBoats[berth.curBoatGlobalId].localId].buyAssign
            && !boats[totalBoats[berth.curBoatGlobalId].localId].carry) {
            //没去卖
            Boat &boat = boats[totalBoats[berth.curBoatGlobalId].localId];
            decisionBoatBuy(boat, berth, boatSellPoints[boat.targetSellId], goodsNumList);
            return true;
        }
    }
    Boat *bestBoat = nullptr;
    Berth *bestBerth = nullptr;
    BoatSellPoint *bestSellPoint = nullptr;
    double bestProfit = -GAME_FRAME;

    for (int i = 0; i < berths.size(); i++) {
        Berth &berth = berths[i];
        if (berthLock.count(berth.id)) {
            continue;
        }
        int berthGoodNum = goodsNumList[i];
        if (berth.minSellPointId == -1) {
            continue;
        }
        //找最近的船
        Boat *selectBoat = nullptr;
        int minBuyDistance = INT_INF;
        for (Boat &boat: boats) {
            if (boat.buyAssign) {
                continue;
            }
            if (!berth.boatCanReach(boat.corePoint, boat.direction)) {
                continue;
            }
            int distance = boatMinToBerthDistance(boat, berth);
            if (distance < minBuyDistance) {
                minBuyDistance = distance;
                selectBoat = &boat;
            }
        }
        if (selectBoat == nullptr) {
            continue;
        }
        int minSellDistance = berth.minSellDistance;
        if (frameId + minBuyDistance + minSellDistance >= GAME_FRAME) {
            continue;//不做决策
        }
        //价值是船上的个数加泊位个数
        int needCount = selectBoat->carry ? selectBoat->capacity : selectBoat->capacity - selectBoat->num;
        int value = min(needCount, berthGoodNum);
        double profit;
        if (value > 0) {
            profit = 1.0 * value / (double) (minBuyDistance + minSellDistance);
        } else {
            profit = value + 1.0 / (minBuyDistance + minSellDistance);
        }
        if (!selectBoat->carry
            && selectBoat->targetBerthId == berth.id &&
            berth.curBoatGlobalId
            != selectBoat->globalId) {
            //没到泊位切换目标降低点价值
            profit *= (1 + BOAT_SAME_TARGET_FACTOR);
        }
        if (profit > bestProfit) {
            bestProfit = profit;
            bestBoat = selectBoat;
            bestBerth = &berth;
            bestSellPoint = &boatSellPoints[berth.minSellPointId];
        }
    }
    if (bestBoat == nullptr)
        return false;
    //已经在某个泊位上，没超过一定的数量差不切换泊位？
    if (!bestBoat->carry && bestBoat->targetBerthId != -1 &&
        berths[bestBoat->targetBerthId].curBoatGlobalId == bestBoat->globalId &&
        bestBerth->id != bestBoat->targetBerthId &&
        goodsNumList[bestBerth->id] < BOAT_CHANGE_BERTH_MIN_NUM_DIFF) {
        bestBerth = &berths[bestBoat->targetBerthId];
    }
    decisionBoatBuy(*bestBoat, *bestBerth, *bestSellPoint, goodsNumList);
    return true;
}

bool Strategy::boatGreedyBuy3(int goodsNumList[MAX_BERTH_PER_PLAYER]) {
    for (Berth &berth: berths) {
        //停靠在泊位上了
        if (berth.curBoatGlobalId != -1
            && berth.goodsNums > 0 && totalBoats[berth.curBoatGlobalId].belongToMe
            && !boats[totalBoats[berth.curBoatGlobalId].localId].buyAssign
            && !boats[totalBoats[berth.curBoatGlobalId].localId].carry) {
            //没去卖
            Boat &boat = boats[totalBoats[berth.curBoatGlobalId].localId];
            decisionBoatBuy(boat, berth, boatSellPoints[boat.targetSellId], goodsNumList);
            return true;
        }
    }
    Boat *bestBoat = nullptr;
    Berth *bestBerth = nullptr;
    BoatSellPoint *bestSellPoint = nullptr;
    double bestProfit = -GAME_FRAME;

    for (int i = 0; i < berths.size(); i++) {
        Berth &berth = berths[i];
        if (berthLock.count(berth.id)) {
            continue;
        }
        int berthGoodNum = goodsNumList[i];
        if (berth.minSellPointId == -1) {
            continue;
        }
        //找最近的船
        Boat *selectBoat = nullptr;
        int minBuyDistance = INT_INF;
        for (Boat &boat: boats) {
            if (boat.buyAssign) {
                continue;
            }
            if (!berth.boatCanReach(boat.corePoint, boat.direction)) {
                continue;
            }
            int distance = boatMinToBerthDistance(boat, berth);
            if (distance < minBuyDistance) {
                minBuyDistance = distance;
                selectBoat = &boat;
            }
        }
        if (selectBoat == nullptr) {
            continue;
        }
        int minSellDistance = berth.minSellDistance;
        if (frameId + minBuyDistance + minSellDistance >= GAME_FRAME) {
            continue;//不做决策
        }
        //价值是船上的个数加泊位个数
        int needCount = selectBoat->carry ? selectBoat->capacity : selectBoat->capacity - selectBoat->num;
        int value = min(needCount, berthGoodNum);
        double profit;//近的，货物多的
        profit = value + 1.0 / (double) (minBuyDistance * 2 + minSellDistance);
        if (!selectBoat->carry
            && selectBoat->targetBerthId == berth.id &&
            berth.curBoatGlobalId
            != selectBoat->globalId) {
            //没到泊位切换目标降低点价值
            profit *= (1 + BOAT_SAME_TARGET_FACTOR);
        }
        if (profit > bestProfit) {
            bestProfit = profit;
            bestBoat = selectBoat;
            bestBerth = &berth;
            bestSellPoint = &boatSellPoints[berth.minSellPointId];
        }
    }
    if (bestBoat == nullptr)
        return false;
    //已经在某个泊位上，没超过一定的数量差不切换泊位？
    if (!bestBoat->carry && bestBoat->targetBerthId != -1 &&
        berths[bestBoat->targetBerthId].curBoatGlobalId == bestBoat->globalId &&
        bestBerth->id != bestBoat->targetBerthId &&
        goodsNumList[bestBerth->id] < BOAT_CHANGE_BERTH_MIN_NUM_DIFF) {
        bestBerth = &berths[bestBoat->targetBerthId];
    }
    decisionBoatBuy(*bestBoat, *bestBerth, *bestSellPoint, goodsNumList);
    return true;
}


void Strategy::robotDoAction() {
    long long int l1 = runTime();
    for (auto &set: robotLock) {
        set.clear();
    }
    while (true) {
        if (!robotGreedySell()) {
            break;
        }  //决策
    }
    valueList.clear();
    robotLastFrameSelectIds.clear();
    for (const auto &robot: robots) {
        if (!robot.carry && robot.targetWorkBenchId != -1 && workbenches.count(robot.targetWorkBenchId)) {
            workbenches[robot.targetWorkBenchId].lastFrameSelect = true;
            robotLastFrameSelectIds.insert(robot.targetWorkBenchId);
        }
    }
    for (auto &item: workbenches) {
        Workbench &workbench = item.second;
        double maxProfit = 0.5 * workbench.value / max(1, workbench.minSellDistance);
        int minSellDistance = workbench.minSellDistance + berths[workbench.minSellBerthId].minSellDistance;
        if (minSellDistance + frameId >= GAME_FRAME) {
            maxProfit = -minSellDistance;
        }
        if (workbench.lastFrameSelect) {
            if (maxProfit >= 0) {
                maxProfit *= (1 + SAME_TARGET_REWARD_FACTOR + 1);//乘以10倍
            } else {
                maxProfit = 0;//直接增加到0
            }
            workbench.lastFrameSelect = false;
        }
        workbench.curMaxProfit = maxProfit;
        workbench.maxProfitId = -1;
        int value = ceil(maxProfit * PROFIT_ZOOM_FACTOR);
        if (workbenchesLock.count(workbench.id)) {
            continue;//别人选择过了
        }
        if (workbenchesPermanentLock.count(workbench.id)) {
            continue;//回答错误的workbench；
        }
        valueList.emplace_back(value, item.first);
    }
    sort(valueList.begin(), valueList.end(), [](pair<int, int> &first, pair<int, int> &second) -> bool {
        if (first.first != second.first) {
            return first.first > second.first;
        }
        return first.second < second.second;
    });
    robotStartDecisionBuyTime = runTime();
    while (true) {
        if (!robotGreedyBuy2()) {
            break;
        }
    }
    long long int l2 = runTime();
    Robot *tmpRobots[ROBOTS_PER_PLAYER];
    for (int i = 0; i < robots.size(); i++) {
        tmpRobots[i] = &robots[i];
    }
    sortRobots(tmpRobots);
    //1.选择路径,和修复路径
    vector<vector<Point>> otherPaths;
    otherPaths.reserve(ROBOTS_PER_PLAYER);
    for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
        Robot &robot = *tmpRobots[i];
        if (!robot.assigned || robot.isPending) {
            continue;
        }
        vector<Point> path;

        if (robot.carry) {
            //启发式寻路,如果保存的话太多了
            path = berths[robot.targetBerthId].robotMoveFrom(robot.pos);
        } else {
            path = workbenches[robot.targetWorkBenchId].moveFrom(robot.pos);
        }

        //检查是否与前面机器人相撞，如果是，则重新搜一条到目标点的路径，极端情况，去到物品消失不考虑
        if (RobotUtils::robotCheckCrash(gameMap, path, otherPaths)) {
            if (robot.carry) {
                //to berth
                vector<Point> avoidPath = robotToBerthHeuristic(robot, berths[robot.targetBerthId], otherPaths,
                                                                nullptr);
                if (!avoidPath.empty()) {
                    path = avoidPath;
                }
            } else {
                vector<Point> avoidPath = robotToWorkBenchHeuristic(robot, workbenches[robot.targetWorkBenchId],
                                                                    otherPaths, nullptr);
                if (!avoidPath.empty()) {
                    path = avoidPath;
                }
                if (avoidPath.empty() && !robot.redundancy) {
                    robot.assigned = false;
                    robot.buyAssign = false;
                    robotLock[robot.id].insert(robot.targetWorkBenchId);
                    robotGreedyBuy2();
                    i--;
                    //重开
                    continue;
                }
            }
        }
        if (robot.avoidOtherTime > 0) {
            vector<Point> avoidOtherPoints = robotsAvoidOtherPoints[robot.id];
            if (avoidOtherPoints.empty()) {
                printError("error in avoid OtherTime");
            }
            for (const Point &avoidPoint: avoidOtherPoints) {
                gameMap.commonConflictPoints[avoidPoint.x][avoidPoint.y] = true;
            }
            vector<Point> avoidPath;
            if (robot.carry) {
                //to berth
                avoidPath = robotToBerthHeuristic(robot, berths[robot.targetBerthId], otherPaths,
                                                  gameMap.commonConflictPoints);
            } else {
                avoidPath = robotToWorkBenchHeuristic(robot, workbenches[robot.targetWorkBenchId], otherPaths,
                                                      gameMap.commonConflictPoints);
            }
            if (!avoidPath.empty()) {
                path = std::move(avoidPath);
                //找不到不需要随机走
            }
            for (const Point &avoidPoint: avoidOtherPoints) {
                gameMap.commonConflictPoints[avoidPoint.x][avoidPoint.y] = false;
            }
        }
        otherPaths.push_back(path);
        robot.path = std::move(path);
    }
    long long int l3 = runTime();
    //2.碰撞避免

    for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
        //固定只预测未来一个格子，就是两个小格子，
        //预测他未来两个格子就行，四下，如果冲突，则他未来一个格子自己不能走，未来第二个格子自己尽量也不走
        Robot &robot = *tmpRobots[i];
        robotsPredictPath[robot.id].clear();
        if (!robot.assigned || robot.isPending) {
            //未来一格子在这里不动，如果别人撞过来，则自己避让
            robot.path.push_back(GameMap::posToDiscrete(robot.pos));
            for (int j = 1; j <= 2; j++) {
                robotsPredictPath[robot.id].push_back(GameMap::posToDiscrete(robot.pos));
                robot.path.push_back(GameMap::posToDiscrete(robot.pos));
            }
        } else {
            if (robot.path.size() <= 1) {
                //原点不动，大概率有漏洞
                robotsPredictPath[robot.id].push_back(GameMap::posToDiscrete(robot.pos));
                robotsPredictPath[robot.id].push_back(GameMap::posToDiscrete(robot.pos));
                printError("error robot.path.size()<=1");
            }
            for (int j = 1; j <= min(6, ((int) robot.path.size()) - 1); j++) {
                robotsPredictPath[robot.id].push_back(robot.path[j]);
            }
        }
        int crashId = -1;
        for (int j = 0; j < i; j++) {
            //一个格子之内撞不撞
            for (int k = 0; k < 2; k++) {
                Point point = robotsPredictPath[robot.id][k];
                if (point == (robotsPredictPath[tmpRobots[j]->id][k]) &&
                    !gameMap.isRobotDiscreteMainChannel(point.x, point.y)) {
                    crashId = tmpRobots[j]->id;
                    break;
                }
            }
            if (crashId != -1) {
                break;
            }
        }

        //看看自己是否可以避让，不可以的话就说明被夹住了,让冲突点去让,并且自己强制提高优先级50帧
        if (crashId != -1) {
            robots[crashId].beConflicted = 2 * FPS;
            vector<Point> candidates;
            candidates.push_back(robot.pos);
            for (int j = 0; j < sizeof(DIR) / sizeof(DIR[0]) / 2; j++) {
                candidates.push_back(robot.pos + (DIR[j]));
            }
            Point result(-1, -1);
            int bestDist = INT_INF;

            for (Point candidate: candidates) {
                if (!gameMap.robotCanReach(candidate.x, candidate.y)) {
                    continue;
                }
                //细化成两个去判断
                bool crash = false;
                for (Robot *tmpRobot: tmpRobots) {
                    Robot &otherRobot = *tmpRobot;
                    if (otherRobot.id == robot.id || robotsPredictPath[otherRobot.id].empty()) {
                        continue;//后面的
                    }
                    Point start = GameMap::posToDiscrete(robot.pos);
                    Point end = GameMap::posToDiscrete(candidate);
                    Point mid = (start + end) / 2;
                    if ((mid == (robotsPredictPath[otherRobot.id][0]) &&
                         !gameMap.isRobotDiscreteMainChannel(mid.x, mid.y))
                        || (end == (robotsPredictPath[otherRobot.id][1]) &&
                            !gameMap.isRobotDiscreteMainChannel(end.x, end.y))) {
                        //去重全加进来
                        crash = true;
                        break;
                    }
                }
                if (crash) {
                    continue;
                }
                int dist;
                if (!robot.carry) {
                    if (!robot.assigned) {
                        dist = MAP_FILE_ROW_NUMS * MAP_FILE_COL_NUMS;
                    } else {
                        dist = workbenches[robot.targetWorkBenchId].getMinDistance(candidate);
                    }
                } else {
                    dist = berths[robot.targetBerthId].getRobotMinDistance(candidate);
                }
                Point tmp = GameMap::posToDiscrete(candidate);
                for (Point &point: robotsPredictPath[crashId]) {
                    if (tmp == point && !gameMap.isRobotDiscreteMainChannel(candidate.x, candidate.y)) {
                        dist += 2;//在别人路径上惩罚增大
                        break;
                    }
                }
                if (dist < bestDist) {
                    result = candidate;
                    bestDist = dist;
                }
            }
            if (!(result == Point(-1, -1))) {
                //修改预测路径
                robotsPredictPath[robot.id].clear();
                robots[robot.id].avoid = true;
                Point start = GameMap::posToDiscrete(robot.pos);
                Point end = GameMap::posToDiscrete(result);
                Point mid = (start + end) / 2;
                robotsPredictPath[robot.id].push_back(mid);//中间
                robotsPredictPath[robot.id].push_back(end);//下一个格子
            } else {
                robot.beConflicted = 2 * FPS;
                robot.forcePri += 1;
                sortRobots(tmpRobots);
                i = -1;
            }
        }
    }
    for (Robot &robot: robots) {
        if (robot.avoid) {
            Point start = GameMap::posToDiscrete(robot.pos);
            robot.path.clear();
            robot.path.push_back(start);
            robot.path.push_back(robotsPredictPath[robot.id][0]);
            robot.path.push_back(robotsPredictPath[robot.id][1]);
            //在避让，所以路径改变了，稍微改一下好看一点
        }
    }
    for (Robot &robot: robots) {
        if (robot.isPending || !robot.assigned) {
            continue;
        }
        bool avoidOther = false;
        Point originNext = GameMap::discreteToPos(robot.path[2]);
        if (robot.pos == originNext) {
            if (!robot.avoid) {
                printError("error in avoid other");
            }
            continue;
        }

        for (SimpleRobot &other: totalRobots) {
            if (other.belongToMe) {
                continue;
            }
            //对面5帧不动，直接让
            if (other.noMoveTime >= ROBOT_DIRECTION_AVOID_OTHER_TRIGGER_THRESHOLD && other.p == originNext) {
                avoidOther = true;
                break;
            }
        }

        if (robot.noMoveTime >= ROBOT_AVOID_OTHER_TRIGGER_THRESHOLD) {
            //自己，2帧没动，也让
            avoidOther = true;
        }
        if (!avoidOther) {
            //对面超过10帧没动，或者自己没动超过2帧，则避让
            continue;
        }
        //大于判断是否其他人也不动
        //我的下一个点四周有至少一个机器人不动

        vector<Point> avoidPoints;
        avoidPoints.push_back(originNext);//前一个点是避让点
        for (int j = 0; j < 4; j++) {
            Point nextPoint = robot.pos + DIR[j];
            if (nextPoint == originNext ||
                !gameMap.robotCanReach(nextPoint.x, nextPoint.y)
                || gameMap.isRobotMainChannel(nextPoint.x, nextPoint.y)) {
                continue;
            }
            //检查是否又机器人超过5帧不动
            for (SimpleRobot &simpleRobot: totalRobots) {
                if (simpleRobot.belongToMe) {
                    continue;
                }
                if (simpleRobot.noMoveTime < ROBOT_DIRECTION_AVOID_OTHER_TRIGGER_THRESHOLD) {
                    //5帧没动也让
                    continue;
                }
                if (simpleRobot.p == nextPoint) {
                    avoidPoints.push_back(nextPoint);
                    break;
                }
            }
        }
        //得到需要避让的点,重新寻路
        if (robot.path.size() == 3) {
            bool hasOther = false;
            for (SimpleRobot &other: totalRobots) {
                if (other.belongToMe) {
                    continue;
                }
                //对面5帧不动，直接让
                if (other.p == originNext) {
                    hasOther = true;
                    break;
                }
            }
            if (!hasOther) {
                //下一个目标没人，给他了
                robot.path.clear();
                robot.path.push_back(GameMap::posToDiscrete(robot.pos));
                robot.path.push_back(GameMap::posToDiscrete(robot.pos));
                robot.path.push_back(GameMap::posToDiscrete(robot.pos));
            }
            //有人，则努力冲，直到他50帧不动当他人机
            continue;
        }
        //避让路径不存在的点加入避让路径进去
        for (Point &avoidPoint: avoidPoints) {
            bool contain = false;
            for (const Point &point2: robotsAvoidOtherPoints[robot.id]) {
                if (avoidPoint == point2) {
                    contain = true;
                    break;
                }
            }
            if (!contain) {
                //加入进去
                robotsAvoidOtherPoints[robot.id].push_back(avoidPoint);
            }
        }
        for (const Point &point2: robotsAvoidOtherPoints[robot.id]) {
            //冲突点更新
            gameMap.commonConflictPoints[point2.x][point2.y] = true;
        }
        //保持目标不变，重新寻路,启发式搜
        vector<Point> avoidPath;
        if (robot.assigned) {
            if (robot.carry) {
                //不避让队友，最大程度寻路
                avoidPath = robotToBerthHeuristic(robot, berths[robot.targetBerthId], {},
                                                  gameMap.commonConflictPoints);
            } else {
                avoidPath = robotToWorkBenchHeuristic(robot, workbenches[robot.targetWorkBenchId], {},
                                                      gameMap.commonConflictPoints);
            }
            if (!avoidPath.empty()) {
                robot.path = avoidPath;
            }
        }
        if (avoidPath.empty() || robot.noMoveTime > FPS * 3) {
            if (robot.noMoveTime > FPS * 3) {
                printError(
                        "error,frame:" + to_string(frameId) + "robotId:" + to_string(robot.id) + ",findPathNoUse kasi");
            }
            //随机找一个位置去避让，没办法了
            printError("frame:" + to_string(frameId) + "robotId:" + to_string(robot.id) + ",can not avoid suiji");
            int index = int(e() % 4);
            Point next = robot.pos + DIR[index];
            if (next == originNext) {
                //如果随机出了原先下一个点，则站着不动
                next = robot.pos;
            }
            while (!gameMap.robotCanReach(next.x, next.y)) {
                index = int(e() % 4);//至少前面一个点可以走
                next = robot.pos + DIR[index];
                if (next == originNext) {
                    //如果随机出了原先下一个点，则站着不动
                    next = robot.pos;
                }
            }
            Point mid = (next + robot.pos) / 2;
            robot.path.clear();
            robot.path.push_back(GameMap::posToDiscrete(robot.pos));
            robot.path.push_back(GameMap::posToDiscrete(mid));
            robot.path.push_back(GameMap::posToDiscrete(next));
        }
        //回退
        for (const Point &point2: robotsAvoidOtherPoints[robot.id]) {
            gameMap.commonConflictPoints[point2.x][point2.y] = false;
        }
        //避让
        robot.avoidOtherTime = ROBOT_AVOID_OTHER_DURATION;

    }

    for (Robot &robot: robots) {
        robot.finish();
        if (robot.avoidOtherTime-- < 0) {
            //清空锁死点
            robotsAvoidOtherPoints[robot.id].clear();
        }
        if (robot.beConflicted-- < 0 && robot.forcePri != 0) {
            robot.forcePri = 0;
        }
    }
    long long int l4 = runTime();
    printError("frame:" + to_string(frameId) + ",robotDicision:" + to_string(l2 - l1) + ",path:" + to_string(l3 - l2) +
               ",other:" + to_string(l4 - l3));
}

void Strategy::sortRobots(Robot **tmpRobots) {
    sort(tmpRobots, tmpRobots + ROBOTS_PER_PLAYER, [](const Robot *o1, const Robot *o2) {
        if (o1->isPending ^ o2->isPending) {//暴力优先级最高
            return o1->isPending;//pending在前面，因为此时不动是最好的，等他pending结束再让
        }
        if (o1->forcePri != o2->forcePri) {//暴力优先级最高
            return o1->forcePri > o2->forcePri;
        }
        if (o1->redundancy ^ o2->redundancy) {//没冗余时间
            return !o1->redundancy;//不携带为true在前面
        }
        if (o1->carryValue != o2->carryValue) {
            //不携带一定是0
            return o1->carryValue > o2->carryValue;//看价值
        }
        return o1->priority != o2->priority ? o1->priority > o2->priority : o1->id < o2->id;
    });
    for (auto &path: robotsPredictPath) {
        path.clear();
    }
    for (Robot &robot: robots) {
        robot.avoid = false;
    }
}


bool Strategy::robotGreedyBuy1() {
    Robot *bestRobot = nullptr;
    Workbench *bestWorkBench = nullptr;
    Berth *bestWorkBerth = nullptr;
    for (Robot &robot: robots) {
        if (robot.buyAssign) {
            continue;
        }
        if (robot.isPending) {
            //答题状态优先决策
            assert (robot.targetWorkBenchId != -1);
            assert (robot.targetBerthId != -1);
            bestWorkBench = &workbenches[robot.targetWorkBenchId];
            bestWorkBerth = &berths[robot.targetBerthId];
            bestRobot = &robot;
            assignRobot(bestWorkBench, *bestWorkBerth, *bestRobot, RA_BUY);
            return true;
        }
    }
    double bestProfit = -GAME_FRAME;
    int count = 0;
    long long int curTime = runTime();
    if (curTime - robotStartDecisionBuyTime < 4) {
        robotMaxCount = int(valueList.size());
    } else {
        robotMaxCount = int(valueList.size()) / 4;//只决策前1/4;
    }
    for (const auto &item: valueList) {
        count++;
        if (IS_ONLINE && count > robotMaxCount) {
            break;
        }
        //todo，超过4ms只取前1/4
        if (item.first <= bestProfit * PROFIT_ZOOM_FACTOR) {
            //负收益没剪枝成功。。。,后面的只会更小
            break;
        }
        if (!workbenches.count(item.second)) {
            continue;//掉帧消失被我删除了
        }
        Workbench &buyWorkbench = workbenches[item.second];
        if (ONLY_BUY_PRECIOUS_WORKBENCH && buyWorkbench.value < PRECIOUS_WORKBENCH_BOUNDARY) {
            continue;
        }
        if (buyWorkbench.curMaxProfit <= bestProfit) {
            continue;
        } else if (buyWorkbench.maxProfitId != -1 && !robots[buyWorkbench.maxProfitId].buyAssign) {
            double profit = buyWorkbench.curMaxProfit;
            if (profit > 0 && robots[buyWorkbench.maxProfitId].targetWorkBenchId != -1 &&
                robots[buyWorkbench.maxProfitId].targetWorkBenchId != buyWorkbench.id &&
                !robots[buyWorkbench.maxProfitId].carry) {
                profit *= ROBOT_ANTI_JITTER;//降低点价值，防止抖动；
            }
            if (profit > bestProfit) {
                bestProfit = buyWorkbench.curMaxProfit;
                bestRobot = &robots[buyWorkbench.maxProfitId];
                bestWorkBench = &buyWorkbench;
                bestWorkBerth = &berths[buyWorkbench.minSellBerthId];
                continue;
            }
        }
        //存在就一定有产品
        if (workbenchesLock.count(buyWorkbench.id)) {
            continue;//别人选择过了
        }
        if (workbenchesPermanentLock.count(buyWorkbench.id)) {
            continue;//回答错误的workbench；
        }


        assert(buyWorkbench.minSellBerthId != -1);
        //贪心，选择最近的机器人
        Robot *selectRobot = nullptr;
        int minDist = SHORT_INF;
        for (Robot &robot: robots) {
            if (robot.buyAssign) {
                continue;
            }
            if (!buyWorkbench.canReach(robot.pos)) {
                continue; //不能到达
            }
            if (robotLock[robot.id].count(buyWorkbench.id)) {
                continue;
            }
            int dist = robotMinToWorkbenchDistance(robot, buyWorkbench);
            if (dist < minDist) {
                minDist = dist;
                selectRobot = &robot;
            }
        }
        //判断是否是最近的去买的,因为贪心，可以让他卖了再买，如果当前有卖的且距离更近，直接赋值null
        if (selectRobot == nullptr || minDist > buyWorkbench.remainTime) {
            buyWorkbench.curMaxProfit = -INT_INF;//后面的人一定也不能买
            continue;
        }
        int arriveBuyTime = minDist;
        Berth &sellBerth = berths[buyWorkbench.minSellBerthId];
        int arriveSellTime = sellBerth.getRobotMinDistance(buyWorkbench.pos);//机器人买物品的位置开始
        int collectTime = getFastestCollectTime(arriveBuyTime + arriveSellTime, sellBerth);
        int sellTime = collectTime + sellBerth.minSellDistance;
        double profit;
        double value = floor(1.0 * buyWorkbench.value / 2);//机器人只能拿一半价值
        if (frameId + sellTime > GAME_FRAME) {
            profit = -sellTime;//最近的去决策，万一到了之后能卖就ok，买的时候检测一下
        } else {
            //拿不完没消失价值
//            if (buyWorkbench.value < PRECIOUS_WORKBENCH_BOUNDARY) {
//                value += DISAPPEAR_REWARD_FACTOR * value * (WORKBENCH_EXIST_TIME - buyWorkbench.remainTime) /
//                         WORKBENCH_EXIST_TIME;
//            }
            profit = value / (arriveBuyTime + arriveSellTime);//不算ToBerth时间,因为是从泊位出发到工作台
            //如果没到，随时可以换目标，没有惩罚，
            //拿不完，随便换目标，只要收益大
//            if (selectRobot->targetWorkBenchId == buyWorkbench.id && !selectRobot->carry) {
//                profit *= (1 + SAME_TARGET_REWARD_FACTOR);
//            }
        }
        if (OTHER_ROBOT_DAMPING && profit > 0 && profit > bestProfit &&
            buyWorkbench.value > PRECIOUS_WORKBENCH_BOUNDARY) {
            //普通货物不衰减，因为很多人不拿
            //浪费时间估计
            int estimateOtherComeCount = 0;
            for (const auto &other: totalRobots) {
                //对面机器人越多，越说明可能贵重物品很少，应该没事
                if (other.belongToMe) {
                    continue;
                }
                if (other.num >= other.maxNum) {
                    continue;
                }
                if (buyWorkbench.getMinDistance(other.p) > arriveBuyTime) {
                    continue;
                }
                if (buyWorkbench.getMinDistance(other.p) >= buyWorkbench.getMinDistance(other.lastP)) {
                    continue;
                }
                estimateOtherComeCount++;
            }
            profit = value / (arriveBuyTime + OTHER_ROBOT_ADD_TIME_FACTOR
                                              * estimateOtherComeCount
                                              * arriveBuyTime +
                              arriveSellTime);
        }
        buyWorkbench.curMaxProfit = profit;
        buyWorkbench.maxProfitId = selectRobot->id;
        if (profit > 0 && selectRobot->targetWorkBenchId != -1 &&
            selectRobot->targetWorkBenchId != buyWorkbench.id &&
            !robots[buyWorkbench.maxProfitId].carry) {
            profit *= ROBOT_ANTI_JITTER;//降低点价值，防止抖动；远的可能较大，没关系，不会排除
        }
        if (profit > bestProfit) {
            bestProfit = profit;
            bestRobot = selectRobot;
            bestWorkBench = &buyWorkbench;
            bestWorkBerth = &sellBerth;
        }
    }
    if (bestRobot == nullptr)
        return false;
    assignRobot(bestWorkBench, *bestWorkBerth, *bestRobot, RA_BUY);
    return true;
}

bool Strategy::robotGreedyBuy2() {
    Robot *bestRobot = nullptr;
    Workbench *bestWorkBench = nullptr;
    Berth *bestWorkBerth = nullptr;
    for (Robot &robot: robots) {
        if (robot.buyAssign) {
            continue;
        }
        if (robot.isPending) {
            //答题状态优先决策
            assert (robot.targetWorkBenchId != -1);
            assert (robot.targetBerthId != -1);
            bestWorkBench = &workbenches[robot.targetWorkBenchId];
            bestWorkBerth = &berths[robot.targetBerthId];
            bestRobot = &robot;
            assignRobot(bestWorkBench, *bestWorkBerth, *bestRobot, RA_BUY);
            return true;
        }
    }
    double bestProfit = -GAME_FRAME;
    int count = 0;
    long long int curTime = runTime();
    if (curTime - robotStartDecisionBuyTime < 4) {
        robotMaxCount = int(valueList.size());
    } else {
        robotMaxCount = int(valueList.size()) / 4;//只决策前1/4;
    }
    for (const auto &item: valueList) {
        count++;
        if (IS_ONLINE && count > robotMaxCount) {
            break;
        }
        if (item.first <= bestProfit * PROFIT_ZOOM_FACTOR) {
            //负收益没剪枝成功。。。,后面的只会更小
            break;
        }
        if (!workbenches.count(item.second)) {
            continue;//掉帧消失被我删除了
        }
        Workbench &buyWorkbench = workbenches[item.second];
        if (ONLY_BUY_PRECIOUS_WORKBENCH && buyWorkbench.value < PRECIOUS_WORKBENCH_BOUNDARY) {
            continue;
        }
        if (buyWorkbench.curMaxProfit <= bestProfit) {
            continue;
        } else if (buyWorkbench.maxProfitId != -1 && !robots[buyWorkbench.maxProfitId].buyAssign) {
            double profit = buyWorkbench.curMaxProfit;
            if (profit > bestProfit) {
                bestProfit = buyWorkbench.curMaxProfit;
                bestRobot = &robots[buyWorkbench.maxProfitId];
                bestWorkBench = &buyWorkbench;
                bestWorkBerth = &berths[buyWorkbench.minSellBerthId];
                continue;
            }
        }
        //存在就一定有产品
        if (workbenchesLock.count(buyWorkbench.id)) {
            continue;//别人选择过了
        }
        if (workbenchesPermanentLock.count(buyWorkbench.id)) {
            continue;//回答错误的workbench；
        }
        assert(buyWorkbench.minSellBerthId != -1);
        //贪心，选择最近的机器人
        Robot *selectRobot = nullptr;
        int minDist = SHORT_INF;
        for (Robot &robot: robots) {
            if (robot.buyAssign) {
                continue;
            }
            if (!buyWorkbench.canReach(robot.pos)) {
                continue; //不能到达
            }
            if (robotLock[robot.id].count(buyWorkbench.id)) {
                continue;
            }
            int dist = robotMinToWorkbenchDistance(robot, buyWorkbench);
            if (dist < minDist) {
                minDist = dist;
                selectRobot = &robot;
            }
        }
        //判断是否是最近的去买的,因为贪心，可以让他卖了再买，如果当前有卖的且距离更近，直接赋值null
        if (selectRobot == nullptr || minDist > buyWorkbench.remainTime) {
            buyWorkbench.curMaxProfit = -INT_INF;//后面的人一定也不能买
            continue;
        }
        int arriveBuyTime = minDist;
        Berth &sellBerth = berths[buyWorkbench.minSellBerthId];
        int arriveSellTime = sellBerth.getRobotMinDistance(buyWorkbench.pos);//机器人买物品的位置开始
        int collectTime = getFastestCollectTime(arriveBuyTime + arriveSellTime, sellBerth);
        int sellTime = collectTime + sellBerth.minSellDistance;
        double profit;
        double value = floor(1.0 * buyWorkbench.value / 2);//机器人只能拿一半价值
        if (frameId + sellTime > GAME_FRAME) {
            profit = -sellTime;//最近的去决策，万一到了之后能卖就ok，买的时候检测一下
        } else {
            //拿不完没消失价值
            profit = value / (arriveBuyTime + arriveSellTime);//不算ToBerth时间,因为是从泊位出发到工作台
            //如果没到，随时可以换目标，没有惩罚，
            //拿不完，随便换目标，只要收益大
            if (selectRobot->targetWorkBenchId == buyWorkbench.id && !selectRobot->carry) {
                profit *= (1 + SAME_TARGET_REWARD_FACTOR);
            }
        }
        buyWorkbench.curMaxProfit = profit;
        if (robotLastFrameSelectIds.count(buyWorkbench.id)) {
            if (buyWorkbench.curMaxProfit > 0) {
                buyWorkbench.curMaxProfit *= (1 + SAME_TARGET_REWARD_FACTOR);
            } else {
                buyWorkbench.curMaxProfit = 0;
            }
        }
        buyWorkbench.maxProfitId = selectRobot->id;
        if (profit > bestProfit) {
            bestProfit = profit;
            bestRobot = selectRobot;
            bestWorkBench = &buyWorkbench;
            bestWorkBerth = &sellBerth;
        }
    }

    if (bestRobot == nullptr)
        return false;
    assignRobot(bestWorkBench, *bestWorkBerth, *bestRobot, RA_BUY);
    return true;
}

bool Strategy::robotGreedySell() {

    Robot *bestRobot = nullptr;
    Berth *bestBerth = nullptr;
    double bestProfit = -GAME_FRAME;
    for (Robot &robot: robots) {
        if (robot.assigned || !robot.carry) {
            continue;
        }

        //选择折现价值最大的
        Berth *select = nullptr;
        double maxProfit = -GAME_FRAME;
        for (Berth &sellBerth: berths) {
            if (!sellBerth.robotCanReach(robot.pos)) {
                continue; //不能到达
            }
            if (berthLock.count(sellBerth.id)) {
                continue;
            }
            //assert fixTime != Integer.MAX_VALUE;
            int arriveTime = sellBerth.getRobotMinDistance(robot.pos);
            double profit;
            //包裹被揽收的最小需要的时间
            int collectTime = getFastestCollectTime(arriveTime - 1, sellBerth);
            int sellTime = collectTime + sellBerth.minSellDistance;
            if (frameId + sellTime >= GAME_FRAME) {
                //如果不能到达，收益为负到达时间
                profit = -sellTime;
            } else {
                double value = robot.carryValue;
//                    value += estimateEraseValue(sellTime, robot, sellBerth);
                //防止走的特别近马上切泊位了
                profit = value / arriveTime;
                if (robot.targetBerthId == sellBerth.id) {//同一泊位
                    profit *= (1 + SAME_TARGET_REWARD_FACTOR);
                }
            }

            if (profit > maxProfit) {
                maxProfit = profit;
                select = &sellBerth;
            }
        }
        if (select != nullptr && maxProfit > bestProfit) {
            bestRobot = &robot;
            bestBerth = select;
            bestProfit = maxProfit;
        }
    }
    if (bestRobot == nullptr)
        return false;
    assignRobot(nullptr, *bestBerth, *bestRobot, RA_SELL);
    return true;
}

void Strategy::assignRobot(Workbench *workbench_, Berth &berth, Robot &robot, RobotAction action) {
    robot.assigned = true;
    if (!robot.carry || action == RA_SELL) {
        //没携带物品，或者是卖，需要改变目标
        robot.targetBerthId = berth.id;
    }
    if (action == RA_BUY) {
        robot.buyAssign = true;
        Workbench &workbench = *workbench_;
        workbenchesLock.insert(workbench.id);//锁住，别人不准选择
        int robotLastFrameId = robot.targetWorkBenchId;
        robot.targetWorkBenchId = workbench.id;
        if (!robot.carry) {
            int toWorkbenchDist = robotMinToWorkbenchDistance(robot, workbench);
            robot.redundancy = toWorkbenchDist != workbench.remainTime;
        }
        robot.priority = workbench.value;
        if (!robot.carry && robot.pos == workbench.pos && !robot.isPending) {
            //没带物品，且到目标，一定买，然后重新决策一个下一个目标买,已经pending没必要再get
            //上一帧移动成功，且发了buy指令，这种时候没有买到，说明一定被别人拿了
            if (workbench.id == robotLastFrameId) {
                //上一帧的目标还是他，因为一定提前buy，此时如果没进入pending或者买到了，说明一定是别人拿走了
                gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] = -1;
                workbenches.erase(workbench.id);//重新决策
                robot.buyAssign = false;
                robot.assigned = false;
                return;
            }
            robot.buy();
            if (workbench.value < PRECIOUS_WORKBENCH_BOUNDARY) {
                robot.buyAssign = false;
            }
            if (!robot.carry) {
                robot.assigned = false;
            }
        }
    } else {
        robot.redundancy = true;
        if (gameMap.getBelongToBerthId(robot.pos) == berth.id) {
            printError("error,no pre sell");
            robot.pull();
        }
    }
}

int Strategy::getFastestCollectTime(int goodArriveTime, Berth &berth) {
    //这个泊位这个物品到达的时候泊位的数量
    return goodArriveTime;
//    int estimateGoodsNums = berth.goodsNums;
//    estimateGoodsNums++;//加上这个货物时间
//    int minDistance = INT_INF;
//    for (auto &boat: boats) {
//        if (boat.targetBerthId == berth.id) {
//            //来的是同一搜
//            int comingTime = boatMinToBerthDistance(boat, berth);
//            int remainSpace = boat.carry ? boat.capacity : boat.capacity - boat.num;
//            if (remainSpace >= estimateGoodsNums) {
//                minDistance = min(minDistance, comingTime);
//            } else {
//                minDistance = min(minDistance, comingTime + 2 * berth.minSellDistance);
//            }
//        } else {
//            int remainSpace = boat.carry ? boat.capacity : boat.capacity - boat.num;
//            int id = boat.targetBerthId == -1 ? 0 : boat.targetBerthId;
//            int comingTime = boatMinToBerthDistance(boat, berths[id]);
//            int changeBerthTime = boatMinBerthToBerthDistance(berths[id], berth);
//            remainSpace -= berths[id].goodsNums;
//            //去到估计的位置，在加一来一回
//            if (remainSpace >= estimateGoodsNums) {
//                //可能切换，也可能不切换
//                minDistance = min(minDistance, comingTime + changeBerthTime);
//            } else {
//                //去另一个泊位装满，再回去，再去你这里
//                minDistance = min(minDistance, comingTime + berths[id].minSellDistance + berth.minSellDistance);
//            }
//        }
//    }
//
//
//    //检查所有的船，找出这个泊位的这个物品最快被什么时候消费
//    //没到达也没法消费
//    return max(goodArriveTime, minDistance);
}


int Strategy::robotMinToWorkbenchDistance(Robot &robot, Workbench &workbench) {
    if (robot.carry) {
        assert (robot.assigned);
        int toBerth = berths[robot.targetBerthId].getRobotMinDistance(robot.pos);
        int toWorkBench = berths[robot.targetBerthId].getRobotMinDistance(workbench.pos);
        toWorkBench += 2;//最坏需要再走两帧
        return toWorkBench + toBerth;
    }
    return workbench.getMinDistance(robot.pos);
}


void Strategy::updateMap() {
    if (berths[0].corePoint == Point(3, 98) && berths[1].corePoint == Point(92, 3)) {
        isMap1 = true;
        return;
    }
    if (berths[0].corePoint == Point(98, 34) && berths[1].corePoint == Point(98, 60)) {
        isMap2 = true;
        return;
    }
    isMap3 = true;
}

void Strategy::boatUpdateFlashPoint(int i, int j) {
    int minDistance = INT_INF;
    int maxRight = 0;
    int maxLeft = 0;
    int maxTop = 0;
    int maxBottom = 0;
    Point *bestPoint = nullptr;
    for (Point &candidate: boatFlashCandidates) {
        int curDistance = abs(candidate.x - i) + abs(candidate.y - j);
        if (curDistance > minDistance) {
            continue;//大于立马排除
        }
        //后面按照哪个方向计算
        int curRight = max(candidate.y - j, 0);
        int curLeft = max(j - candidate.y, 0);
        int curTop = max(i - candidate.x, 0);
        int curBottom = max(candidate.x - i, 0);
        if (curDistance < minDistance || curRight > maxRight ||
            (curRight == maxRight && curLeft > maxLeft) ||
            (curRight == maxRight && curLeft == maxLeft && curTop > maxTop)
            || (curRight == maxRight && curLeft == maxLeft
                && curTop == maxTop && curBottom > maxBottom)) {
            bestPoint = &candidate;
            minDistance = curDistance;
            maxRight = curRight;
            maxLeft = curLeft;
            maxTop = curTop;
            maxBottom = curBottom;
        }
    }
    // 找到最好的点
    assert(bestPoint != nullptr);
    for (int k = 0; k < (sizeof DIR / sizeof DIR[0]) / 2; k++) {
        if (gameMap.boatIsAllInMainChannel(*bestPoint, k)) {
            //找到传送点
            boatFlashMainChannelPoint[i][j] = PointWithDirection(*bestPoint, k);
        }
    }
}


PointWithDirection Strategy::getBoatFlashDeptPoint(Point point) {
    if (boatFlashMainChannelPoint[point.x][point.y].direction == -1) {
        boatUpdateFlashPoint(point.x, point.y);
    }
    return boatFlashMainChannelPoint[point.x][point.y];
}

void Strategy::sortBoats(Boat *tmpBoats[]) {
    sort(tmpBoats, tmpBoats + BOATS_PER_PLAYER, [](const Boat *o1, const Boat *o2) {
        if (o1->forcePri != o2->forcePri) {//暴力优先级最高
            return o1->forcePri > o2->forcePri;
        }
        if (o1->num != o2->num) {//没冗余时间
            return o1->num > o2->num;//不携带为true在前面
        }
        if (o1->assigned ^ o2->assigned) {
            //有路径的放前面
            return o1->assigned;
        }
        return o1->id < o2->id;
    });
    for (Boat &boat: boats) {
        boat.avoid = false;
    }
}

bool Strategy::boatGreedySell() {
    struct Stat {
        Boat *boat;
        BoatSellPoint *boatSellPoint;
        double profit;
    public:
        Stat(Boat *boat, BoatSellPoint *boatSellPoint, double profit) {
            this->boat = boat;
            this->boatSellPoint = boatSellPoint;
            this->profit = profit;
        }

        bool operator<(const Stat &b) const {
            return this->profit > b.profit;
        }
    };

    vector<Stat> stat;
    for (Boat &boat: boats) {
        if (boat.assigned || boat.num == 0) {
            continue;
        }
        int minSellDistance = INT_INF;
        BoatSellPoint *selectPoint = nullptr;
        for (BoatSellPoint &boatSellPoint: boatSellPoints) {
            if (!boatSellPoint.canReach(boat.corePoint, boat.direction)) {
                continue;
            }
            int distance = boatSellPoint.getMinDistance(boat.corePoint, boat.direction);
            //避让时间,稍微增加一帧
            distance += boat.remainRecoveryTime;
            if (distance < minSellDistance) {
                minSellDistance = distance;
                selectPoint = &boatSellPoint;
            }
        }
        if (selectPoint == nullptr) {
            continue;
        }
        if (boat.num == boat.capacity ||
            frameId + minSellDistance >= GAME_FRAME - min(FPS / 2 + 5, int(boats.size() * 5))) {
            //卖
            int value = boat.num;
            double profit = 1.0 * value / minSellDistance;
            stat.emplace_back(&boat, selectPoint, profit);
        }
    }

    if (stat.empty()) {
        return false;
    }
    sort(stat.begin(), stat.end());

    Boat &boat = *stat[0].boat;
    BoatSellPoint &boatSellPoint = *stat[0].boatSellPoint;
    boat.assigned = true;
    boat.targetSellId = boatSellPoint.id;

    if (!boat.carry) {
        boat.sellCount++;
    }
    boat.carry = true;


    if (boat.status == 2) {
        boat.status = 0;//离开泊位
        boat.targetBerthId = -1;
    }
    return true;
}

int Strategy::boatMinToBerthDistance(Boat &boat, Berth &berth) {
    int recoveryTime = boat.remainRecoveryTime;
    if (boat.carry) {
        //卖
        assert(boat.targetSellId != -1);
        int toSellDistance = boatSellPoints[boat.targetSellId].getMinDistance(boat.corePoint, boat.direction);
        int toBerthDistance = 0;
        return recoveryTime + toSellDistance + toBerthDistance;
    }
    return recoveryTime + berth.getBoatMinDistance(boat.corePoint, boat.direction);
}


void Strategy::robotAndBoatBuy() {
    if (money < ROBOT_PRICE) {
        return;
    }
    int type0Size = 0;
    int type1Size = 0;
    for (Robot &robot: robots) {
        if (robot.type == 0) {
            type0Size++;
        } else {
            type1Size++;
        }
    }
    type1Size += int(pendingRobots.size());

    while (type0Size < INIT_ROBOT_TYPE0_COUNT && money >= ROBOT_PRICE) {
        buyRobot(0);
        type0Size++;
    }
    while (type1Size < INIT_ROBOT_TYPE1_COUNT && money >= ROBOT_TYPE1_PRICE) {
        buyRobot(1);
        type1Size++;
    }
    while (boats.size() < INIT_BOAT_COUNT && money >= BOAT_PRICE) {
        if (!buyBoat(0)) {
            //下一帧再说
            return;
        }
    }
    while (type0Size < MIN_ROBOT_TYPE0_COUNT && money >= ROBOT_PRICE) {
        buyRobot(0);
        type0Size++;
    }
    while (type1Size < MIN_ROBOT_TYPE1_COUNT && money >= ROBOT_TYPE1_PRICE) {
        buyRobot(1);
        type1Size++;
    }
    while (boats.size() < MIN_BOAT_COUNT && (money >= (FUTURE_BUY_BOAT_TYPE == 0 ? BOAT_PRICE
                                                                                 : BOAT_TYPE1_PRICE))) {
        if (!buyBoat(FUTURE_BUY_BOAT_TYPE)) {
            //可能本帧直接买会撞，买不到,下一帧再说
            return;
        }
    }
    if (money < ROBOT_PRICE || GAME_FRAME == frameId) {
        return;
    }
    //动态决定是否买机器人和船
    int goodCounts = totalValidWorkBenchCount;
    int remainCount = int(workbenches.size());
    //剩余估计要来的count
    double speed = 1.0 * (double) goodCounts / (double) frameId;
    double estimateRemainTotalCount = speed * (GAME_FRAME - frameId);
    double curRobotCount = 0;//折算的个数
    for (Robot &robot: robots) {
        curRobotCount += frameId - robot.buyFrame + 1;
    }
    curRobotCount /= (double) frameId;

    //估计最大机器人数
    estimateMaxRobotCount = (estimateRemainTotalCount + remainCount) * avgWorkBenchLoopDistance /
                            (GAME_FRAME - frameId);
    double remainTotalValue = (estimateRemainTotalCount + remainCount) * goodAvgValue;
    double curCanGetValue = min(remainTotalValue,
                                1.0 * pullScore / 2 / totalValue * remainTotalValue *
                                int(robots.size())
                                / curRobotCount);//只能拿一半价值
    remainTotalValue -= curCanGetValue;
    int buyRobotCount = 0;
    double curRemainValue = remainTotalValue;
    while ((int) robots.size() + int(pendingRobots.size()) + buyRobotCount < estimateMaxRobotCount) {
        double oneRobotValue = curRemainValue * 1.0 * pullScore / 2 / totalValue / curRobotCount;
        if (oneRobotValue > BUY_ROBOT_FACTOR * ROBOT_PRICE) {
            buyRobotCount++;
            curRemainValue -= oneRobotValue * 2;//实际少了两倍的价值
        } else {
            break;
        }
    }
    //决定是否买船

    int berthRemainNum = 0;
    for (Berth &berth: berths) {
        if (berthLock.count(berth.id)) {
            continue;
        }
        berthRemainNum += berth.goodsNums;
        if (berth.curBoatGlobalId != -1 && !totalBoats[berth.curBoatGlobalId].belongToMe) {
            int needCount = boatCapacity - totalBoats[berth.curBoatGlobalId].num;
            berthRemainNum -= needCount;
        }
    }
    for (Boat &boat: boats) {
        if (!boat.carry && boat.targetBerthId != -1) {
            int needCount = boat.capacity - boat.num;
            berthRemainNum -= needCount;
        }
    }
    int berthTotalNum = totalPullGoodsCount;//只看自己丢的货物
    //估计未来到达的货物
    double berthSpeed = 1.0 * berthTotalNum / frameId;
    double estimateBerthComingGood = 1.0 * berthSpeed * (GAME_FRAME - frameId) * int(robots.size()) / curRobotCount;
    estimateMaxBoatCount = (estimateBerthComingGood + berthRemainNum) / boatCapacity * avgBerthLoopDistance
                           / (GAME_FRAME - frameId);
    double oneBoatValue = boatCapacity * avgPullGoodsValue;
    double oneBoatCanGetValue = floor(oneBoatValue * (GAME_FRAME - frameId) / avgBerthLoopDistance);
    int buyBoatCount = 0;
    int boatRealSize = 0;
    for (Boat &boat: boats) {
        if (boat.capacity == boatCapacity) {
            boatRealSize++;
        } else {
            boatRealSize += boatCapacity1 / boatCapacity;
        }
    }
    if (FUTURE_BUY_BOAT_TYPE == 1) {
        boatRealSize += int((boatCapacity1 / boatCapacity)) * int(pendingBoats.size());
    } else {
        boatRealSize += int(pendingBoats.size());
    }
    while (int(boatRealSize) + int(pendingBoats.size()) + buyBoatCount < estimateMaxBoatCount) {
        double oneBoatRealValue =
                oneBoatCanGetValue * min(estimateMaxBoatCount - buyBoatCount - double(boats.size()), 1.0);
        if (oneBoatRealValue / 2 > BUY_BOAT_FACTOR * BOAT_PRICE) {//实际只赚了一半的钱
            buyBoatCount++;
        } else {
            break;
        }
    }
    if (FUTURE_BUY_BOAT_TYPE == 1) {
        buyBoatCount = ceil(1.0 * buyBoatCount * boatCapacity / boatCapacity1);
    }
    while (buyBoatCount > 1) {
        if (money >= (FUTURE_BUY_BOAT_TYPE == 0 ? BOAT_PRICE
                                                : BOAT_TYPE1_PRICE)) {
            if (buyBoat(FUTURE_BUY_BOAT_TYPE)) {
                buyBoatCount--;
            } else {
                return;
            }
        } else {
            //此时钱不够，必须得等钱来
            return;
        }
    }
    //后面要买一定买type1
    while (buyRobotCount > 0 && (money >= (FUTURE_BUY_ROBOT_TYPE == 0 ? ROBOT_PRICE
                                                                      : ROBOT_TYPE1_PRICE))) {
        buyRobot(FUTURE_BUY_ROBOT_TYPE);
        buyRobotCount--;
    }

    while (buyBoatCount == 1) {
        if (money >= (FUTURE_BUY_BOAT_TYPE == 0 ? BOAT_PRICE
                                                : BOAT_TYPE1_PRICE)) {
            if (buyBoat(FUTURE_BUY_BOAT_TYPE)) {
                buyBoatCount--;
            } else {
                return;
            }
        } else {
            //此时钱不够，必须得等钱来
            return;
        }
    }

}

bool Strategy::buyBoat(int type) {
    int index = getBestBoatBuyPos();
    if (index == -1) {
        return false;
    }
    printf("lboat %d %d %d\n", boatPurchasePoint[index].x, boatPurchasePoint[index].y, type);

    if (type == 0) {
        money -= BOAT_PRICE;
        boats.emplace_back(this, boatCapacity);
    } else {
        money -= BOAT_TYPE1_PRICE;
        boats.emplace_back(this, boatCapacity1);
    }
    return true;
}

void Strategy::buyRobot(int type) {
    int index = getBestBuyRobotPos();
    printf("lbot %d %d %d\n", robotPurchasePoint[index].x, robotPurchasePoint[index].y, type);
    robots.emplace_back(this, type);
    robotPurchaseCount[index] = robotPurchaseCount[index] + 1;
    robots.back().buyFrame = frameId;
    if (type == 0) {
        money -= ROBOT_PRICE;
    } else {
        money -= ROBOT_TYPE1_PRICE;
    }
}

int Strategy::getBestBoatBuyPos() {
    //货物最多的berth
    int remainGoodsCount[BERTH_PER_PLAYER];
    for (Berth &berth: berths) {
        remainGoodsCount[berth.id] = berth.goodsNums;
    }
    for (Boat &boat: boats) {
        if (boat.targetBerthId != -1) {
            int needCount = boat.carry ? boat.capacity : boat.capacity - boat.num;
            remainGoodsCount[boat.targetBerthId] -= needCount;
        }
    }
    int maxCount = -GAME_FRAME;
    int bestBerthId = -1;
    for (int i = 0; i < BERTH_PER_PLAYER; i++) {
        if (remainGoodsCount[i] > maxCount) {
            maxCount = remainGoodsCount[i];
            bestBerthId = i;
        }
    }
    int bestI = -1;
    int minDistance = SHORT_INF;
    for (int i = 0; i < boatPurchasePoint.size(); i++) {
        Point &point = boatPurchasePoint[i];
        int distance = berths[bestBerthId].getBoatMinDistance(point, 0);
        if (distance < minDistance) {
            minDistance = distance;
            bestI = i;
        }
    }
    //冲突检测
    for (Boat &boat: boats) {
        PointWithDirection buyPoint(boatPurchasePoint[bestI], 0);
        if (BoatUtils::boatCheckCrash(gameMap, buyPoint, {boat.corePoint, boat.direction})) {
            bestI = -1;//这一帧，买不了
            break;
        }
    }
    return bestI;
}

int Strategy::getBestBuyRobotPos() {
    double maxProfit = 0;
    int bestI = -1;
    int bestWorkBenchId = -1;
    for (auto &pair: workbenches) {
        Workbench &buyWorkbench = pair.second;
        if (buyWorkbench.value < PRECIOUS_WORKBENCH_BOUNDARY && workbenches.size() > 1000) {
            continue;//只买贵重物品
        }
        if (workbenchesLock.count(buyWorkbench.id)) {
            continue;//别人选择过了
        }
        short buyTime = SHORT_INF;
        int curI = -1;
        for (int i = 0; i < robotPurchasePoint.size(); i++) {
            //第一帧靠运气整几个，后面1 2 3 4，因为没有memset，值是不确定的
            Point &point = robotPurchasePoint[i];
            short curBuyTime = buyWorkbench.getMinDistance(point);
            if (curBuyTime < buyTime) {
                buyTime = curBuyTime;
                curI = i;
            }
        }

        int sellTime = buyWorkbench.minSellDistance;
        if (frameId + buyTime + sellTime > GAME_FRAME) {
            continue;
        }
        double value = buyWorkbench.value;
        double profit = value / (double) (buyTime + sellTime);
        if (profit > maxProfit) {
            maxProfit = profit;
            bestI = curI;
            bestWorkBenchId = buyWorkbench.id;
        }
    }

    if (bestWorkBenchId != -1) {
        workbenchesLock.insert(bestWorkBenchId);
    } else {
        //咋搞
        int minBuyCount = INT_INF;
        for (int i = 0; i < robotPurchaseCount.size(); i++) {
            if (robotPurchaseCount[i] < minBuyCount) {
                minBuyCount = robotPurchaseCount[i];
                bestI = i;
            }
        }
        //todo 可以尝试改成随机？
//        int minBuyCount = INT_INF;
//        vector<int> minCountIds;
//        for (int i = 0; i < robotPurchaseCount.size(); i++) {
//            if (robotPurchaseCount[i] < minBuyCount) {
//                minBuyCount = robotPurchaseCount[i];
//                minCountIds.clear();
//                minCountIds.push_back(i);
//            }
//            if (robotPurchaseCount[i] == minBuyCount) {
//                minCountIds.push_back(i);
//            }
//        }
//        unsigned int index = e() % int(minCountIds.size());
//        bestI = minCountIds[index];

    }
    return bestI;
}

void Strategy::decisionBoatBuy(Boat &boat, Berth &berth, BoatSellPoint &boatSellPoint,
                               int goodsNumList[MAX_BERTH_PER_PLAYER]) {
    boat.targetBerthId = berth.id;
    if (!boat.carry) {
        boat.targetSellId = boatSellPoint.id;
    }
    boat.assigned = true;
    boat.buyAssign = true;
    int needCount = boat.capacity - boat.num;
    goodsNumList[berth.id] -= needCount;
}

int Strategy::boatMinBerthToBerthDistance(Berth &fromBerth, Berth &toBerth) {
    return toBerth.getBoatMinDistance(fromBerth.corePoint, fromBerth.coreDirection);
}

int Strategy::boatGetMinSellPointIndex(Berth &berth) {
    int minDistance = INT_INF;
    int bestIndex = -1;
    for (int i = 0; i < boatSellPoints.size(); i++) {
        BoatSellPoint &boatSellPoint = boatSellPoints[i];
        int distance = boatSellPoint.getMinDistance(berth.corePoint, berth.coreDirection);
        if (distance < minDistance) {
            minDistance = distance;
            bestIndex = i;
        }
    }
    return bestIndex;
}

vector<Point> Strategy::robotToBerthHeuristic(Robot &robot, Berth &berth, const vector<vector<Point>> &otherPaths,
                                              bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
    auto maxDeep = (short) (berths[berth.id].robotMinDistance[robot.pos.x][robot.pos.y] >> 2);
    if (conflictPoints != nullptr) {
        maxDeep += ROBOT_AVOID_OTHER_DEEP;//多5帧能让对面也行,不然只能随机了
    }
    return RobotUtils::robotMoveToPointBerthHeuristicCs(gameMap, robot.pos, berth.id, {}, maxDeep, otherPaths,
                                                        berth.robotMinDistance, conflictPoints); //考虑
}

vector<Point>
Strategy::robotToWorkBenchHeuristic(Robot &robot, Workbench &workbench, const vector<vector<Point>> &otherPaths,
                                    bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
    short  (*robotCommonHeuristicCs)[MAP_FILE_COL_NUMS];
    //复用逻辑判断，保证一个workbench几乎只走一次
    if (gameMap.robotUseHeuristicCsWbIds.count(workbench.id)) {
        int index = gameMap.robotUseHeuristicCsWbIds[workbench.id];
        robotCommonHeuristicCs = gameMap.robotCommonHeuristicCs[index];
    } else {
        int pre = gameMap.robotUseHeuristicCsWbIdList.front();
        gameMap.robotUseHeuristicCsWbIdList.pop();
        int index = gameMap.robotUseHeuristicCsWbIds[pre];
        gameMap.robotUseHeuristicCsWbIds.erase(pre);
        gameMap.robotUseHeuristicCsWbIds[workbench.id] = index;
        gameMap.robotUseHeuristicCsWbIdList.push(workbench.id);
        robotCommonHeuristicCs = gameMap.robotCommonHeuristicCs[index];
        workbench.setHeuristicCs(robotCommonHeuristicCs);
    }
    auto maxDeep = (short) (robotCommonHeuristicCs[robot.pos.x][robot.pos.y] >> 2);
    if (conflictPoints != nullptr) {
        maxDeep += ROBOT_AVOID_OTHER_DEEP;
    }
    return RobotUtils::robotMoveToPointBerthHeuristicCs(gameMap, robot.pos, -1, workbench.pos, maxDeep, otherPaths,
                                                        robotCommonHeuristicCs, conflictPoints);
}

vector<PointWithDirection> Strategy::boatToAnyPoint(Boat &boat, PointWithDirection target) {
    int maxDeep = INT_INF;
    return BoatUtils::boatMoveToBerthSellPoint(gameMap, {boat.corePoint, boat.direction}, target, -1, {},
                                               boat.remainRecoveryTime, maxDeep, boat.id, {}, {}, nullptr,
                                               nullptr);
}

vector<PointWithDirection> Strategy::boatToSellPointHeuristic(Boat &boat, BoatSellPoint &boatSellPoint,
                                                              const vector<vector<PointWithDirection>> &otherPaths,
                                                              const vector<int> &otherIds, bool
                                                              conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
    short(*heuristicCs)[MAP_FILE_COL_NUMS][(sizeof DIR / sizeof DIR[0]) / 2] = boatSellPoint.boatMinDistance;
    int maxDeep = (heuristicCs[boat.corePoint.x][boat.corePoint.y][boat.direction] >> 2);
    if (!otherPaths.empty()) {
        maxDeep += BOAT_FIND_PATH_DEEP;//避让自己深度
    }
    if (conflictPoints != nullptr) {
        maxDeep += BOAT_AVOID_OTHER_DEEP;
    }
    return BoatUtils::boatMoveToBerthSellPointHeuristic(gameMap, {boat.corePoint, boat.direction},
                                                        {boatSellPoint.point, -1}, -1, {}, boat.remainRecoveryTime,
                                                        maxDeep, boat.id, otherPaths, otherIds, heuristicCs,
                                                        conflictPoints);
}

vector<PointWithDirection>
Strategy::boatToBerthHeuristic(Boat &boat, Berth &berth, const vector<vector<PointWithDirection>> &otherPaths,
                               const vector<int> &otherIds, bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
    short(*heuristicCs)[MAP_FILE_COL_NUMS][(sizeof DIR / sizeof DIR[0]) / 2] = berth.boatMinDistance;
    int maxDeep = (heuristicCs[boat.corePoint.x][boat.corePoint.y][boat.direction] >> 2);
    if (!otherPaths.empty()) {
        maxDeep += BOAT_FIND_PATH_DEEP;//避让自己深度
    }
    if (conflictPoints != nullptr) {
        maxDeep += BOAT_AVOID_OTHER_DEEP;//减去一点点，太大也不好
    }
    return BoatUtils::boatMoveToBerthSellPointHeuristic(gameMap, {boat.corePoint, boat.direction}, {}, berth.id,
                                                        {berth.corePoint, berth.coreDirection}, boat.remainRecoveryTime,
                                                        maxDeep, boat.id, otherPaths, otherIds, heuristicCs,
                                                        conflictPoints);
}


