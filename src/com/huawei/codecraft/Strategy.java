package com.huawei.codecraft;

import java.io.IOException;
import java.util.*;

import static com.huawei.codecraft.BoatDecisionType.DECISION_ON_ORIGIN;
import static com.huawei.codecraft.BoatStatus.*;
import static com.huawei.codecraft.BoatUtils.*;
import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.RobotAction.RA_BUY;
import static com.huawei.codecraft.RobotAction.RA_SELL;
import static com.huawei.codecraft.RobotUtils.*;
import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.*;

public class Strategy {

    //是否拿倒数第一参数
    int lastSelectRobotId = -1;
    int lastSelectWorkbenchId = -1;
    int lastSelectBerthId = -1;
    int lastValueThreshold = 1;
    boolean lastRobotDoAction = false;
    public int pullScore;//船把所有货物都收了的极限分
    @SuppressWarnings("all")
    private int money;
    public GameMap gameMap;
    public static int workbenchId = 0;
    public final HashMap<Integer, Workbench> workbenches = new HashMap<>();
    public final HashSet<Integer> workbenchesLock = new HashSet<>();//锁住某些工作台

    public int totalValue = 0;
    public int goodAvgValue = 0;
    public int totalPullGoodsValues = 0;//总的卖的货物价值
    public int totalPullGoodsCount = 0;//总的卖的货物价值
    public double avgPullGoodsValue = 150;//总的卖的货物价值
    public double avgPullGoodsTime = 15;//10个泊位平均卖货时间

    public final Robot[] robots = new Robot[MAX_ROBOTS_PER_PLAYER];
    @SuppressWarnings("unchecked")
    public final HashSet<Integer>[] robotLock = new HashSet[MAX_ROBOTS_PER_PLAYER];
    public final Berth[] berths = new Berth[MAX_BERTH_PER_PLAYER];
    public final Boat[] boats = new Boat[MAX_BOATS_PER_PLAYER];
    @SuppressWarnings("unchecked")
    public ArrayList<Point>[] robotsPredictPath = new ArrayList[MAX_ROBOTS_PER_PLAYER];

    public int curFrameDiff = 1;
    int jumpCount = 0;

    //boat的闪现位置，闪现泊位不用算

    public ArrayList<Point> boatFlashCandidates;
    public PointWithDirection[][] boatFlashMainChannelPoint = new PointWithDirection[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];


    public ArrayList<Point> robotPurchasePoint = new ArrayList<>();
    public ArrayList<Point> boatPurchasePoint = new ArrayList<>();
    public ArrayList<BoatSellPoint> boatSellPoints = new ArrayList<>();


    private int boatCapacity;

    public void init() throws IOException {
        long startTime = System.currentTimeMillis();
        char[][] mapData = new char[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
        gameMap = new GameMap();
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            fgets(mapData[i], inStream);
        }
        for (int i = 0; i < mapData.length; i++) {
            for (int j = 0; j < mapData[0].length; j++) {
                if (mapData[i][j] == 'R')
                    robotPurchasePoint.add(new Point(i, j));
                else if (mapData[i][j] == 'S')
                    boatPurchasePoint.add(new Point(i, j));
                else if (mapData[i][j] == 'T')
                    boatSellPoints.add(new BoatSellPoint(new Point(i, j)));
            }
        }
        gameMap.setMap(mapData);
        for (BoatSellPoint boatSellPoint : boatSellPoints) {
            boatSellPoint.init(gameMap);
        }

        BERTH_PER_PLAYER = getIntInput();
        //码头
        long maxUpdateTime = 0;
        for (int i = 0; i < BERTH_PER_PLAYER; i++) {
            String s = inStream.readLine();
            printMost(s);
            String[] parts = s.trim().split(" ");
            int id = Integer.parseInt(parts[0]);
            berths[id] = new Berth();
            berths[id].corePoint.x = Integer.parseInt(parts[1]);
            berths[id].corePoint.y = Integer.parseInt(parts[2]);
            berths[id].loadingSpeed = Integer.parseInt(parts[3]);
            berths[id].id = id;
            long curTime = System.currentTimeMillis();
            if (curTime - startTime + maxUpdateTime > 1000 * 4 + 800) {
                berths[id].init(gameMap, true);
                continue;
            }
            berths[id].init(gameMap, false);
            long r = System.currentTimeMillis();
            maxUpdateTime = max(maxUpdateTime, r - curTime);
        }
        boatCapacity = getIntInput();
        String okk = inStream.readLine();
        printMost(okk);

        //闪现点
        boatFlashCandidates = new ArrayList<>();
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                for (int k = 0; k < DIR.length / 2; k++) {
                    if (gameMap.boatIsAllInMainChannel(new Point(i, j), k)) {
                        boatFlashCandidates.add(new Point(i, j));
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                if (gameMap.boatCanReach(i, j)) {
                    //留200ms阈值,超时不更新，到时候再更新
                    long curTime = System.currentTimeMillis();
                    if (curTime - startTime > 1000 * 4 + 800) {
                        break;
                    }
                    boatUpdateFlashPoint(i, j);
                }
            }
        }
        for (int i = 0; i < robotLock.length; i++) {
            robotLock[i] = new HashSet<>();
        }
        System.gc();//主动gc一下，防止后面掉帧
        outStream.print("OK\n");
    }

    private void boatUpdateFlashPoint(int i, int j) {
        int[] bestFactors = {Integer.MAX_VALUE, 0, 0, 0, 0};//距离，右，左，上，下
        Point bestPoint = null;
        for (Point candidate : boatFlashCandidates) {
            int[] curFactors = new int[5];
            curFactors[0] = abs(candidate.x - i) + abs(candidate.y - j);
            curFactors[1] = max(candidate.y - j, 0);
            curFactors[2] = max(j - candidate.y, 0);
            curFactors[3] = max(i - candidate.x, 0);
            curFactors[4] = max(candidate.x - i, 0);
            boolean better = false;
            for (int k = 0; k < bestFactors.length; k++) {
                if (k == 0) {
                    if (curFactors[k] < bestFactors[k]) {
                        better = true;
                        break;
                    } else if (curFactors[k] > bestFactors[k]) {
                        break;//大的话说明不会更好
                    }
                } else {
                    if (curFactors[k] > bestFactors[k]) {
                        better = true;
                        break;
                    } else if (curFactors[k] < bestFactors[k]) {
                        break;//小的话说明不会更好
                    }
                }
            }
            if (better) {
                bestPoint = candidate;
                bestFactors = curFactors;
            }
        }
        // 找到最好的点
        assert bestPoint != null;
        for (int k = 0; k < DIR.length / 2; k++) {
            if (gameMap.boatIsAllInMainChannel(bestPoint, k)) {
                //找到传送点
                boatFlashMainChannelPoint[i][j] = new PointWithDirection(bestPoint, k);
            }
        }
    }

    public ArrayList<PointWithDirection> boatToBerth(Boat boat, Berth berth, int maxDeep) {
        boat.targetBerthId = berth.id;
        return boatMoveToBerth(gameMap, new PointWithDirection(boat.corePoint, boat.direction)
                , berth.id, new PointWithDirection(berth.corePoint,berth.coreDirection), maxDeep, berth.berthAroundPoints.size(), boat.remainRecoveryTime);
    }

    public ArrayList<PointWithDirection> boatToPoint(Boat boat, PointWithDirection pointWithDirection, int maxDeep) {
        return boatMoveToPoint(gameMap, new PointWithDirection(boat.corePoint, boat.direction)
                , pointWithDirection, maxDeep
                , boat.remainRecoveryTime);
    }

    public ArrayList<PointWithDirection> boatToPoint(Boat boat, PointWithDirection pointWithDirection, int maxDeep
            , ArrayList<ArrayList<PointWithDirection>> otherPath, ArrayList<Integer> otherIds) {
        return boatMoveToPoint(gameMap, new PointWithDirection(boat.corePoint, boat.direction)
                , pointWithDirection, maxDeep, boat.id, otherPath, otherIds, boat.remainRecoveryTime
        );
    }

    public ArrayList<Point> robotToBerth(Robot robot, Berth berth, int maxDeep) {
        return robotMoveToBerth(gameMap, robot.pos, berth.id, maxDeep, null);
    }

    public ArrayList<Point> robotToBerth(Robot robot, Berth berth, int maxDeep, ArrayList<ArrayList<Point>> otherPaths) {
        return robotMoveToBerth(gameMap, robot.pos, berth.id, maxDeep, otherPaths);
    }

    public ArrayList<Point> robotToPoint(Robot robot, Point end, int maxDeep) {
        return robotMoveToPoint(gameMap, robot.pos, end, maxDeep, null);
    }

    public ArrayList<Point> robotToPoint(Robot robot, Point end, int maxDeep
            , ArrayList<ArrayList<Point>> otherPaths) {
        return robotMoveToPoint(gameMap, robot.pos, end, maxDeep, otherPaths);
    }

    public ArrayList<Point> robotToPointHeuristic(Robot robot, Point end, int maxDeep
            , ArrayList<ArrayList<Point>> otherPaths, int[][] heuristicCs) {
        return robotMoveToPointHeuristic(gameMap, robot.pos, end, maxDeep, otherPaths, heuristicCs);
    }


    public void mainLoop() throws IOException {
        while (input()) {

            dispatch();

//            if (frameId > 1 && frameId < 100) {
//                Boat boat = boats[0];
//                boat.path = boatToPoint(boat, new PointWithDirection(new Point(94, 70), 1), 9999);
//                boat.finish();
//            }
//            if (frameId > 100 && frameId < 200) {
//                Boat boat = boats[1];
//                boat.path = boatToPoint(boat, new PointWithDirection(new Point(93, 60), 0), 9999);
//                boat.finish();
//            }
//
//            if (frameId > 200 && frameId < 500) {
//                PointWithDirection boat0Target = new PointWithDirection(new Point(94, 55), 1);
//                PointWithDirection boat1Target = new PointWithDirection(new Point(93, 75), 0);
//                Boat boat = boats[0];
//                boat.path = boatToPoint(boat, boat0Target, 9999);
//                Boat boat1 = boats[1];
//                boat1.path = boatToPoint(boat1, boat1Target, 9999);
//                ArrayList<ArrayList<PointWithDirection>> otherPaths = new ArrayList<>();
//                ArrayList<Integer> otherIds = new ArrayList<>();
//                for (int i = 0; i < 2; i++) {
//                    ArrayList<PointWithDirection> myPath = new ArrayList<>();
//                    for (int j = 0; j < min(BOAT_PREDICT_DISTANCE, boats[i].path.size()); j++) {
//                        myPath.add(boats[i].path.get(j));
//                    }
//                    int crashId = boatCheckCrash(gameMap, i, myPath, otherPaths, otherIds, BOAT_AVOID_DISTANCE);
//                    if (crashId != -1 && boats[i].status != 1) {
//                        //避让
//                        for (boolean[] conflictPoint : gameMap.commonConflictPoints) {
//                            Arrays.fill(conflictPoint, false);
//                        }
//                        for (boolean[] commonNoResultPoint : gameMap.commonNoResultPoints) {
//                            Arrays.fill(commonNoResultPoint, false);
//                        }
//                        for (ArrayList<PointWithDirection> otherPath : otherPaths) {
//                            PointWithDirection pointWithDirection = otherPath.get(0);
//                            ArrayList<Point> points = gameMap.getBoatPoints(pointWithDirection.point, pointWithDirection.direction);
//                            for (Point point : points) {
//                                gameMap.commonConflictPoints[point.x][point.y] = true;
//                            }
//                        }
//                        for (int j = 0; j < otherPaths.size(); j++) {
//                            if (otherIds.get(j) == crashId) {
//                                ArrayList<PointWithDirection> pointWithDirections = otherPaths.get(j);
//                                for (PointWithDirection pointWithDirection : pointWithDirections) {
//                                    ArrayList<Point> points = gameMap.getBoatPoints(pointWithDirection.point, pointWithDirection.direction);
//                                    for (Point point : points) {
//                                        gameMap.commonNoResultPoints[point.x][point.y] = true;
//                                    }
//                                }
//                            }
//                        }
//                        ArrayList<PointWithDirection> result = boatGetSafePoints(gameMap, gameMap.commonCs, myPath.get(0), gameMap.commonConflictPoints, gameMap.commonNoResultPoints, 25);
//                        boats[i].path = backTrackPath(gameMap, result.get(0), gameMap.commonCs, 0);
//                        myPath.clear();
//                        for (int j = 0; j < min(BOAT_PREDICT_DISTANCE, boats[i].path.size()); j++) {
//                            myPath.add(boats[i].path.get(i));
//                        }
//                    }
//
//                    //高优先级的撞到你，则不动
//                    for (int j = 0; j < otherPaths.size(); j++) {
//                        if (otherPaths.get(j).size() <= 1) {
//                            continue;
//                        }
//                        PointWithDirection otherNext = otherPaths.get(j).get(1);
//                        boolean crash = false;
//                        if (otherIds.get(j) < i) {
//                            if (boatCheckCrash(gameMap, otherNext, myPath.get(0))) {
//                                crash = true;
//                            }
//                        } else {
//                            if (boatCheckCrash(gameMap, otherNext, myPath.get(1))) {
//                                crash = true;
//                            }
//                        }
//                        if (crash) {
//                            PointWithDirection start = otherPaths.get(j).get(0);
//                            otherPaths.get(j).clear();
//                            for (int k = 0; k < 2; k++) {
//                                otherPaths.get(j).add(start);
//                            }
//                            boats[otherIds.get(j)].path = new ArrayList<>(otherPaths.get(j));
//                            //选择不动去避让
//                        }
//                    }
//                    otherPaths.add(myPath);
//                    otherIds.add(i);
//                }
//                boat.finish();
//                boat1.finish();
//            }


//            if (frameId == 1) {
//                outStream.printf("lbot %d %d\n", robotPurchasePoint.get(0).x, robotPurchasePoint.get(0).y);
//                robots[ROBOTS_PER_PLAYER++] = new Robot(this);
//                outStream.printf("lbot %d %d\n", robotPurchasePoint.get(0).x, robotPurchasePoint.get(0).y);
//                robots[ROBOTS_PER_PLAYER++] = new Robot(this);
//            }
//            if (frameId > 1 && frameId < 200) {
//                Robot robot = robots[0];
//                robot.path = robotToPoint(robot, new Point(93, 73), 9999);
//                robot.finish();
//            }
//            if (frameId > 200 && frameId < 400) {
//                Robot robot = robots[1];
//                robot.path = robotToPoint(robot, new Point(92, 73), 9999);
//                robot.finish();
//            }
//            if (frameId == 401) {
////                outStream.printf("move %d %d\n", 0, 2);
//                outStream.printf("move %d %d\n", 1, 3);
//            }


            outStream.print("OK\n");
            outStream.flush();
        }
    }


    private void dispatch() {

        robotDoAction();
//            boatDoAction();

        if (frameId == 1) {
            outStream.printf("lbot %d %d\n", robotPurchasePoint.get(0).x, robotPurchasePoint.get(0).y);
            robots[ROBOTS_PER_PLAYER++] = new Robot(this);
            outStream.printf("lbot %d %d\n", robotPurchasePoint.get(0).x, robotPurchasePoint.get(0).y);
            robots[ROBOTS_PER_PLAYER++] = new Robot(this);
            outStream.printf("lboat %d %d\n", boatPurchasePoint.get(1).x, boatPurchasePoint.get(1).y);
            boats[BOATS_PER_PLAYER++] = new Boat(this, boatCapacity);
        }
        if (frameId > 1 && frameId < 100) {
            Boat boat = boats[0];
            boat.path = boatToPoint(boat,new PointWithDirection(new Point(101,52),3), 9999);
            boat.finish();
        }
        if(frameId>100&&frameId<200){
            Boat boat = boats[0];
            boat.path = boatToBerth(boat, berths[2], 9999);
            boat.finish();
        }
    }

//    private void boatDoAction() {
//        //船只选择回家
//        for (Boat boat : boats) {
//            if (boat.targetId != -1 && boat.num == boat.capacity) {//满了回家
//                boat.go();
//                boat.remainTime = berths[boat.targetId].transportTime;
//                boat.status = 0;//移动中
//                boat.targetId = -1;
//                boat.estimateComingBerthId = -1;
//                boat.exactStatus = IN_ORIGIN_TO_BERTH;
//            } else if (boat.num > 0 && boat.targetId != -1) {
//                int minSellTime;
//                if (boat.status != 0) {
//                    //说明到达目标了
//                    minSellTime = berths[boat.targetId].transportTime;
//                } else {
//                    minSellTime = min(berths[boat.lastArriveTargetId].transportTime,
//                            boat.remainTime + berths[boat.targetId].transportTime);//去到之后再会来的时间
//                }
//                if (frameId + minSellTime >= GAME_FRAME - FPS / 10) {//会掉帧，预留10帧
//                    //极限卖
//                    boat.go();
//                    boat.remainTime = minSellTime;
//                    boat.status = 0;//移动中
//                    boat.targetId = -1;
//                    boat.assigned = true;
//                    boat.estimateComingBerthId = -1;
//                    boat.exactStatus = IN_ORIGIN_TO_BERTH;
//                }
//            }
//        }
//        //船只贪心去买卖，跟机器人做一样的决策
//        //先保存一下泊位的价值列表
//        //1当前价值列表+机器人运过来的未来价值列表
//        int[] goodsNumList = new int[BERTH_PER_PLAYER];
//        for (int i = 0; i < goodsNumList.length; i++) {
//            goodsNumList[i] = berths[i].goodsNums;
//        }
//        for (Robot robot : robots) {
//            if (!robot.assigned) {
//                continue;
//            }
//            if (robot.targetBerthId != -1) {
//                goodsNumList[robot.targetBerthId]++;
//            }
//        }
//        int[] goodComingTimes = new int[BERTH_PER_PLAYER];//货物开始来泊位的最早时间
//        Arrays.fill(goodComingTimes, 0);//这一帧开始到来
//        //贪心切换找一个泊位
//
//        while (true) {
//            if (!boatGreedyBuy(goodsNumList, goodComingTimes)) {
//                break;
//            }
//        }
//
//
//        //装载货物
//        for (Boat boat : boats) {
//            //移动立即生效
//            if (boat.remainTime > 0) {
//                boat.remainTime--;
//            }
//            //移动完之后的下一帧才能开始装货
//            if (boat.status == 1 && boat.targetId != -1 && berths[boat.targetId].goodsNums > 0) {
//                //正常运行,开始装货，如果跳帧，什么时候到达已经不知道了
//                //如果remainTime不为0，说明中间跳帧了，多装几次货就好了
//                for (int i = 0; i < (boat.remainTime + 1); i++) {
//                    int load = min(berths[boat.targetId].loadingSpeed, berths[boat.targetId].goodsNums);
//                    load = min(load, boat.capacity - boat.num);//剩余空间
//                    boat.num += load;
//                    berths[boat.targetId].goodsNums -= load;
//                    for (int j = 0; j < load; j++) {
//                        assert !berths[boat.targetId].goods.isEmpty();
//                        Integer value = berths[boat.targetId].goods.poll();
//                        assert value != null;
//                        boat.value += value;
//                        berths[boat.targetId].totalValue -= value;
//                    }
//                }
//                boat.remainTime = 0;
//            }
//        }
//    }


    private boolean boatGreedyBuy(int[] goodsNumList, int[] goodComingTimes) {
        //三种决策，在去目标中的，
        //在回家或者在家的
        //在泊位的或者在泊位等待的
        class Stat implements Comparable<Stat> {
            final Boat boat;
            final Berth berth;
            final int count;//消费个数
            final int updateTime;//消费个数
            final double profit;

            public Stat(Boat boat, Berth berth, int count, int updateTime, double profit) {
                this.boat = boat;
                this.berth = berth;
                this.count = count;
                this.profit = profit;
                this.updateTime = updateTime;
            }

            @Override
            public int compareTo(Stat b) {
                return Double.compare(b.profit, profit);
            }
        }
        double goodsComingTime = avgPullGoodsTime * BERTH_PER_PLAYER;
        ArrayList<Boat> oneTypesBoats = new ArrayList<>();
        ArrayList<Boat> twoTypesBoats = new ArrayList<>();
        ArrayList<Boat> threeTypesBoats = new ArrayList<>();
        for (Boat boat : boats) {
            if (boat.assigned) {
                continue;
            }
            if (boat.targetId != -1 && boat.status == 0) {
                oneTypesBoats.add(boat);
            } else if (boat.targetId == -1) {
                twoTypesBoats.add(boat);//回家或者在家的
            } else {
                threeTypesBoats.add(boat);
            }
        }
        //第一种，在移动向泊位的，不切换目标，直接消耗
        for (Boat boat : oneTypesBoats) {
            int needCount = boat.capacity - boat.num;
            if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
                return true;
        }

        //第二种,选择泊位货物最多的，一样多则选updateTime最早的，一样早选择最远的
        //考虑在移动，运输中也算不能返回，移动完才可以动？
        ArrayList<Stat> stat = new ArrayList<>();
        //选择折现价值最大的
        for (Berth buyBerth : berths) {
            Boat selectBoat = null;//最近的先决策
            int minDistance = Integer.MAX_VALUE;
            for (Boat boat : twoTypesBoats) {
                int distance = getBoatToBerthDistance(buyBerth, boat);
                if (distance < minDistance) {
                    minDistance = distance;
                    selectBoat = boat;
                }
            }
            if (selectBoat == null) {
                continue;
            }

            if (frameId + minDistance + buyBerth.transportTime >= GAME_FRAME) {
                continue;//货物装不满，则不管
            }

//            int buyTime = minDistance;
//            int sellTime = buyBerth.transportTime;
//            int needCount = selectBoat.capacity;
//            int remainTime = GAME_FRAME - buyTime - sellTime - frameId;
//            int realCount = min(needCount, remainTime / buyBerth.loadingSpeed);//装货
//            realCount = min(realCount, goodsNumList[buyBerth.id] + (int) (remainTime / goodsComingTime));//来货
//            int loadTime = (int) ceil(1.0 * realCount / buyBerth.loadingSpeed);
//            int totalWaitTime = goodComingTimes[buyBerth.id] + (int) ceil(max(0, realCount - goodsNumList[buyBerth.id]) * goodsComingTime);
//            int arriveWaitTime = max(0, totalWaitTime - buyTime);//到达之后的等待时间
//            double profit = 1.0 * realCount / (buyTime + max(arriveWaitTime, loadTime) + sellTime);

            //只能装那么多，尽量去装吧,可以切泊位可能上面决策好一点，不可以切泊位，还是看数量高分
            int realCount = selectBoat.capacity;
            double profit = 1e10 + goodsNumList[buyBerth.id] * 1e10 - goodComingTimes[buyBerth.id] * 1e5 + minDistance;
            int totalWaitTime = goodComingTimes[buyBerth.id] +
                    (int) ceil(max(0, selectBoat.capacity - goodsNumList[buyBerth.id])
                            * goodsComingTime);
            stat.add(new Stat(selectBoat, buyBerth, min(goodsNumList[buyBerth.id], realCount)
                    , totalWaitTime, profit));
        }
        if (!stat.isEmpty()) {
            Collections.sort(stat);
            //同一个目标不用管
//            boatRealDecision(goodsNumList, goodComingTimes, stat.get(0).berth
//                    , stat.get(0).boat, stat.get(0).updateTime, stat.get(0).count);
            return true;
        }

        //第三种，在泊位上或者等待的,货物数量和,单位时间价值
        for (Boat boat : threeTypesBoats) {
            //到每个泊位距离一定是500
            int needCount = boat.capacity - boat.num;
            if (boatDecisionType == DECISION_ON_ORIGIN) {
                //不可以切换泊位,
                assert boat.estimateComingBerthId == boat.targetId;
                if (!BOAT_NO_WAIT || frameId + berths[boat.targetId].transportTime * 3 > GAME_FRAME || berths[boat.targetId].goodsNums > 0) {
                    if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
                        return true;
                } else {
                    //回家
                    boat.remainTime = berths[boat.targetId].transportTime;
                    boat.status = 0;//移动中
                    boat.targetId = -1;
                    boat.estimateComingBerthId = -1;
                    boat.exactStatus = IN_ORIGIN_TO_BERTH;
                    return true;
                }
            }

            if (BOAT_NO_WAIT && frameId + berths[boat.targetId].transportTime * 3 < GAME_FRAME) {
                if (berths[boat.targetId].goodsNums > 0) {
                    if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
                        return true;
                }
                //没货物了，选择切泊位买了走人，或者回家
                double maxProfit = 1.0 * boat.num / berths[boat.targetId].transportTime / 2;
                Berth changeBerth = null;
                int maxWaitTime = -1;
                int maxCount = 0;

                //来回一趟基本价值，如果有人超过了，则切泊位，否则回家
                for (Berth berth : berths) {
                    if (boat.targetId == berth.id) {
                        continue;
                    }
                    int buyTime = BERTH_CHANGE_TIME;
                    int sellTime = berth.transportTime;
                    if (frameId + buyTime + sellTime >= GAME_FRAME) {
                        continue;//不干
                    }
                    int remainTime = GAME_FRAME - buyTime - sellTime - frameId;
                    int realCount = min(needCount, remainTime / berth.loadingSpeed);//装货
                    realCount = min(realCount, goodsNumList[berth.id] + (int) ((buyTime - goodComingTimes[berth.id]) / goodsComingTime));//来货
                    int loadTime = (int) ceil(1.0 * realCount / berth.loadingSpeed);
                    int totalWaitTime = goodComingTimes[berth.id] + (int) ceil(max(0, realCount - goodsNumList[berth.id]) * goodsComingTime);
                    double profit = 1.0 * (realCount + boat.num) / (berths[boat.targetId].transportTime + 1.0 * boat.num
                            / berths[boat.targetId].loadingSpeed + buyTime + loadTime + sellTime);
                    if (profit > maxProfit) {
                        changeBerth = berth;
                        maxProfit = profit;
                        maxWaitTime = totalWaitTime;
                        maxCount = min(realCount, goodsNumList[berth.id]);
                    }

                }
                if (changeBerth != null) {
                    stat.add(new Stat(boat, changeBerth, min(maxCount, goodsNumList[changeBerth.id]), maxWaitTime, maxProfit));
                } else {
                    //回家
                    if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
                        return true;
                }
            } else {
                //目前船所在的泊位的来货速度,如果来货速度实在太慢，不需要等，直接切目标
                int curBerthGoodsComingTime = frameId / max(1, berths[boat.targetId].totalGoodsNums);
                for (Berth berth : berths) {
                    int buyTime = boat.targetId == berth.id ? 0 : BERTH_CHANGE_TIME;
                    int sellTime = berth.transportTime;
                    if (frameId + buyTime + sellTime >= GAME_FRAME) {
                        continue;//不干
                    }
                    if (berth.id != boat.targetId && berths[boat.targetId].goodsNums > 0) {
                        continue;//有货物了，船只能选择这个泊位不变
                    }

                    int remainTime = GAME_FRAME - buyTime - sellTime - frameId;
                    int realCount = min(needCount, remainTime / berth.loadingSpeed);//装货
                    realCount = min(realCount, goodsNumList[berth.id] + (int) ((remainTime + buyTime - goodComingTimes[berth.id]) / goodsComingTime));//来货
                    int loadTime = (int) ceil(1.0 * realCount / berth.loadingSpeed);
                    int totalWaitTime = goodComingTimes[berth.id] + (int) ceil(max(0, realCount - goodsNumList[berth.id]) * goodsComingTime);
                    int arriveWaitTime = max(0, totalWaitTime - buyTime);//到达之后的等待时间
                    //todo 可调参
                    if (berth.id != boat.targetId && arriveWaitTime > 0//留一半阈值,&&remainTime > 2 * berth.loadingSpeed * realCount
                            && curBerthGoodsComingTime * 2 > goodsComingTime//来货速度的两倍小于平均来货速度，直接切目标
                            && remainTime > 2 * berth.transportTime - buyTime
                    ) {
                        continue;//不是同一个泊位，且到达之后需要等待，那么先不去先,有问题，最后还是不会切泊位的
                    }
                    double profit = realCount + 1.0 * realCount / (buyTime + max(arriveWaitTime, loadTime) + sellTime);
                    //增益超过保持因子，则切换目标
                    stat.add(new Stat(boat, berth, min(realCount, goodsNumList[berth.id]), totalWaitTime, profit));
                }
            }
        }
        if (!stat.isEmpty()) {
            Collections.sort(stat);
            //同一个目标不用管
//            boatRealDecision(goodsNumList, goodComingTimes, stat.get(0).berth
//                    , stat.get(0).boat, stat.get(0).updateTime, stat.get(0).count);
            return true;
        }
        return false;

    }

//    private static void boatRealDecision(int[] goodsNumList, int[] goodComingTimes, Berth berth, Boat boat, int updateTime, int count) {
//        goodsNumList[berth.id] -= count;
//        goodComingTimes[berth.id] = updateTime;
//        //boat去移动
//        if (boat.targetId != berth.id && (!(boat.status == 0 && boat.targetId == -1))) {
//            boat.ship(berth.id);//不在往原点运输中
//            if (boat.lastArriveTargetId == -1) {
//                //在虚拟点，或者虚拟点到泊位
//                boat.remainTime = berth.transportTime;
//            } else {
//                boat.remainTime = BERTH_CHANGE_TIME;
//            }
//            //printERROR("boat:" + boat.id + ",原来目标:" + boat.targetId + ",现在目标:" + berth.id + ",time:" + boat.remainTime);
//            boat.targetId = berth.id;
//            boat.status = 0;
//        }
//        boat.assigned = true;
//        //更新泊位的货物列表和前缀和,说明这个船消耗了这么多货物
//        boat.estimateComingBerthId = berth.id;
//    }


    private boolean updateSumTimes(int[] goodsNumList, int[] goodComingTimes, Boat boat, double goodsComingSpeed, int needCount) {
        int comingBerthId = boat.estimateComingBerthId;
        if (comingBerthId != -1) {
            int comingCount = max(0, needCount - goodsNumList[comingBerthId]);
            int addComingTime = 0;
            if (comingCount > 0) {
                addComingTime = (int) ceil(comingCount * goodsComingSpeed);
            }
            goodsNumList[comingBerthId] -= min(goodsNumList[comingBerthId], needCount);
            goodComingTimes[comingBerthId] += addComingTime;
            boat.assigned = true;
            return true;
        }
        return false;
    }


    @SuppressWarnings("all")
    private int getCanReachBerthsCount() {
        int count = 0;
        for (Berth berth : berths) {
            for (Boat boat : boats) {
                int distance = getBoatToBerthDistance(berth, boat);
                if (frameId + distance + berth.transportTime < GAME_FRAME) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private static int getBoatToBerthDistance(Berth buyBerth, Boat boat) {
        int dist;
        if (boat.targetId == buyBerth.id) {
            dist = boat.remainTime;
            if (boat.exactStatus == IN_BERTH_WAIT) {
                dist = 1;//泊位外等待为1
            }
        } else if (boat.lastArriveTargetId != -1 && boat.targetId != -1) {
            //泊位内，泊位等待，泊位切换中
            dist = BERTH_CHANGE_TIME;
        } else if (boat.targetId == -1 && boat.status == 0) {//往原点运输中
            dist = boat.remainTime + buyBerth.transportTime;//虚拟点到泊位时间
        } else {
            dist = buyBerth.transportTime;//虚拟点到泊位时间
        }
        return dist;
    }

    private void robotDoAction() {
        workbenchesLock.clear();//动态需要解锁
        while (true) {
            if (!greedySell()) {
                break;
            }  //决策
        }
        while (true) {
            if (!greedyBuy()) {
                break;
            }
        }


        //选择路径，碰撞避免
        Robot[] tmpRobots = new Robot[ROBOTS_PER_PLAYER];
        System.arraycopy(robots, 0, tmpRobots, 0, ROBOTS_PER_PLAYER);
        sortRobots(tmpRobots);
        //1.选择路径,和修复路径
        for (int i = 0; i < tmpRobots.length; i++) {
            Robot robot = tmpRobots[i];
            if (!robot.assigned) {
                continue;
            }

            ArrayList<Point> path;
            int[][] heuristicPoints;
            if (robot.carry) {
                ArrayList<Integer> candidate = berths[robot.targetBerthId].robotMinDistanceIndexes[robot.pos.x][robot.pos.y];
                //找到一条路径不与之前的路径相撞,找不到，选择第一条路径
                path = berths[robot.targetBerthId].robotDijkstra.get(candidate.get(0)).moveFrom(robot.pos);

                for (int j = 1; j < candidate.size(); j++) {
                    int index = candidate.get(j);
                    ArrayList<Point> candidatePath = berths[robot.targetBerthId].robotDijkstra.get(index).moveFrom(robot.pos);
                    if (!checkCrash(candidatePath, robot.id)) {
                        path = candidatePath;
                        break;
                    } else {
                        //尝试找不撞的
                        heuristicPoints = berths[robot.targetBerthId].robotDijkstra.get(index).cs;
                        boolean result = findABestPath(candidatePath, heuristicPoints, robot.id);
                        if (result) {
                            path = candidatePath;
                            break;
                        }
                    }
                }
                heuristicPoints = berths[robot.targetBerthId].robotDijkstra.get(0).cs;
            } else {
                path = workbenches.get(robot.targetWorkBenchId).dijkstra.moveFrom(robot.pos);
                heuristicPoints = workbenches.get(robot.targetWorkBenchId).dijkstra.cs;
            }
            //检查是否与前面机器人相撞，如果是，则重新搜一条到目标点的路径，极端情况，去到物品消失不考虑
            if (checkCrash(path, robot.id)) {
                //尝试搜一条不撞的路径
                assert !path.isEmpty();
                boolean result = findABestPath(path, heuristicPoints, robot.id);
                if (!result && !robot.redundancy) {
                    //去到很大概率会消失,锁住这个工作台,重新决策分配路径
                    robot.assigned = false;
                    robot.buyAssign = false;
                    robotLock[robot.id].add(robot.targetBerthId);
                    greedyBuy();
                    i--;
                    //重开
                    continue;
                }

            }
            robot.path.clear();
            robot.path.addAll(path);
        }

        //2.碰撞避免
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            //固定只预测未来一个格子，就是两个小格子，
            //预测他未来两个格子就行，四下，如果冲突，则他未来一个格子自己不能走，未来第二个格子自己尽量也不走
            Robot robot = tmpRobots[i];
            robotsPredictPath[robot.id] = new ArrayList<>();
            if (!robot.assigned) {
                //未来一格子在这里不动，如果别人撞过来，则自己避让
                for (int j = 1; j <= 2; j++) {
                    robotsPredictPath[robot.id].add(gameMap.posToDiscrete(robot.pos));
                }
            } else {
//                assert robot.path.size() >= 3;//包含起始点
                if (robot.path.size() == 1) {
                    //原点不动，大概率有漏洞
                    robotsPredictPath[robot.id].add(robot.path.get(0));
                    robotsPredictPath[robot.id].add(robot.path.get(0));
                    printError("error robot.path.size()==1");
                }
                //至少有未来一个格子，如果有两个也要预测，因为两个可以让他往旁边避让，预测前面3个格子，避让机器人要尽量躲开他的三个格子。
                for (int j = 1; j <= min(6, robot.path.size() - 1); j++) {
                    robotsPredictPath[robot.id].add(robot.path.get(j));
                }
            }
            int crashId = -1;
            for (int j = 0; j < i; j++) {
                //一个格子之内撞不撞
                for (int k = 0; k < 2; k++) {
                    Point point = robotsPredictPath[robot.id].get(k);
                    if (point.equal(robotsPredictPath[tmpRobots[j].id].get(k)) && !gameMap.isRobotMainChannel(point.x, point.y)) {
                        crashId = tmpRobots[j].id;
                        break;
                    }
                }
                if (crashId != -1) {
                    break;
                }
            }
            int avoidId = robot.id;

            if (crashId != -1) {

                //看看自己是否可以避让，不可以的话就说明被夹住了,让冲突点去让,并且自己强制提高优先级50帧
                robots[crashId].beConflicted = FPS;
                ArrayList<Point> candidates = new ArrayList<>();
                candidates.add(robots[avoidId].pos);
                for (int j = 0; j < DIR.length / 2; j++) {
                    candidates.add(robots[avoidId].pos.add(DIR[j]));
                }
                Point result = new Point(-1, -1);
                int bestDist = Integer.MAX_VALUE;

                for (Point candidate : candidates) {
                    if (!gameMap.robotCanReach(candidate.x, candidate.y)) {
                        continue;
                    }
                    boolean crash = false;
                    for (Robot tmpRobot : tmpRobots) {
                        if (tmpRobot.id == avoidId || robotsPredictPath[tmpRobot.id] == null) {
                            continue;//后面的
                        }
                        Point start = gameMap.posToDiscrete(robots[avoidId].pos);
                        Point end = gameMap.posToDiscrete(candidate);
                        Point mid = start.add(end).div(2);
                        if ((mid.equal(robotsPredictPath[tmpRobot.id].get(0)) && !gameMap.isRobotMainChannel(mid.x, mid.y))
                                || (end.equal(robotsPredictPath[tmpRobot.id].get(1)) && !gameMap.isRobotMainChannel(end.x, end.y))) {
                            //去重全加进来
                            crash = true;
                            break;
                        }
                    }
                    if (crash) {
                        continue;
                    }

                    int dist;
                    if (!robots[avoidId].carry) {
                        if (robots[avoidId].targetWorkBenchId == -1) {
                            assert !robots[avoidId].assigned;
                            dist = MAP_FILE_ROW_NUMS * MAP_FILE_COL_NUMS;
                        } else {
                            dist = workbenches.get(robots[avoidId].targetWorkBenchId).getMinDistance(candidate);
                        }
                    } else {
                        dist = berths[robots[avoidId].targetBerthId].getRobotMinDistance(candidate);
                    }
                    assert dist != Integer.MAX_VALUE;
                    for (Point point : robotsPredictPath[crashId]) {
                        if (candidate.equal(point) && !gameMap.isRobotMainChannel(candidate.x, candidate.y)) {
                            dist += 2;//在别人路径上惩罚增大
                            break;
                        }
                    }
                    if (dist < bestDist) {
                        result = candidate;
                        bestDist = dist;
                    }
                }
                if (!result.equal(-1, -1)) {
                    //修改预测路径
                    robotsPredictPath[avoidId].clear();
                    robots[avoidId].avoid = true;
                    Point start = gameMap.posToDiscrete(robots[avoidId].pos);
                    Point end = gameMap.posToDiscrete(result);
                    Point mid = start.add(end).div(2);
                    robotsPredictPath[avoidId].add(mid);//中间
                    robotsPredictPath[avoidId].add(end);//下一个格子
                } else {
                    robots[avoidId].beConflicted = FPS;
                    robots[avoidId].forcePri += 1;
                    sortRobots(tmpRobots);
                    i = -1;
                }

            }
        }


        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            if (robots[i].avoid) {
                Point start = gameMap.posToDiscrete(robots[i].pos);
                robots[i].path.clear();
                robots[i].path.add(start);
                robots[i].path.add(robotsPredictPath[robots[i].id].get(0));
                robots[i].path.add(robotsPredictPath[robots[i].id].get(1));
                //在避让，所以路径改变了，稍微改一下好看一点
            }
            robots[i].finish();
            if (robots[i].beConflicted-- < 0 && robots[i].forcePri != 0) {
                robots[i].forcePri = 0;
            }
        }
    }

    private void sortRobots(Robot[] robots) {
        Arrays.sort(robots, (o1, o2) -> {
            if (o1.forcePri != o2.forcePri) {//暴力优先级最高
                return o2.forcePri - o1.forcePri;
            }
            if (o1.redundancy ^ o2.redundancy) {//没冗余时间
                return o2.redundancy ? -1 : 1;
            }
            if (o1.carryValue != o2.carryValue) {//携带物品高
                return o2.carryValue - o1.carryValue;//看价值
            }
            return o1.priority != o2.priority ? o2.priority - o1.priority : o1.id - o2.id;
        });
        Arrays.fill(robotsPredictPath, null);
    }


    private boolean findABestPath(ArrayList<Point> path, int[][] cs, int robotId) {
        //发现一条更好的路径,启发式搜
        class Pair {
            final int deep;
            final Point p;

            public Pair(int deep, Point p) {
                this.deep = deep;
                this.p = p;
            }
        }
        int[][] discreteCs = new int[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH]; //前面2位是距离，后面的位数是距离0xdistdir
        for (int[] discreteC : discreteCs) {
            Arrays.fill(discreteC, Integer.MAX_VALUE);
        }

        Point start = path.get(0);
        Point end = path.get(path.size() - 1);
        Stack<Pair> stack = new Stack<>();
        stack.push(new Pair(0, start));
        discreteCs[start.x][start.y] = 0;
        while (!stack.empty()) {
            Point top = stack.peek().p;
            if (top.equal(end)) {
                //回溯路径
                ArrayList<Point> result = getRobotPathByCs(discreteCs, top);
                Collections.reverse(result);
                path.clear();
                path.addAll(result);
                return true;
            }
            int deep = stack.peek().deep;
            stack.pop();
            for (int i = 0; i < DIR.length / 2; i++) {
                //四方向的
                int lastDirIdx = discreteCs[top.x][top.y] & 3;
                int dirIdx = i ^ lastDirIdx; // 优先遍历上一次过来的方向
                Point dir = DIR[dirIdx];
                int dx = top.x + dir.x;
                int dy = top.y + dir.y;//第一步
                if (!gameMap.robotCanReachDiscrete(dx, dy) || discreteCs[dx][dy] != Integer.MAX_VALUE) {
                    continue; // 下个点不可达
                }
                if (!gameMap.robotCanReachDiscrete(dx + dir.x, dy + dir.y) || discreteCs[dx + dir.x][dy + dir.y] != Integer.MAX_VALUE) {
                    continue;//下下个点
                }
                if (checkCrashInDeep(robotId, deep + 1, dx, dy)
                        || checkCrashInDeep(robotId, deep + 2, dx + dir.x, dy + dir.y)) {
                    continue;
                }
                Point cur = gameMap.discreteToPos(top.x, top.y);
                Point next = gameMap.discreteToPos(dx + dir.x, dy + dir.y);
                if ((cs[cur.x][cur.y] >> 2) != (cs[next.x][next.y] >> 2) + 1) {
                    //启发式剪枝，不是距离更近则直接结束
                    continue;
                }
                discreteCs[dx][dy] = ((deep + 1) << 2) + dirIdx;//第一步
                dx += dir.x;
                dy += dir.y;
                discreteCs[dx][dy] = ((deep + 2) << 2) + dirIdx;//第一步
                stack.push(new Pair(deep + 2, new Point(dx, dy)));
            }
        }
        //搜不到，走原路
        return false;
    }


    private boolean checkCrashInDeep(int robotId, int deep, int dx, int dy) {
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            Robot robot = robots[i];
            if (!robot.assigned || robot.id == robotId) {
                continue;
            }
            if (deep > robot.path.size() - 1) {
                continue;
            }
            if (robot.path.get(deep).equal(dx, dy) && !gameMap.isRobotMainChannel(dx, dy)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkCrash(ArrayList<Point> path, int robotId) {
        return checkCrash(path, robotId, Integer.MAX_VALUE);
    }

    @SuppressWarnings("all")
    private boolean checkCrash(ArrayList<Point> path, int robotId, int maxDetectDistance) {
        //默认检测整条路径
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            Robot robot = robots[i];
            if (robot.id == robotId || robot.path.isEmpty()) {
                continue;
            }
            int detectDistance = min(maxDetectDistance, min(path.size(), robot.path.size()));
            for (int j = 0; j < detectDistance; j++) {
                Point point = robot.path.get(j);
                if (point.equal(path.get(j)) && !gameMap.isRobotMainChannel(point.x, point.y)) {
                    return true;
                }
            }
        }

        return false;
    }

    int robotMinToWorkbenchDistance(Robot robot, Workbench workbench) {
        if (robot.carry) {
            assert (robot.assigned);
            int toBerth = berths[robot.targetBerthId].getRobotMinDistance(robot.pos);
            int k = berths[robot.targetBerthId].robotMinDistanceIndexes[robot.pos.x][robot.pos.y].get(0);
            int toWorkBench = berths[robot.targetBerthId].robotDijkstra.get(k).getMoveDistance(workbench.pos);
            return toWorkBench + toBerth;
        }
        return workbench.getMinDistance(robot.pos);
    }

    private boolean greedyBuy() {
        class Stat implements Comparable<Stat> {
            final Robot robot;
            final Workbench workbench;
            final Berth seller;
            final double profit;

            public Stat(Robot robot, Workbench workbench, Berth seller, double profit) {
                this.robot = robot;
                this.workbench = workbench;
                this.seller = seller;
                this.profit = profit;
            }

            @Override
            public int compareTo(Stat b) {
                return Double.compare(b.profit, profit);
            }
        }

        ArrayList<Stat> stat = new ArrayList<>();
        //选择折现价值最大的
        for (Workbench buyWorkbench : workbenches.values()) {
            //存在就一定有产品
            if (workbenchesLock.contains(buyWorkbench.id)) {
                continue;//别人选择过了
            }
            //贪心，选择最近的机器人

            //选距离最近的，如果是没到泊位的有好几个，选离泊位最近的
            Robot selectRobot = null;
            int minDist = Integer.MAX_VALUE;
            for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
                Robot robot = robots[i];
                if (robot.buyAssign) {
                    continue;
                }
                if (!buyWorkbench.canReach(robot.pos)) {
                    continue; //不能到达
                }
                if (robotLock[robot.id].contains(buyWorkbench.id)) {
                    continue;
                }
                int dist = robotMinToWorkbenchDistance(robot, buyWorkbench);
                int toBerthTime = 0;
                if (robot.carry) {
                    toBerthTime = berths[robot.targetBerthId].getRobotMinDistance(robot.pos);
                }
                dist -= toBerthTime;
                if (dist < minDist) {
                    minDist = dist;
                    selectRobot = robot;
                }
            }
            if (selectRobot == null) {
                continue;
            }
            int toBerthTime = 0;
            if (selectRobot.carry) {
                toBerthTime = berths[selectRobot.targetBerthId].getRobotMinDistance(selectRobot.pos);
            }

            if (toBerthTime + minDist > buyWorkbench.remainTime) {
                continue;//去到货物就消失了,不去
            }
            int arriveBuyTime = minDist;
            double maxProfit = -GAME_FRAME;
            Berth selectSellBerth = null;
            for (int i = 0; i < BERTH_PER_PLAYER; i++) {
                Berth sellBerth = berths[i];
                if (!sellBerth.robotCanReach(buyWorkbench.pos)) {
                    continue; //不能到达
                }
                int arriveSellTime = sellBerth.getRobotMinDistance(buyWorkbench.pos);//机器人买物品的位置开始
                int collectTime = getFastestCollectTime(toBerthTime + arriveBuyTime + arriveSellTime, sellBerth);
                int sellTime = collectTime + sellBerth.transportTime;

                double profit;
                if (frameId + sellTime >= GAME_FRAME) {
                    profit = -sellTime;//最近的去决策，万一到了之后能卖就ok，买的时候检测一下
                } else {
                    double value = buyWorkbench.value;
                    value += DISAPPEAR_REWARD_FACTOR * value * (WORKBENCH_EXIST_TIME - buyWorkbench.remainTime) / WORKBENCH_EXIST_TIME;
                    profit = value / (arriveSellTime + arriveBuyTime);
                    //考虑注释掉，可能没啥用，因为所有泊位都可以卖，可能就应该选最近的物品去买
                    if (selectRobot.targetWorkBenchId == buyWorkbench.id && !selectRobot.carry) {
                        profit *= (1 + SAME_TARGET_REWARD_FACTOR);
                    }
                }

                if (profit > maxProfit) {
                    maxProfit = profit;
                    selectSellBerth = sellBerth;
                }
            }
            if (selectSellBerth == null)
                continue;
            stat.add(new Stat(selectRobot, buyWorkbench, selectSellBerth, maxProfit));
        }
        if (stat.isEmpty())
            return false;
        Collections.sort(stat);

        //锁住买家
        assignRobot(stat.get(0).workbench, stat.get(0).seller, stat.get(0).robot, RA_BUY);
        return true;
    }


    private boolean greedySell() {
        class Stat implements Comparable<Stat> {
            final Robot robot;
            final Berth berth;
            final double profit;

            public Stat(Robot robot, Berth berth, double profit) {
                this.robot = robot;
                this.berth = berth;
                this.profit = profit;
            }

            @Override
            public int compareTo(Stat b) {
                return Double.compare(b.profit, profit);
            }
        }

        ArrayList<Stat> stat = new ArrayList<>();
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            Robot robot = robots[i];
            if (robot.assigned || !robot.carry) {
                continue;
            }

            //选择折现价值最大的
            Berth select = null;
            double maxProfit = -GAME_FRAME;
            for (int j = 0; j < BERTH_PER_PLAYER; j++) {
                Berth sellBerth = berths[j];
                if (!sellBerth.robotCanReach(robot.pos)) {
                    continue; //不能到达
                }
                //assert fixTime != Integer.MAX_VALUE;
                int arriveTime = sellBerth.getRobotMinDistance(robot.pos);
                double profit;
                //包裹被揽收的最小需要的时间
                int collectTime = getFastestCollectTime(arriveTime - 1, sellBerth);
                int sellTime = collectTime + sellBerth.transportTime;
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
                    select = sellBerth;
                }
            }
            if (select != null) {
                stat.add(new Stat(robot, select, maxProfit));
            }
        }
        if (stat.isEmpty())
            return false;
        Collections.sort(stat);
        assignRobot(null, stat.get(0).berth, stat.get(0).robot, RA_SELL);
        return true;
    }

    private void assignRobot(Workbench workbench, Berth berth, Robot robot, RobotAction action) {
        robot.assigned = true;
        if (!robot.carry || action == RA_SELL) {
            //没携带物品，或者是卖，需要改变目标
            robot.targetBerthId = berth.id;
        }
        if (action == RA_BUY) {
            robot.buyAssign = true;
            workbenchesLock.add(workbench.id);//锁住，别人不准选择
            robot.targetWorkBenchId = workbench.id;
            if (!robot.carry) {
                //携带了物品不管
                int toWorkbenchDist = robotMinToWorkbenchDistance(robot, workbench);
                robot.estimateUnloadTime = frameId + toWorkbenchDist + berth.getRobotMinDistance(workbench.pos);
                robot.redundancy = toWorkbenchDist != workbench.remainTime;
            }
            if (!robot.carry && robot.pos.equal(workbench.pos)) {
                //没带物品，且到目标，一定买，然后重新决策一个下一个目标买
                robot.buy();
                robot.buyAssign = false;
            }
            robot.priority = workbench.value;
        } else {
            robot.redundancy = true;
            robot.estimateUnloadTime = frameId + berth.getRobotMinDistance(robot.pos);
            if (gameMap.getBelongToBerthId(robot.pos) == berth.id) {
                robot.pull();
            }
        }
    }

    private int getFastestCollectTime(int goodArriveTime, Berth berth) {
        //这个泊位这个物品到达的时候泊位的数量
        return goodArriveTime;
//        int estimateGoodsNums = berth.goodsNums;
//        for (Robot robot : robots) {
//            if (robot.assigned && robot.targetBerthId == berth.id) {
//                estimateGoodsNums++;
//            }
//        }
//        estimateGoodsNums++;//加上这个货物时间
//        int minDistance = Integer.MAX_VALUE;
//        for (Boat boat : boats) {
//            if (boat.estimateComingBerthId == berth.id) {
//                //来的是同一搜
//                int comingTime = getBoatToBerthDistance(berth, boat);
//                int remainSpace = boat.targetId == berth.id ? boat.capacity - boat.num : boat.capacity;
//                if (remainSpace >= estimateGoodsNums) {
//                    minDistance = min(minDistance, comingTime);
//                } else {
//                    minDistance = min(minDistance, comingTime + 2 * berth.transportTime);
//                }
//            } else {
//                int remainSpace = boat.capacity;
//                int id = boat.estimateComingBerthId == -1 ? 0 : boat.estimateComingBerthId;
//                int comingTime = getBoatToBerthDistance(berths[id], boat);
//                remainSpace -= berths[id].goodsNums;
//                //去到估计的位置，在加一来一回
//                if (remainSpace >= estimateGoodsNums) {
//                    minDistance = min(minDistance, comingTime + berths[id].transportTime + berth.transportTime);
//                } else {
//                    //去另一个泊位装满，再回去，再去你这里
//                    minDistance = min(minDistance, comingTime + berths[id].transportTime + 3 * berth.transportTime);
//                }
//
//            }
//        }
//
//
//        //检查所有的船，找出这个泊位的这个物品最快被什么时候消费
//        //没到达也没法消费
//        return max(goodArriveTime, minDistance) + estimateGoodsNums / berth.loadingSpeed;
    }

    private boolean input() throws IOException {
        String line = inStream.readLine();
        printMost(line);
        if (line == null) {
            return false;
        }
        String[] parts = line.trim().split(" ");
        int tmp = frameId;
        frameId = Integer.parseInt(parts[0]);
        curFrameDiff = frameId - tmp;
        if (tmp + 1 != frameId) {
            jumpCount += frameId - tmp - 1;
        }
        money = Integer.parseInt(parts[1]);


        //新增工作台
        int num = getIntInput();
        ArrayList<Integer> deleteIds = new ArrayList<>();//满足为0的删除
        for (Map.Entry<Integer, Workbench> entry : workbenches.entrySet()) {
            Workbench workbench = entry.getValue();
            workbench.remainTime -= curFrameDiff;//跳帧也要减过去
            if (workbench.remainTime <= 0) {
                deleteIds.add(entry.getKey());
            }
        }
        for (Integer id : deleteIds) {
            workbenches.remove(id);
        }
        deleteIds.clear();
        for (int i = 1; i <= num; i++) {
            Workbench workbench = new Workbench(workbenchId);
            workbench.input(gameMap);
            if (workbench.value == 0) {
                //不管，反正计算过了
                continue;
            }
            totalValue += workbench.value;
            workbenches.put(workbenchId, workbench);
            workbenchId++;
            goodAvgValue = totalValue / workbenchId;
        }

        ROBOTS_PER_PLAYER = getIntInput();
        //机器人
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
//            if (robots[i] == null) {
//                robots[i] = new Robot(this);
//            }
            robots[i].input();
        }


        BOATS_PER_PLAYER = getIntInput();
        //船
        for (int i = 0; i < BOATS_PER_PLAYER; i++) {
//            if (boats[i] == null) {
//                boats[i] = new Boat(boatCapacity);
//            }
            boats[i].input();
        }
        String okk = inStream.readLine();
        printMost(okk);
        return true;
    }
}
