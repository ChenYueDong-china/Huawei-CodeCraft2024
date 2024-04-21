package com.huawei.codecraft;

import java.io.IOException;
import java.util.*;

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
    public final HashMap<Integer, Workbench2> workbenches = new HashMap<>();
    public final HashSet<Integer> workbenchesLock = new HashSet<>();//锁住某些工作台
    public final HashSet<Integer> workbenchesPermanentLock = new HashSet<>();//万一自己,没拿到，永久锁住


    public int totalValue = 0;
    public int goodAvgValue = 0;
    public int totalPullGoodsValues = 0;//总的卖的货物价值
    public int totalPullGoodsCount = 0;//总的卖的货物价值
    public double avgPullGoodsValue = 150;//总的卖的货物价值
    public double avgPullGoodsTime = 15;//10个泊位平均卖货时间
    public final ArrayList<SimpleRobot> totalRobots = new ArrayList<>();
    public final ArrayList<SimpleBoat> totalBoats = new ArrayList<>();
    public final ArrayList<Robot> robots = new ArrayList<>();
    public final ArrayList<Boat> boats = new ArrayList<>();
    @SuppressWarnings("unchecked")
    public final HashSet<Integer>[] robotLock = new HashSet[MAX_ROBOTS_PER_PLAYER];
    public final ArrayList<Berth> berths = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public ArrayList<Point>[] robotsPredictPath = new ArrayList[MAX_ROBOTS_PER_PLAYER];
    @SuppressWarnings("unchecked")
    public ArrayList<Point>[] robotsAvoidOtherPoints = new ArrayList[MAX_ROBOTS_PER_PLAYER];//避让其他人的点

    public int curFrameDiff = 1;
    int jumpCount = 0;

    //boat的闪现位置，闪现泊位不用算

    public ArrayList<Point> boatFlashCandidates;
    //public PointWithDirection[][] boatFlashMainChannelPoint = new PointWithDirection[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];


    public ArrayList<Point> robotPurchasePoint = new ArrayList<>();
    public ArrayList<Integer> robotPurchaseCount = new ArrayList<>();
    public ArrayList<Point> boatPurchasePoint = new ArrayList<>();
    public ArrayList<BoatSellPoint> boatSellPoints = new ArrayList<>();

    private int boatCapacity;

    public int totalWorkbenchLoopDistance = 0;
    public int totalValidWorkBenchCount = 0;
    public int totalValidBerthCount = 0;
    public double avgBerthLoopDistance = -1;//平均工作台循环距离
    public double avgWorkBenchLoopDistance = -1;//平均货物循环距离
    public double estimateMaxRobotCount = 0;//代码估计的
    public double estimateMaxBoatCount = 0;


    public Queue<Workbench2> workbenchCache = new ArrayDeque<>();
    public Queue<Robot> pendingRobots = new ArrayDeque<>();
    public Queue<Boat> pendingBoats = new ArrayDeque<>();

    public void init() throws IOException {

        long startTime = System.currentTimeMillis();

        char[][] mapData = new char[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
        gameMap = new GameMap();
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            fgets(mapData[i], inStream);
            String ok = inStream.readLine();
            printMost(ok);
            outStream.print("OK\n");
            outStream.flush();
        }

        for (int i = 0; i < mapData.length; i++) {
            for (int j = 0; j < mapData[0].length; j++) {
                if (mapData[i][j] == 'R') {
                    robotPurchasePoint.add(new Point(i, j));
                    robotPurchaseCount.add(0);
                } else if (mapData[i][j] == 'S') {
                    boatPurchasePoint.add(new Point(i, j));
                } else if (mapData[i][j] == 'T') {
                    boatSellPoints.add(new BoatSellPoint(new Point(i, j)));
                }
            }
        }
        gameMap.setMap(mapData);
        BackgroundThread.Instance().init();
        int questionId = BackgroundThread.Instance().sendQuestion("下列哪种材质常用于制作快递袋，" +
                "具有防水和防撕裂的特点？ A. 塑料 B. 纸板 C. 布料 D. 金属");
        BoatDijkstra boatDijkstra = new BoatDijkstra();
        for (int i = 0; i < boatSellPoints.size(); i++) {
            boatSellPoints.get(i).init(i, gameMap,boatDijkstra);
        }
        BERTH_PER_PLAYER = getIntInput();
        //码头
        for (int i = 0; i < BERTH_PER_PLAYER; i++) {
            berths.add(new Berth());
        }
        for (Berth berth : berths) {
            String s = inStream.readLine();
            printMost(s);
            String[] parts = s.trim().split(" ");
            int id = Integer.parseInt(parts[0]);
            berth.corePoint.x = Integer.parseInt(parts[1]);
            berth.corePoint.y = Integer.parseInt(parts[2]);
            berth.loadingSpeed = Integer.parseInt(parts[3]);
            berth.id = id;
            berth.init(gameMap,boatDijkstra);
        }


        boatCapacity = getIntInput();
        String okk = inStream.readLine();
        printMost(okk);


        for (int i = 0; i < robotLock.length; i++) {
            robotLock[i] = new HashSet<>();
        }
        for (int i = 0; i < robotsAvoidOtherPoints.length; i++) {
            robotsAvoidOtherPoints[i] = new ArrayList<>();
        }
        //更新berth循环距离
        int totalBerthLoopDistance = 0;
        for (Berth berth : berths) {
            if (berth.minSellDistance != Integer.MAX_VALUE) {
                totalBerthLoopDistance += 2 * berth.minSellDistance + (int) ceil(1.0 * boatCapacity / berth.loadingSpeed);
                totalValidBerthCount++;
            }
        }

        avgBerthLoopDistance = 1.0 * totalBerthLoopDistance / max(1, totalValidBerthCount);

        long l1 = System.currentTimeMillis();
        boatFlashCandidates = new ArrayList<>();
//        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
//            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
//                for (int k = 0; k < DIR.length / 2; k++) {
//                    if (gameMap.boatIsAllInMainChannel(new Point(i, j), k)) {
//                        boatFlashCandidates.add(new Point(i, j));
//                        break;
//                    }
//                }
//            }
//        }
//        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
//            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
//                if (gameMap.boatCanReach(i, j)) {
//                    //留200ms阈值,超时不更新，到时候再更新
//                    long curTime = System.currentTimeMillis();
//                    if (curTime - startTime > 1000 * 19 + 500) {
//                        break;
//                    }
//                    boatUpdateFlashPoint(i, j);
//                }
//            }
//        }

        long l2 = System.currentTimeMillis();
        printError("boat flash target update time:" + (l2 - l1));
        long l11 = System.currentTimeMillis();
        printError("initTime:" + (l11 - startTime));
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory(); // 总内存
        long freeMemory = runtime.freeMemory(); // 可用内存
        long usedMemory = totalMemory - freeMemory; // 已使用内存
        printError("Total Memory (bytes): " + totalMemory);
        printError("Free Memory (bytes): " + freeMemory);
        printError("Used Memory (bytes): " + usedMemory);
        printError("--------------------------");
        System.gc();
        BackgroundThread.Instance().getAnswer(questionId);
        outStream.print("OK\n");
        outStream.flush();
    }

    private void boatUpdateFlashPoint(int i, int j) {
        int minDistance = Integer.MAX_VALUE;
        int maxRight = 0;
        int maxLeft = 0;
        int maxTop = 0;
        int maxBottom = 0;
        Point bestPoint = null;
        for (Point candidate : boatFlashCandidates) {
            int curDistance = abs(candidate.x - i) + abs(candidate.y - j);
            if (curDistance > minDistance) {
                continue;//大于立马排除
            }
            //后面按照哪个方向计算
            int curRight = max(candidate.y - j, 0);
            int curLeft = max(j - candidate.y, 0);
            int curTop = max(i - candidate.x, 0);
            int curBottom = max(candidate.x - i, 0);
            if (curDistance < minDistance || curRight > maxRight || (curRight == maxRight && curLeft > maxLeft) || (curRight == maxRight && curLeft == maxLeft && curTop > maxTop) || (curRight == maxRight && curLeft == maxLeft && curTop == maxTop && curBottom > maxBottom)) {
                bestPoint = candidate;
                minDistance = curDistance;
                maxRight = curRight;
                maxLeft = curLeft;
                maxTop = curTop;
                maxBottom = curBottom;
            }
        }
        // 找到最好的点
        assert bestPoint != null;
//        for (int k = 0; k < DIR.length / 2; k++) {
//            if (gameMap.boatIsAllInMainChannel(bestPoint, k)) {
//                //找到传送点
//                boatFlashMainChannelPoint[i][j] = new PointWithDirection(bestPoint, k);
//            }
//        }
    }

//    public ArrayList<PointWithDirection> boatToBerthWithPruning(Boat boat, Berth berth, int maxDeep, ArrayList<ArrayList<PointWithDirection>> otherPath, ArrayList<Integer> otherIds
//    ) {
//        boat.targetBerthId = berth.id;
//        return boatMoveToBerth(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction)
//                , berth.id, new PointWithDirection(new Point(berth.corePoint), berth.coreDirection), maxDeep, berth.berthAroundPoints.size(), boat.id
//                , otherPath, otherIds, boat.remainRecoveryTime, berth.boatMinDistance);
//    }

    //    public ArrayList<PointWithDirection> boatToBerth(Boat boat, Berth berth, int maxDeep, ArrayList<ArrayList<PointWithDirection>> otherPath, ArrayList<Integer> otherIds) {
//        boat.targetBerthId = berth.id;
//        return boatMoveToBerth(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction), berth.id, new PointWithDirection(new Point(berth.corePoint), berth.coreDirection), maxDeep, berth.berthAroundPoints.size(), boat.id, otherPath, otherIds
//                , boat.remainRecoveryTime, null);
//    }
//
//    public ArrayList<PointWithDirection> boatToBerthHeuristic(Boat boat, Berth berth) {
//        boat.targetBerthId = berth.id;
//        return boatMoveToBerthHeuristic(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction), berth.id, new PointWithDirection(new Point(berth.corePoint), berth.coreDirection), boat.remainRecoveryTime, berth.boatMinDistance);
//    }
//
//    public ArrayList<PointWithDirection> boatToBerth(Boat boat, Berth berth, int maxDeep) {
//        boat.targetBerthId = berth.id;
//        return boatMoveToBerth(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction), berth.id, new PointWithDirection(new Point(berth.corePoint), berth.coreDirection), maxDeep, berth.berthAroundPoints.size(), boat.remainRecoveryTime);
//    }
//
//    public ArrayList<PointWithDirection> boatToPoint(Boat boat, PointWithDirection pointWithDirection, int maxDeep) {
//        return boatMoveToPoint(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction), pointWithDirection, maxDeep, boat.remainRecoveryTime);
//    }
//
//    public ArrayList<PointWithDirection> boatToPoint(Boat boat, PointWithDirection pointWithDirection, int maxDeep, ArrayList<ArrayList<PointWithDirection>> otherPath, ArrayList<Integer> otherIds) {
//        return boatMoveToPoint(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction), pointWithDirection, maxDeep, boat.id, otherPath, otherIds, boat.remainRecoveryTime, null);
//    }
//
//    public ArrayList<PointWithDirection> boatToSellPoint(Boat boat, BoatSellPoint sellPoint, int maxDeep, ArrayList<ArrayList<PointWithDirection>> otherPath, ArrayList<Integer> otherIds) {
//        return boatMoveToPoint(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction), new PointWithDirection(sellPoint.point, -1), maxDeep, boat.id, otherPath, otherIds, boat.remainRecoveryTime, sellPoint.boatMinDistance);
//    }
//
//    public ArrayList<Point> robotToBerth(Robot robot, Berth berth, int maxDeep) {
//        return robotMoveToBerth(gameMap, robot.pos, berth.id, maxDeep, null);
//    }
//
//    public ArrayList<Point> robotToBerth(Robot robot, Berth berth, int maxDeep, ArrayList<ArrayList<Point>> otherPaths) {
//        return robotMoveToBerth(gameMap, robot.pos, berth.id, maxDeep, otherPaths);
//    }
//
//    public ArrayList<Point> robotToPoint(Robot robot, Point end, int maxDeep) {
//        return robotMoveToPoint(gameMap, robot.pos, end, maxDeep, null);
//    }
//
//    public ArrayList<Point> robotToPoint(Robot robot, Point end, int maxDeep, ArrayList<ArrayList<Point>> otherPaths) {
//        return robotMoveToPoint(gameMap, robot.pos, end, maxDeep, otherPaths);
//    }
//
    public ArrayList<Point> robotToBerthHeuristic(Robot robot, int berthId, ArrayList<ArrayList<Point>> otherPath, boolean[][] conflictPoints) {
        if (otherPath == null && conflictPoints == null) {
            return robotMoveToBerth(gameMap, robot.pos, berthId, berths.get(berthId).robotMinDistance);
        } else {
            short maxDeep = berths.get(berthId).robotMinDistance[robot.pos.x][robot.pos.y];
            if (conflictPoints != null) {
                maxDeep += 5;//多5帧能让对面也行,不然只能随机了
            }
            return robotMoveToPointBerth(gameMap, robot.pos, berthId, null, maxDeep, otherPath, berths.get(berthId).robotMinDistance, conflictPoints);
        }
    }

//    public ArrayList<Point> robotToPointHeuristic(Robot robot, Point end, ArrayList<ArrayList<Point>> otherPaths, int[][] heuristicCs) {
//        return robotMoveToPointHeuristic(gameMap, robot.pos, end, otherPaths, heuristicCs);
//    }

    public void mainLoop() throws IOException, InterruptedException {
        while (input()) {
            //todo 测试多机器人避让，测试多船避让，测试买船，测试最优的决策
            long l = System.currentTimeMillis();
            dispatch();
            long e = System.currentTimeMillis();
//            printDebug("frameId:" + frameId + ",time:" + (e - l));
            if (e - l > 50) {
                printError("frameId:" + frameId + ",time:" + (e - l));
            }
            if (frameId >= GAME_FRAME - 2) {
                int sellCount = 0;
                for (Boat boat : boats) {
                    sellCount += boat.sellCount;
                }
                printError("jumpTime:" + jumpCount + ",buyRobotCount:" + robots.size() + ",buyBoatCount:" + boats.size() + ",totalValue:" + totalValue + ",pullValue:" + pullScore + ",score:" + money + ",boatSellCount:" + sellCount);
            }
            //留2ms阈值
//            while (BackgroundThread.Instance().IsWorking() && frameStartTime + 15 < System.currentTimeMillis()) {
//                //noinspection BusyWait
//                Thread.sleep(1);
//            }
            outStream.print("OK\n");
            outStream.flush();
        }
        BackgroundThread.Instance().exitThread();
    }


    private void dispatch() {

        long l = System.currentTimeMillis();
//        robotDoAction();
//        boatDoAction();
        if (frameId > 19500 && money >= 3000) {
            //再买一个
            outStream.printf("lbot %d %d %d\n", robotPurchasePoint.get(0).x, robotPurchasePoint.get(0).y, 1);
            Robot robot = new Robot(this, 1);
            robotPurchaseCount.set(0, robotPurchaseCount.get(0) + 1);
            robot.buyFrame = frameId;
            robot.id = robots.size();
            robots.add(robot);
            money -= 3000;
        }

        long r = System.currentTimeMillis();
        printDebug("frame:" + frameId + ",robotRunTime:" + (r - l));
        if (frameId == 1) {
            for (int i = 0; i < 11; i++) {
                outStream.printf("lbot %d %d %d\n", robotPurchasePoint.get(0).x, robotPurchasePoint.get(0).y, 0);
                Robot robot = new Robot(this, 0);
                robotPurchaseCount.set(0, robotPurchaseCount.get(0) + 1);
                robot.buyFrame = frameId;
                robot.id = robots.size();
                robots.add(robot);
            }
        }
//        if (money > 2000 && ROBOTS_PER_PLAYER < 50) {
//            outStream.printf("lbot %d %d %d\n", robotPurchasePoint.get(0).x, robotPurchasePoint.get(0).y, 0);
//            Robot robot = new Robot(this, 0);
//            robotPurchaseCount.set(0, robotPurchaseCount.get(0) + 1);
//            robot.buyFrame = frameId;
//            robot.id = robots.size();
//            robots.add(robot);
//        }
//            boatDoAction();
//            robotAndBoatBuy();

//        if (frameId == 1) {
//            for (int i = 0; i < 1; i++) {
//                outStream.printf("lboat %d %d\n", boatPurchasePoint.get(0).x, boatPurchasePoint.get(0).y);
//                boats.add(new Boat(this, boatCapacity));
//                outStream.printf("lboat %d %d\n", boatPurchasePoint.get(1).x, boatPurchasePoint.get(1).y);
//                boats.add(new Boat(this, boatCapacity));
//            }
//        }
//        if (frameId > 1 && frameId < 500) {
//            Boat boat1 = boats.get(0);
//            boat1.path = boatToPoint(boat1, new PointWithDirection(new Point(111, 98), -1), 9999);
//            Boat boat2 = boats.get(1);
//            boat2.path = boatToPoint(boat2, new PointWithDirection(new Point(49, 120), -1), 9999);
//            //不然肯定dij不对
//            ArrayList<ArrayList<PointWithDirection>> otherPaths = new ArrayList<>();
//            ArrayList<Integer> otherIds = new ArrayList<>();
//            otherPaths.add(boat1.path);
//            otherIds.add(0);
//            if (boatCheckCrash(gameMap, boat2.id, boat2.path, otherPaths, otherIds, Integer.MAX_VALUE) != -1) {
//                //撞了
//                boat2.path = boatToPoint(boat2, new PointWithDirection(new Point(49, 120), -1)
//                        , 9999, otherPaths, otherIds);
//
//            }
//            boat1.finish();
//            boat2.finish();
//        }
    }

    //
//    private void robotAndBoatBuy() {
//        if (money < ROBOT_PRICE) {
//            return;
//        }
//        while (robots.size() < INIT_ROBOT_COUNT && money > ROBOT_PRICE) {
//            buyRobot();
//        }
//        while (boats.size() < INIT_BOAT_COUNT && money > BOAT_PRICE) {
//            if (!buyBoat()) {
//                //下一帧再说
//                return;
//            }
//        }
//        while (robots.size() < MIN_ROBOT_COUNT && money > ROBOT_PRICE) {
//            buyRobot();
//        }
//        while (boats.size() < MIN_BOAT_COUNT && money > BOAT_PRICE) {
//            if (!buyBoat()) {
//                //可能本帧直接买会撞，买不到,下一帧再说
//                return;
//            }
//        }
//        if (money < ROBOT_PRICE || GAME_FRAME == frameId) {
//            return;
//        }
//
//        //动态决定是否买机器人和船
//        int goodCounts = totalValidWorkBenchCount;
//        int remainCount = 0;
//        for (Workbench value : workbenches.values()) {
//            if (value.minSellDistance != Integer.MAX_VALUE) {
//                remainCount++;
//            }
//        }
//        //剩余估计要来的count
//        double speed = 1.0 * goodCounts / frameId;
//        double estimateRemainTotalCount = speed * (GAME_FRAME - frameId);
//        double curRobotCount = 0;//折算的个数
//        for (Robot robot : robots) {
//            curRobotCount += frameId - robot.buyFrame + 1;
//        }
//        curRobotCount /= frameId;
//
//        //估计最大机器人数
//        estimateMaxRobotCount = (estimateRemainTotalCount + remainCount) * avgWorkBenchLoopDistance / (GAME_FRAME - frameId);
//        double remainTotalValue = (estimateRemainTotalCount + remainCount) * goodAvgValue;
//        double curCanGetValue = min(remainTotalValue,
//                1.0 * pullScore / totalValue * remainTotalValue * (0.5 * (robots.size() - curRobotCount)
//                        / curRobotCount + 1));
//        remainTotalValue -= curCanGetValue;
//        int buyRobotCount = 0;
//        double curRemainValue = remainTotalValue;
//        while (robots.size() + buyRobotCount < estimateMaxRobotCount) {
//            double oneRobotValue = curRemainValue * 1.0 * pullScore / totalValue / curRobotCount;
//            if (oneRobotValue > BUY_ROBOT_FACTOR * ROBOT_PRICE) {
//                buyRobotCount++;
//                curRemainValue -= oneRobotValue;
//            } else {
//                break;
//            }
//        }
//
//        //决定是否买船
//        int berthTotalNum = 0;
//        int berthRemainNum = 0;
//        for (Berth berth : berths) {
//            berthTotalNum += berth.totalGoodsNums;
//            berthRemainNum += berth.goodsNums;
//        }
//        for (Boat boat : boats) {
//            if (!boat.carry && boat.targetBerthId != -1) {
//                int needCount = boat.capacity - boat.num;
//                berthRemainNum -= needCount;
//            }
//        }
//        //估计未来到达的货物
//        double berthSpeed = 1.0 * berthTotalNum / frameId;
//        double estimateBerthComingGood = 1.0 * berthSpeed * (GAME_FRAME - frameId) * robots.size() / curRobotCount;
//        estimateMaxBoatCount = (estimateBerthComingGood + berthRemainNum) / boatCapacity * avgBerthLoopDistance / (GAME_FRAME - frameId);
//        double oneBoatValue = boatCapacity * avgPullGoodsValue;
//        double oneBoatCanGetValue = floor(oneBoatValue * (GAME_FRAME - frameId) / avgBerthLoopDistance);
//        int buyBoatCount = 0;
//        while (boats.size() + buyBoatCount < estimateMaxBoatCount) {
//            double oneBoatRealValue = oneBoatCanGetValue * min(estimateMaxBoatCount - buyBoatCount - boats.size(), 1);
//            if (oneBoatRealValue > BUY_BOAT_FACTOR * BOAT_PRICE) {
//                buyBoatCount++;
//            } else {
//                break;
//            }
//        }
//        while (buyBoatCount > 1) {
//            if (money > BOAT_PRICE) {
//                if (buyBoat()) {
//                    buyBoatCount--;
//                } else {
//                    return;
//                }
//            } else {
//                //此时钱不够，必须得等钱来
//                return;
//            }
//        }
//        while (buyRobotCount > 0 && money > ROBOT_PRICE) {
//            buyRobot();
//            buyRobotCount--;
//        }
//        while (buyBoatCount == 1) {
//            if (money > BOAT_PRICE) {
//                if (buyBoat()) {
//                    buyBoatCount--;
//                } else {
//                    return;
//                }
//            } else {
//                //此时钱不够，必须得等钱来
//                return;
//            }
//        }
//
//    }
//
//    private boolean buyBoat() {
//        int index = getBestBoatBuyPos();
//        if (index == -1) {
//            return false;
//        }
//        outStream.printf("lboat %d %d\n", boatPurchasePoint.get(index).x, boatPurchasePoint.get(index).y);
//        boats.add(new Boat(this, boatCapacity));
//        money -= BOAT_PRICE;
//        return true;
//    }
//
//    private void buyRobot() {
//        int index = getBestBuyRobotPos();
//        outStream.printf("lbot %d %d %d\n", robotPurchasePoint.get(index).x, robotPurchasePoint.get(index).y, 0);
//        Robot robot = new Robot(this);
//        robotPurchaseCount.set(index, robotPurchaseCount.get(index) + 1);
//        robot.buyFrame = frameId;
//        robots.add(robot);
//        money -= ROBOT_PRICE;
//    }
//
//    private int getBestBoatBuyPos() {
//        //货物最多的berth
//        int[] remainGoodsCount = new int[BERTH_PER_PLAYER];
//        for (Berth berth : berths) {
//            remainGoodsCount[berth.id] = berth.goodsNums;
//        }
//        for (Boat boat : boats) {
//            if (boat.targetBerthId != -1) {
//                int needCount = boat.carry ? boat.capacity : boat.capacity - boat.num;
//                remainGoodsCount[boat.targetBerthId] -= needCount;
//            }
//        }
//        int maxCount = -GAME_FRAME;
//        int bestBerthId = -1;
//        for (int i = 0; i < remainGoodsCount.length; i++) {
//            if (remainGoodsCount[i] > maxCount) {
//                maxCount = remainGoodsCount[i];
//                bestBerthId = i;
//            }
//        }
//        int bestI = -1;
//        int minDistance = Integer.MAX_VALUE;
//        for (int i = 0; i < boatPurchasePoint.size(); i++) {
//            Point point = boatPurchasePoint.get(i);
//            int distance = berths.get(bestBerthId).boatMinDistance[point.x][point.y][0];
//            if (distance < minDistance) {
//                minDistance = distance;
//                bestI = i;
//            }
//        }
//        //冲突检测
//        for (Boat boat : boats) {
//            PointWithDirection buyPoint = new PointWithDirection(boatPurchasePoint.get(bestI), 0);
//            if (boatCheckCrash(gameMap, buyPoint, new PointWithDirection(boat.corePoint, boat.direction))) {
//                bestI = -1;//这一帧，买不了
//                break;
//            }
//        }
//        return bestI;
//    }
//
//    private int getBestBuyRobotPos() {
//        double maxProfit = 0;
//        int bestI = -1;
//        int bestWorkBenchId = -1;
//        for (int i = 0; i < robotPurchasePoint.size(); i++) {
//            for (Workbench buyWorkbench : workbenches.values()) {
//                if (workbenchesLock.contains(buyWorkbench.id)) {
//                    continue;//别人选择过了
//                }
//                Point point = robotPurchasePoint.get(i);
//                int buyTime = buyWorkbench.getMinDistance(point);
//                int sellTime = buyWorkbench.minSellDistance;
//                if (frameId + buyTime + sellTime > GAME_FRAME) {
//                    continue;
//                }
//                double value = buyWorkbench.value;
//                value += DISAPPEAR_REWARD_FACTOR * value * (WORKBENCH_EXIST_TIME - buyWorkbench.remainTime) / WORKBENCH_EXIST_TIME;
//                double profit = value / (buyTime + sellTime);
//                if (profit > maxProfit) {
//                    maxProfit = profit;
//                    bestI = i;
//                    bestWorkBenchId = buyWorkbench.id;
//                }
//            }
//        }
//        if (bestWorkBenchId != -1) {
//            workbenchesLock.add(bestWorkBenchId);
//        } else {
//            //咋搞
//            int minBuyCount = Integer.MAX_VALUE;
//            for (int i = 0; i < robotPurchaseCount.size(); i++) {
//                if (robotPurchaseCount.get(i) < minBuyCount) {
//                    minBuyCount = robotPurchaseCount.get(i);
//                    bestI = i;
//                }
//            }
//        }
//        return bestI;
//    }
//
//
    private void boatDoAction() {
        //船只选择回家
        while (true) {
            if (!boatGreedySell()) {
                break;
            }  //决策
        }

        //船只贪心去买卖，跟机器人做一样的决策
        int[] goodsNumList = new int[BERTH_PER_PLAYER];
        for (int i = 0; i < goodsNumList.length; i++) {
            goodsNumList[i] = berths.get(i).goodsNums;
        }
        for (Robot robot : robots) {
            if (!robot.assigned) {
                continue;
            }
            if (robot.targetBerthId != -1) {
                goodsNumList[robot.targetBerthId]++;
            }
        }
        while (true) {
            if (!boatGreedyBuy1(goodsNumList)) {
                break;
            }  //决策
        }
        //移动
        //选择路径，碰撞避免
        Boat[] tmpBoats = new Boat[BOATS_PER_PLAYER];
        for (int i = 0; i < BOATS_PER_PLAYER; i++) {
            tmpBoats[i] = boats.get(i);
        }
        sortBoats(tmpBoats);
        //1.选择路径,和修复路径
        ArrayList<ArrayList<PointWithDirection>> otherPaths = new ArrayList<>();
        ArrayList<Integer> otherIds = new ArrayList<>();
        for (Boat boat : tmpBoats) {
            if (!boat.assigned) {
                continue;
            }
            //后面可以考虑启发式搜，目前强搜
            if (boat.carry) {
                //卖
                boat.path = boatToSellPointHeuristic(boat, boatSellPoints.get(boat.targetSellId));

            } else {
                //买
                assert boat.targetBerthId != -1;
                boat.path = boatToBerthHeuristic(boat, berths.get(boat.targetBerthId));
                assert !boat.path.isEmpty();
                //不然肯定dij不对
            }
            otherPaths.add(boat.path);
            otherIds.add(boat.id);
        }

        otherPaths = new ArrayList<>();
        otherIds = new ArrayList<>();
//        for (int i = 0; i < tmpBoats.length; i++) {
//            Boat boat = tmpBoats[i];
//            ArrayList<PointWithDirection> myPath = new ArrayList<>();
//            if (boat.assigned) {
//                for (int j = 0; j < min(BOAT_PREDICT_DISTANCE, boat.path.size()); j++) {
//                    myPath.add(boat.path.get(j));
//                }
//            } else {
//                //没assign
//                for (int j = 0; j < BOAT_PREDICT_DISTANCE; j++) {
//                    myPath.add(new PointWithDirection(boat.corePoint, boat.direction));
//                }
//                boat.path.clear();
//                boat.path.addAll(myPath);
//            }
//            int crashId = boatCheckCrash(gameMap, boat.id, myPath, otherPaths, otherIds, BOAT_AVOID_DISTANCE);
//            if (crashId != -1) {
//                boats.get(crashId).beConflicted = FPS;
//            }
//            if (crashId != -1 && boat.status != 1) {
//                //此时可以避让，则开始避让
//                //避让
//                for (boolean[] conflictPoint : gameMap.commonConflictPoints) {
//                    Arrays.fill(conflictPoint, false);
//                }
//                for (boolean[] commonNoResultPoint : gameMap.commonNoResultPoints) {
//                    Arrays.fill(commonNoResultPoint, false);
//                }
//                for (ArrayList<PointWithDirection> otherPath : otherPaths) {
//                    //别人所在位置锁住
//                    PointWithDirection pointWithDirection = otherPath.get(0);
//                    ArrayList<Point> points = gameMap.getBoatPoints(pointWithDirection.point, pointWithDirection.direction);
//                    for (Point point : points) {
//                        if (!gameMap.isBoatMainChannel(point.x, point.y)) {
//                            gameMap.commonConflictPoints[point.x][point.y] = true;
//                        }
//                    }
//                }
//                for (int j = 0; j < otherPaths.size(); j++) {
//                    if (otherIds.get(j) == crashId) {
//                        //撞到那个人的预测路径不是结果点
//                        ArrayList<PointWithDirection> pointWithDirections = otherPaths.get(j);
//                        for (PointWithDirection pointWithDirection : pointWithDirections) {
//                            ArrayList<Point> points = gameMap.getBoatPoints(pointWithDirection.point, pointWithDirection.direction);
//                            for (Point point : points) {
//                                if (!gameMap.isBoatMainChannel(point.x, point.y)) {
//                                    gameMap.commonNoResultPoints[point.x][point.y] = true;
//                                }
//                            }
//                        }
//                    }
//                }
//                ArrayList<PointWithDirection> result = boatGetSafePoints(gameMap, gameMap.boatCommonCs, myPath.get(0), gameMap.commonConflictPoints, gameMap.commonNoResultPoints, BOAT_AVOID_CANDIDATE_SIZE);
//                if (!result.isEmpty()) {
//                    PointWithDirection selectPoint = null;
//                    double minDistance = Integer.MAX_VALUE;
//                    for (PointWithDirection pointWithDirection : result) {
//                        double curDis = (gameMap.boatCommonCs[pointWithDirection.point.x][pointWithDirection.point.y][pointWithDirection.direction] >> 2);
//                        //到目标距离
//                        if (boat.assigned) {
//                            double toTargetDistance;
//                            if (boat.carry) {
//                                //买
//                                toTargetDistance = boatSellPoints.get(boat.targetSellId).getMinDistance(pointWithDirection.point, pointWithDirection.direction);
//                            } else {
//                                toTargetDistance = berths.get(boat.targetBerthId).getBoatMinDistance(pointWithDirection.point, pointWithDirection.direction);
//                            }
//                            curDis = curDis + toTargetDistance / 2;
//                        }
//                        if (curDis < minDistance) {
//                            selectPoint = pointWithDirection;
//                            minDistance = curDis;
//                        }
//                    }
//                    if (selectPoint != null) {
//                        int oneDistance = (gameMap.boatCommonCs[selectPoint.point.x][selectPoint.point.y][selectPoint.direction] >> 2);
//                        if (boat.assigned) {
//                            if (boat.carry) {
//                                oneDistance += boatSellPoints.get(boat.targetSellId).getMinDistance(selectPoint.point, selectPoint.direction);
//                            } else {
//                                oneDistance += berths.get(boat.targetBerthId).getBoatMinDistance(selectPoint.point, selectPoint.direction);
//                            }
//                        }
//                        //计算闪现需要时间
//                        PointWithDirection mid = getBoatFlashDeptPoint(boat.corePoint);
//                        //计算到达时间
//                        int waitTime = 1 + abs(boat.corePoint.x - mid.point.x) + abs(boat.corePoint.y - mid.point.y);
//                        int twoDistance = waitTime;
//                        if (waitTime == 1) {//闪现原地,一定不选
//                            twoDistance += MAP_FILE_ROW_NUMS * MAP_FILE_COL_NUMS;
//                        }
//                        if (boat.assigned) {
//                            if (boat.carry) {
//                                //买
//                                twoDistance += boatSellPoints.get(boat.targetSellId).getMinDistance(mid.point, mid.direction);
//                            } else {
//                                twoDistance += berths.get(boat.targetBerthId).getBoatMinDistance(mid.point, mid.direction);
//                            }
//                        }
//
//                        if (twoDistance <= oneDistance) {
//                            //闪现避让
//                            myPath.clear();
//                            myPath.add(new PointWithDirection(boat.corePoint, boat.direction));
//                            for (int j = 0; j < waitTime; j++) {
//                                myPath.add(mid);
//                            }
//                        } else {
//                            //正常避让
//                            myPath = backTrackPath(gameMap, result.get(0), gameMap.boatCommonCs, 0);
//                            if (myPath.size() == 1) {
//                                myPath.add(myPath.get(0));
//                            }
//                            while (myPath.size() > BOAT_PREDICT_DISTANCE) {
//                                myPath.remove(myPath.size() - 1);
//                            }
//                        }
//                        boat.avoid = true;
//                    }
//                } else {
//                    //无路可走，可以尝试提高优先级，不然就
//                    if (boat.forcePri > 2) {
//                        //闪现避让
//                        //计算闪现需要时间
//                        printError("no path can go, flash");
//                        PointWithDirection mid = getBoatFlashDeptPoint(boat.corePoint);
//                        //计算到达时间
//                        int waitTime = 1 + abs(boat.corePoint.x - mid.point.x) + abs(boat.corePoint.y - mid.point.y);
//                        myPath.clear();
//                        myPath.add(new PointWithDirection(boat.corePoint, boat.direction));
//                        for (int j = 0; j < waitTime; j++) {
//                            myPath.add(mid);
//                        }
//                        boat.avoid = true;
//                    } else {
//                        boat.beConflicted = FPS;
//                        boat.forcePri += 1;
//                        otherPaths.clear();
//                        otherIds.clear();
//                        sortBoats(tmpBoats);
//                        i = -1;
//                        continue;
//                    }
//                }
//            }
//            //自己不动，切高优先级的撞到你，则不动
//            PointWithDirection cur = myPath.get(0);
//            PointWithDirection next = myPath.get(1);
//            //不是整个船在主隧道上，且自己不动，则需要别人检查是否这个帧id小于他，或者id大于这一帧撞你移动后,都是同一个位置
//            if (cur.equals(next) && !gameMap.boatIsAllInMainChannel(cur.point, cur.direction)) {
//                for (int j = 0; j < otherPaths.size(); j++) {
//                    assert otherPaths.get(j).size() >= 2;
//                    PointWithDirection myStart = myPath.get(0);
//                    assert myStart.equals(myPath.get(1));
//                    PointWithDirection otherNext = otherPaths.get(j).get(1);
//                    if (boatCheckCrash(gameMap, otherNext, myStart)) {
//                        PointWithDirection otherStart = otherPaths.get(j).get(0);
//                        otherPaths.get(j).clear();
//                        for (int k = 0; k < 2; k++) {
//                            otherPaths.get(j).add(otherStart);
//                        }
//                        boats.get(otherIds.get(j)).avoid = true;
//                    }
//                }
//            }
//            otherPaths.add(myPath);
//            otherIds.add(boat.id);
//        }
        for (int i = 0; i < otherPaths.size(); i++) {
            int id = otherIds.get(i);//改成避让路径
            if (boats.get(id).avoid) {
                boats.get(id).path.clear();
                boats.get(id).path.addAll(otherPaths.get(i));
            }
        }
        for (Boat boat : boats) {
            boat.finish();
            if (boat.beConflicted-- < 0 && boat.forcePri != 0) {
                boat.forcePri = 0;
            }
        }
    }

    private ArrayList<PointWithDirection> boatToBerthHeuristic(Boat boat, Berth berth) {
        short[][][] heuristicCs = berth.boatMinDistance;
        int maxDeep = heuristicCs[boat.corePoint.x][boat.corePoint.y][boat.direction];
        maxDeep += 5;//碰到就会立马闪现，这个不一定准，所以加一点
        return boatMoveToBerthSellPoint(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction)
                , null, berth.id
                , new PointWithDirection(berth.corePoint, berth.coreDirection), boat.remainRecoveryTime
                , maxDeep, -1, null, null, heuristicCs, null);
    }

    private ArrayList<PointWithDirection> boatToSellPointHeuristic(Boat boat, BoatSellPoint boatSellPoint) {
        short[][][] heuristicCs = boatSellPoint.boatMinDistance;
        int maxDeep = heuristicCs[boat.corePoint.x][boat.corePoint.y][boat.direction];
        return boatMoveToBerthSellPoint(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction)
                , new PointWithDirection(new Point(boatSellPoint.point), -1), -1
                , null, boat.remainRecoveryTime
                , maxDeep, -1, null, null, heuristicCs, null);
    }

    public PointWithDirection getBoatFlashDeptPoint(Point point) {
//        if (boatFlashMainChannelPoint[point.x][point.y] == null) {
//            boatUpdateFlashPoint(point.x, point.y);
//        }
//        return boatFlashMainChannelPoint[point.x][point.y];
        return null;
    }


    private void sortBoats(Boat[] tmpBoats) {
        Arrays.sort(tmpBoats, (o1, o2) -> {
            if (o1.forcePri != o2.forcePri) {//暴力优先级最高
                return o2.forcePri - o1.forcePri;
            }
            if (o1.num != o2.num) {
                return o2.num - o1.num;//带的越多越珍贵
            }
            if (o1.assigned ^ o2.assigned) {
                //有路径的放前面
                return o1.assigned ? -1 : 1;
            }
            return o1.id - o2.id;
        });
        for (Boat boat : boats) {
            boat.avoid = false;
        }
    }

    private boolean boatGreedySell() {

        class Stat implements Comparable<Stat> {
            final Boat boat;
            final BoatSellPoint boatSellPoint;
            final double profit;

            public Stat(Boat boat, BoatSellPoint boatSellPoint, double profit) {
                this.boat = boat;
                this.boatSellPoint = boatSellPoint;
                this.profit = profit;
            }

            @Override
            public int compareTo(Stat b) {
                return Double.compare(b.profit, profit);
            }
        }
        ArrayList<Stat> stat = new ArrayList<>();
        Boat bestBoat = null;
        BoatSellPoint bestSellPoint = null;
        double bestProfit = -GAME_FRAME;
        for (Boat boat : boats) {
            if (boat.assigned || boat.num == 0) {
                continue;
            }
            int minSellDistance = Integer.MAX_VALUE;
            BoatSellPoint selectPoint = null;
            for (BoatSellPoint boatSellPoint : boatSellPoints) {
                if (!boatSellPoint.canReach(boat.corePoint, boat.direction)) {
                    continue;
                }
                int distance = boatSellPoint.getMinDistance(boat.corePoint, boat.direction);
                //避让时间,稍微增加一帧
                distance += boat.remainRecoveryTime;
                if (distance < minSellDistance) {
                    minSellDistance = distance;
                    selectPoint = boatSellPoint;
                }
            }
            if (selectPoint == null) {
                continue;
            }
            if (boat.num == boat.capacity || frameId + minSellDistance >= GAME_FRAME - min(FPS / 5, boats.size() * 5)) {
                //卖
                int value = boat.num;
                double profit = 1.0 * value / minSellDistance;
                if (profit > bestProfit) {
                    bestProfit = profit;
                    bestBoat = boat;
                    bestSellPoint = selectPoint;
                }
            }
        }
        if (bestBoat == null) {
            return false;
        }
        Boat boat = bestBoat;
        BoatSellPoint boatSellPoint = bestSellPoint;
        boat.assigned = true;
        boat.targetSellId = boatSellPoint.id;
        if (!boat.carry) {
            boat.sellCount++;
        }
        boat.carry = true;
        if (boat.status == 2) {
            //下一个状态不知道,现在状态可以变为0，直接不动,如果不变会认为仍然装货，不动
            boat.status = 0;
            boat.targetBerthId = -1;//重新买
        }
        return true;
    }


    private boolean boatGreedyBuy1(int[] goodsNumList) {


        for (Berth berth : berths) {
            //停靠在泊位上了
            if (berth.curBoatId != -1 && berth.goodsNums > 0
                    && !boats.get(berth.curBoatId).buyAssign
                    && !boats.get(berth.curBoatId).carry) {
                //没去卖
                Boat boat = boats.get(berth.curBoatId);
                decisionBoatBuy(boat, berth, boatSellPoints.get(boat.targetSellId), goodsNumList);
                return true;
            }
        }
        Boat bestBoat = null;
        Berth bestBerth = null;
        BoatSellPoint bestSellPoint = null;
        double bestProfit = -GAME_FRAME;

        for (int i = 0; i < berths.size(); i++) {
            Berth berth = berths.get(i);
            int berthGoodNum = goodsNumList[i];
            //找最近的船
            Boat selectBoat = null;
            int minBuyDistance = Integer.MAX_VALUE;
            for (Boat boat : boats) {
                if (boat.buyAssign) {
                    continue;
                }
                if (!berth.boatCanReach(boat.corePoint, boat.direction)) {
                    continue;
                }
                int distance = boatMinToBerthDistance(boat, berth);
                if (boat.carry) {
                    distance -= boatSellPoints.get(boat.targetSellId).getMinDistance(boat.corePoint, boat.direction);
                }
                if (distance < minBuyDistance) {
                    minBuyDistance = distance;
                    selectBoat = boat;
                }
            }

            int minSellDistance = Integer.MAX_VALUE;
            BoatSellPoint selectSellPoint = null;
            for (BoatSellPoint boatSellPoint : boatSellPoints) {
                int distance = boatSellPoint.getMinDistance(berth.corePoint, berth.coreDirection);
                if (distance < minSellDistance) {
                    minSellDistance = distance;
                    selectSellPoint = boatSellPoint;
                }
            }
            if (selectBoat == null || selectSellPoint == null) {
                continue;
            }
            int toSellDistance = 0;
            if (selectBoat.carry) {
                toSellDistance = boatSellPoints.get(selectBoat.targetSellId).getMinDistance(selectBoat.corePoint, selectBoat.direction);
            }
            if (frameId + toSellDistance + minBuyDistance + minSellDistance >= GAME_FRAME) {
                continue;//不做决策
            }
            //价值是船上的个数加泊位个数
            int needCount = selectBoat.carry ? selectBoat.capacity : selectBoat.capacity - selectBoat.num;
            int value = min(needCount, berthGoodNum);
            double profit = 1.0 * value / (minSellDistance + minBuyDistance);
            if (!selectBoat.carry && selectBoat.targetBerthId == berth.id
                    && berth.curBoatId != selectBoat.id) {//在泊位上，且目前没货了，不增加
                //防止出问题
                profit *= (1 + BOAT_DYNAMIC_SAME_TARGET_FACTOR);
            }
            if (profit > bestProfit) {
                bestProfit = profit;
                bestBoat = selectBoat;
                bestBerth = berth;
                bestSellPoint = selectSellPoint;
            }
        }
        if (bestBoat == null) {
            return false;
        }
        decisionBoatBuy(bestBoat, bestBerth, bestSellPoint, goodsNumList);
        return true;
    }

    //
//    private boolean boatGreedyBuy2(int[] goodsNumList) {
//        class Stat implements Comparable<Stat> {
//            final Boat boat;
//            final Berth berth;
//
//            final BoatSellPoint sellPoint;
//
//            final double profit;
//
//            public Stat(Boat boat, Berth berth, BoatSellPoint sellPoint, double profit) {
//                this.boat = boat;
//                this.berth = berth;
//                this.sellPoint = sellPoint;
//                this.profit = profit;
//            }
//
//            @Override
//            public int compareTo(Stat b) {
//                return Double.compare(b.profit, profit);
//            }
//        }
//        for (Berth berth : berths) {
//            //停靠在泊位上了
//            if (berth.curBoatId != -1 && berth.goodsNums > 0 && !boats.get(berth.curBoatId).buyAssign && !boats.get(berth.curBoatId).carry) {
//                //没去卖
//                Boat boat = boats.get(berth.curBoatId);
//                decisionBoatBuy(boat, berth, boatSellPoints.get(boat.targetSellId), goodsNumList);
//                return true;
//            }
//        }
//        ArrayList<Stat> stat = new ArrayList<>();
//
//        for (int i = 0; i < berths.size(); i++) {
//            Berth berth = berths.get(i);
//            int berthGoodNum = goodsNumList[i];
//            //找最近的船
//            Boat selectBoat = null;
//            int minBuyDistance = Integer.MAX_VALUE;
//            for (Boat boat : boats) {
//                if (boat.buyAssign) {
//                    continue;
//                }
//                if (!berth.boatCanReach(boat.corePoint, boat.direction)) {
//                    continue;
//                }
//                int distance = boatMinToBerthDistance(boat, berth);
//                if (boat.carry) {
//                    distance -= boatSellPoints.get(boat.targetSellId).getMinDistance(boat.corePoint, boat.direction);
//                }
//                if (distance < minBuyDistance) {
//                    minBuyDistance = distance;
//                    selectBoat = boat;
//                }
//            }
//
//            int minSellDistance = Integer.MAX_VALUE;
//            BoatSellPoint selectSellPoint = null;
//            for (BoatSellPoint boatSellPoint : boatSellPoints) {
//                int distance = boatSellPoint.getMinDistance(berth.corePoint, berth.coreDirection);
//                if (distance < minSellDistance) {
//                    minSellDistance = distance;
//                    selectSellPoint = boatSellPoint;
//                }
//            }
//            if (selectBoat == null || selectSellPoint == null) {
//                continue;
//            }
//            int toSellDistance = 0;
//            if (selectBoat.carry) {
//                toSellDistance = boatSellPoints.get(selectBoat.targetSellId).getMinDistance(selectBoat.corePoint, selectBoat.direction);
//            }
//            if (frameId + toSellDistance + minBuyDistance + minSellDistance >= GAME_FRAME) {
//                continue;//不做决策
//            }
//            //价值是船上的个数加泊位个数
//            int needCount = selectBoat.carry ? selectBoat.capacity : selectBoat.capacity - selectBoat.num;
//            double profit;
//            if (berthGoodNum < needCount) {
//                //自己当作中介，再找一个berth
//                profit = 1.0 * berthGoodNum / (minBuyDistance + minSellDistance);
//                //这个boat已经在这个berth上了，不能作为中介
//                if (berth.curBoatId != selectBoat.id) {
//                    for (Berth berth2 : berths) {
//                        if (berth2.id == berth.id) {
//                            continue;
//                        }
//                        int berthToBethDistance = boatMinBerthToBethDistance(berth, berth2);
//                        int sellDistance = berth2.minSellDistance;
//                        int value = min(needCount, berthGoodNum + goodsNumList[berth2.id]);
//                        double curProfit = 1.0 * value / (minBuyDistance + berthToBethDistance + sellDistance);
//                        if (frameId + minBuyDistance + berthToBethDistance + sellDistance >= GAME_FRAME) {
//                            continue;
//                        }
//                        if (curProfit > profit) {
//                            profit = curProfit;
//                        }
//                    }
//                }
//            } else {
//                profit = 1.0 * needCount / (minBuyDistance + minSellDistance);
//            }
//
//            if (!selectBoat.carry && selectBoat.targetBerthId == berth.id && berth.curBoatId != selectBoat.id) {
//                //防止出问题
//                profit *= (1 + BOAT_DYNAMIC_SAME_TARGET_FACTOR);
//            }
//            stat.add(new Stat(selectBoat, berth, selectSellPoint, profit));
//        }
//        if (stat.isEmpty()) return false;
//        Collections.sort(stat);
//        Boat boat = stat.get(0).boat;
//        Berth berth = stat.get(0).berth;
//        BoatSellPoint sellPoint = stat.get(0).sellPoint;
//        decisionBoatBuy(boat, berth, sellPoint, goodsNumList);
//        return true;
//    }
//
//
//    private boolean boatGreedyBuy3(int[] goodsNumList) {
//        class Stat implements Comparable<Stat> {
//            final Boat boat;
//            final Berth berth;
//
//            final BoatSellPoint sellPoint;
//
//            final double profit;
//
//            public Stat(Boat boat, Berth berth, BoatSellPoint sellPoint, double profit) {
//                this.boat = boat;
//                this.berth = berth;
//                this.sellPoint = sellPoint;
//                this.profit = profit;
//            }
//
//            @Override
//            public int compareTo(Stat b) {
//                return Double.compare(b.profit, profit);
//            }
//        }
//        for (Berth berth : berths) {
//            //停靠在泊位上了
//            if (berth.curBoatId != -1 && berth.goodsNums > 0 && !boats.get(berth.curBoatId).buyAssign && !boats.get(berth.curBoatId).carry) {
//                //没去卖
//                Boat boat = boats.get(berth.curBoatId);
//                decisionBoatBuy(boat, berth, boatSellPoints.get(boat.targetSellId), goodsNumList);
//                return true;
//            }
//        }
//        ArrayList<Stat> stat = new ArrayList<>();
//
//        for (int i = 0; i < berths.size(); i++) {
//            Berth berth = berths.get(i);
//            int berthGoodNum = goodsNumList[i];
//            //找最近的船
//            Boat selectBoat = null;
//            int minBuyDistance = Integer.MAX_VALUE;
//            for (Boat boat : boats) {
//                if (boat.buyAssign) {
//                    continue;
//                }
//                if (!berth.boatCanReach(boat.corePoint, boat.direction)) {
//                    continue;
//                }
//                int distance = boatMinToBerthDistance(boat, berth);
//                if (boat.carry) {
//                    distance -= boatSellPoints.get(boat.targetSellId).getMinDistance(boat.corePoint, boat.direction);
//                }
//                if (distance < minBuyDistance) {
//                    minBuyDistance = distance;
//                    selectBoat = boat;
//                }
//            }
//
//            int minSellDistance = Integer.MAX_VALUE;
//            BoatSellPoint selectSellPoint = null;
//            for (BoatSellPoint boatSellPoint : boatSellPoints) {
//                int distance = boatSellPoint.getMinDistance(berth.corePoint, berth.coreDirection);
//                if (distance < minSellDistance) {
//                    minSellDistance = distance;
//                    selectSellPoint = boatSellPoint;
//                }
//            }
//            if (selectBoat == null || selectSellPoint == null) {
//                continue;
//            }
//            int toSellDistance = 0;
//            if (selectBoat.carry) {
//                toSellDistance = boatSellPoints.get(selectBoat.targetSellId).getMinDistance(selectBoat.corePoint, selectBoat.direction);
//            }
//            if (frameId + toSellDistance + minBuyDistance + minSellDistance >= GAME_FRAME) {
//                continue;//不做决策
//            }
//            //价值是船上的个数加泊位个数
//            int needCount = selectBoat.carry ? selectBoat.capacity : selectBoat.capacity - selectBoat.num;
//            double profit;
//            if (berthGoodNum < needCount) {
//                //自己当作中介，再找一个berth
//                profit = berthGoodNum + 1.0 / (minBuyDistance + minSellDistance);
//            } else {
//                profit = needCount + 1.0 / (minBuyDistance + minSellDistance);
//            }
//
//            if (!selectBoat.carry && selectBoat.targetBerthId == berth.id && berth.curBoatId != selectBoat.id) {
//                //防止出问题
//                profit *= (1 + BOAT_DYNAMIC_SAME_TARGET_FACTOR);
//            }
//            stat.add(new Stat(selectBoat, berth, selectSellPoint, profit));
//        }
//        if (stat.isEmpty()) return false;
//        Collections.sort(stat);
//        Boat boat = stat.get(0).boat;
//        Berth berth = stat.get(0).berth;
//        BoatSellPoint sellPoint = stat.get(0).sellPoint;
//        decisionBoatBuy(boat, berth, sellPoint, goodsNumList);
//        return true;
//    }
//
//    private boolean boatGreedyBuy4(int[] goodsNumList) {
//        class Stat implements Comparable<Stat> {
//            final Boat boat;
//            final Berth berth;
//
//            final BoatSellPoint sellPoint;
//
//            final double profit;
//
//            public Stat(Boat boat, Berth berth, BoatSellPoint sellPoint, double profit) {
//                this.boat = boat;
//                this.berth = berth;
//                this.sellPoint = sellPoint;
//                this.profit = profit;
//            }
//
//            @Override
//            public int compareTo(Stat b) {
//                return Double.compare(b.profit, profit);
//            }
//        }
//        for (Berth berth : berths) {
//            //停靠在泊位上了
//            if (berth.curBoatId != -1 && berth.goodsNums > 0 && !boats.get(berth.curBoatId).buyAssign && !boats.get(berth.curBoatId).carry) {
//                //没去卖
//                Boat boat = boats.get(berth.curBoatId);
//                decisionBoatBuy(boat, berth, boatSellPoints.get(boat.targetSellId), goodsNumList);
//                return true;
//            }
//        }
//        ArrayList<Stat> stat = new ArrayList<>();
//
//        for (int i = 0; i < berths.size(); i++) {
//            Berth berth = berths.get(i);
//            int berthGoodNum = goodsNumList[i];
//            //找最近的船
//            Boat selectBoat = null;
//            int minBuyDistance = Integer.MAX_VALUE;
//            for (Boat boat : boats) {
//                if (boat.buyAssign) {
//                    continue;
//                }
//                if (!berth.boatCanReach(boat.corePoint, boat.direction)) {
//                    continue;
//                }
//                int distance = boatMinToBerthDistance(boat, berth);
//                if (boat.carry) {
//                    distance -= boatSellPoints.get(boat.targetSellId).getMinDistance(boat.corePoint, boat.direction);
//                }
//                if (distance < minBuyDistance) {
//                    minBuyDistance = distance;
//                    selectBoat = boat;
//                }
//            }
//
//            int minSellDistance = Integer.MAX_VALUE;
//            BoatSellPoint selectSellPoint = null;
//            for (BoatSellPoint boatSellPoint : boatSellPoints) {
//                int distance = boatSellPoint.getMinDistance(berth.corePoint, berth.coreDirection);
//                if (distance < minSellDistance) {
//                    minSellDistance = distance;
//                    selectSellPoint = boatSellPoint;
//                }
//            }
//            if (selectBoat == null || selectSellPoint == null) {
//                continue;
//            }
//            int toSellDistance = 0;
//            if (selectBoat.carry) {
//                toSellDistance = boatSellPoints.get(selectBoat.targetSellId).getMinDistance(selectBoat.corePoint, selectBoat.direction);
//            }
//            if (frameId + toSellDistance + minBuyDistance + minSellDistance >= GAME_FRAME) {
//                continue;//不做决策
//            }
//            //价值是船上的个数加泊位个数
//            int needCount = selectBoat.carry ? selectBoat.capacity : selectBoat.capacity - selectBoat.num;
//            double profit;
//            if (berthGoodNum < needCount) {
//                //自己当作中介，再找一个berth
//                profit = berthGoodNum + 1.0 / (minBuyDistance + minSellDistance);
//                //这个boat已经在这个berth上了，不能作为中介
//                if (berth.curBoatId != selectBoat.id) {
//                    for (Berth berth2 : berths) {
//                        if (berth2.id == berth.id) {
//                            continue;
//                        }
//                        int berthToBethDistance = boatMinBerthToBethDistance(berth, berth2);
//                        int sellDistance = berth2.minSellDistance;
//                        int value = min(needCount, berthGoodNum + goodsNumList[berth2.id]);
//                        double curProfit = value + 1.0 / (minBuyDistance + berthToBethDistance + sellDistance);
//                        if (frameId + minBuyDistance + berthToBethDistance + sellDistance >= GAME_FRAME) {
//                            continue;
//                        }
//                        if (curProfit > profit) {
//                            profit = curProfit;
//                        }
//                    }
//                }
//            } else {
//                profit = needCount + 1.0 / (minBuyDistance + minSellDistance);
//            }
//
//            if (!selectBoat.carry && selectBoat.targetBerthId == berth.id && berth.curBoatId != selectBoat.id) {
//                //防止出问题
//                profit *= (1 + BOAT_DYNAMIC_SAME_TARGET_FACTOR);
//            }
//            stat.add(new Stat(selectBoat, berth, selectSellPoint, profit));
//        }
//        if (stat.isEmpty()) return false;
//        Collections.sort(stat);
//        Boat boat = stat.get(0).boat;
//        Berth berth = stat.get(0).berth;
//        BoatSellPoint sellPoint = stat.get(0).sellPoint;
//        decisionBoatBuy(boat, berth, sellPoint, goodsNumList);
//        return true;
//    }
//
//
//    private int boatGetMinSellPointIndex(Berth berth) {
//        int minDistance = Integer.MAX_VALUE;
//        int bestIndex = -1;
//        for (int i = 0; i < boatSellPoints.size(); i++) {
//            BoatSellPoint boatSellPoint = boatSellPoints.get(i);
//            int distance = boatSellPoint.getMinDistance(berth.corePoint, berth.coreDirection);
//            if (distance < minDistance) {
//                minDistance = distance;
//                bestIndex = i;
//            }
//        }
//        return bestIndex;
//    }
//
    private void decisionBoatBuy(Boat boat, Berth berth, BoatSellPoint boatSellPoint, int[] goodsNumList) {
        boat.targetBerthId = berth.id;
        if (!boat.carry) {
            boat.targetSellId = boatSellPoint.id;
        }
        boat.assigned = true;
        boat.buyAssign = true;
        int needCount = boat.capacity - boat.num;
        goodsNumList[berth.id] -= needCount;
    }


    private int boatMinToBerthDistance(Boat boat, Berth berth) {
        int recoveryTime = boat.remainRecoveryTime;//核心，这个很重要
        if (boat.carry) {
            //卖
            assert boat.targetSellId != -1;
            int toSellDistance = boatSellPoints.get(boat.targetSellId).getMinDistance(boat.corePoint, boat.direction);
            int toBerthDistance = berth.getBoatMinDistance(boatSellPoints.get(boat.targetSellId).point, 0);
            //直接假设正方向得了
            return recoveryTime + toSellDistance + toBerthDistance;
        }
        return recoveryTime + berth.getBoatMinDistance(boat.corePoint, boat.direction);
    }

    //
////    private boolean boatGreedyBuy(int[] goodsNumList, int[] goodComingTimes) {
////        //三种决策，在去目标中的，
////        //在回家或者在家的
////        //在泊位的或者在泊位等待的
////        class Stat implements Comparable<Stat> {
////            final Boat boat;
////            final Berth berth;
////            final int count;//消费个数
////            final int updateTime;//消费个数
////            final double profit;
////
////            public Stat(Boat boat, Berth berth, int count, int updateTime, double profit) {
////                this.boat = boat;
////                this.berth = berth;
////                this.count = count;
////                this.profit = profit;
////                this.updateTime = updateTime;
////            }
////
////            @Override
////            public int compareTo(Stat b) {
////                return Double.compare(b.profit, profit);
////            }
////        }
////        double goodsComingTime = avgPullGoodsTime * BERTH_PER_PLAYER;
////        ArrayList<Boat> oneTypesBoats = new ArrayList<>();
////        ArrayList<Boat> twoTypesBoats = new ArrayList<>();
////        ArrayList<Boat> threeTypesBoats = new ArrayList<>();
////        for (Boat boat : boats) {
////            if (boat.assigned) {
////                continue;
////            }
////            if (boat.targetId != -1 && boat.status == 0) {
////                oneTypesBoats.add(boat);
////            } else if (boat.targetId == -1) {
////                twoTypesBoats.add(boat);//回家或者在家的
////            } else {
////                threeTypesBoats.add(boat);
////            }
////        }
////        //第一种，在移动向泊位的，不切换目标，直接消耗
////        for (Boat boat : oneTypesBoats) {
////            int needCount = boat.capacity - boat.num;
////            if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
////                return true;
////        }
////
////        //第二种,选择泊位货物最多的，一样多则选updateTime最早的，一样早选择最远的
////        //考虑在移动，运输中也算不能返回，移动完才可以动？
////        ArrayList<Stat> stat = new ArrayList<>();
////        //选择折现价值最大的
////        for (Berth buyBerth : berths) {
////            Boat selectBoat = null;//最近的先决策
////            int minDistance = Integer.MAX_VALUE;
////            for (Boat boat : twoTypesBoats) {
////                int distance = getBoatToBerthDistance(buyBerth, boat);
////                if (distance < minDistance) {
////                    minDistance = distance;
////                    selectBoat = boat;
////                }
////            }
////            if (selectBoat == null) {
////                continue;
////            }
////
////            if (frameId + minDistance + buyBerth.transportTime >= GAME_FRAME) {
////                continue;//货物装不满，则不管
////            }
////
//////            int buyTime = minDistance;
//////            int sellTime = buyBerth.transportTime;
//////            int needCount = selectBoat.capacity;
//////            int remainTime = GAME_FRAME - buyTime - sellTime - frameId;
//////            int realCount = min(needCount, remainTime / buyBerth.loadingSpeed);//装货
//////            realCount = min(realCount, goodsNumList[buyBerth.id] + (int) (remainTime / goodsComingTime));//来货
//////            int loadTime = (int) ceil(1.0 * realCount / buyBerth.loadingSpeed);
//////            int totalWaitTime = goodComingTimes[buyBerth.id] + (int) ceil(max(0, realCount - goodsNumList[buyBerth.id]) * goodsComingTime);
//////            int arriveWaitTime = max(0, totalWaitTime - buyTime);//到达之后的等待时间
//////            double profit = 1.0 * realCount / (buyTime + max(arriveWaitTime, loadTime) + sellTime);
////
////            //只能装那么多，尽量去装吧,可以切泊位可能上面决策好一点，不可以切泊位，还是看数量高分
////            int realCount = selectBoat.capacity;
////            double profit = 1e10 + goodsNumList[buyBerth.id] * 1e10 - goodComingTimes[buyBerth.id] * 1e5 + minDistance;
////            int totalWaitTime = goodComingTimes[buyBerth.id] +
////                    (int) ceil(max(0, selectBoat.capacity - goodsNumList[buyBerth.id])
////                            * goodsComingTime);
////            stat.add(new Stat(selectBoat, buyBerth, min(goodsNumList[buyBerth.id], realCount)
////                    , totalWaitTime, profit));
////        }
////        if (!stat.isEmpty()) {
////            Collections.sort(stat);
////            //同一个目标不用管
//////            boatRealDecision(goodsNumList, goodComingTimes, stat.get(0).berth
//////                    , stat.get(0).boat, stat.get(0).updateTime, stat.get(0).count);
////            return true;
////        }
////
////        //第三种，在泊位上或者等待的,货物数量和,单位时间价值
////        for (Boat boat : threeTypesBoats) {
////            //到每个泊位距离一定是500
////            int needCount = boat.capacity - boat.num;
////            if (boatDecisionType == DECISION_ON_ORIGIN) {
////                //不可以切换泊位,
////                assert boat.estimateComingBerthId == boat.targetId;
////                if (!BOAT_NO_WAIT || frameId + berths.get(boat.targetId).transportTime * 3 > GAME_FRAME || berths.get(boat.targetId).goodsNums > 0) {
////                    if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
////                        return true;
////                } else {
////                    //回家
////                    boat.remainTime = berths.get(boat.targetId).transportTime;
////                    boat.status = 0;//移动中
////                    boat.targetId = -1;
////                    boat.estimateComingBerthId = -1;
////                    boat.exactStatus = IN_ORIGIN_TO_BERTH;
////                    return true;
////                }
////            }
////
////            if (BOAT_NO_WAIT && frameId + berths.get(boat.targetId).transportTime * 3 < GAME_FRAME) {
////                if (berths.get(boat.targetId).goodsNums > 0) {
////                    if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
////                        return true;
////                }
////                //没货物了，选择切泊位买了走人，或者回家
////                double maxProfit = 1.0 * boat.num / berths.get(boat.targetId).transportTime / 2;
////                Berth changeBerth = null;
////                int maxWaitTime = -1;
////                int maxCount = 0;
////
////                //来回一趟基本价值，如果有人超过了，则切泊位，否则回家
////                for (Berth berth : berths) {
////                    if (boat.targetId == berth.id) {
////                        continue;
////                    }
////                    int buyTime = BERTH_CHANGE_TIME;
////                    int sellTime = berth.transportTime;
////                    if (frameId + buyTime + sellTime >= GAME_FRAME) {
////                        continue;//不干
////                    }
////                    int remainTime = GAME_FRAME - buyTime - sellTime - frameId;
////                    int realCount = min(needCount, remainTime / berth.loadingSpeed);//装货
////                    realCount = min(realCount, goodsNumList[berth.id] + (int) ((buyTime - goodComingTimes[berth.id]) / goodsComingTime));//来货
////                    int loadTime = (int) ceil(1.0 * realCount / berth.loadingSpeed);
////                    int totalWaitTime = goodComingTimes[berth.id] + (int) ceil(max(0, realCount - goodsNumList[berth.id]) * goodsComingTime);
////                    double profit = 1.0 * (realCount + boat.num) / (berths.get(boat.targetId).transportTime + 1.0 * boat.num
////                            / berths.get(boat.targetId).loadingSpeed + buyTime + loadTime + sellTime);
////                    if (profit > maxProfit) {
////                        changeBerth = berth;
////                        maxProfit = profit;
////                        maxWaitTime = totalWaitTime;
////                        maxCount = min(realCount, goodsNumList[berth.id]);
////                    }
////
////                }
////                if (changeBerth != null) {
////                    stat.add(new Stat(boat, changeBerth, min(maxCount, goodsNumList[changeBerth.id]), maxWaitTime, maxProfit));
////                } else {
////                    //回家
////                    if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
////                        return true;
////                }
////            } else {
////                //目前船所在的泊位的来货速度,如果来货速度实在太慢，不需要等，直接切目标
////                int curBerthGoodsComingTime = frameId / max(1, berths.get(boat.targetId).totalGoodsNums);
////                for (Berth berth : berths) {
////                    int buyTime = boat.targetId == berth.id ? 0 : BERTH_CHANGE_TIME;
////                    int sellTime = berth.transportTime;
////                    if (frameId + buyTime + sellTime >= GAME_FRAME) {
////                        continue;//不干
////                    }
////                    if (berth.id != boat.targetId && berths.get(boat.targetId).goodsNums > 0) {
////                        continue;//有货物了，船只能选择这个泊位不变
////                    }
////
////                    int remainTime = GAME_FRAME - buyTime - sellTime - frameId;
////                    int realCount = min(needCount, remainTime / berth.loadingSpeed);//装货
////                    realCount = min(realCount, goodsNumList[berth.id] + (int) ((remainTime + buyTime - goodComingTimes[berth.id]) / goodsComingTime));//来货
////                    int loadTime = (int) ceil(1.0 * realCount / berth.loadingSpeed);
////                    int totalWaitTime = goodComingTimes[berth.id] + (int) ceil(max(0, realCount - goodsNumList[berth.id]) * goodsComingTime);
////                    int arriveWaitTime = max(0, totalWaitTime - buyTime);//到达之后的等待时间
////                    //todo 可调参
////                    if (berth.id != boat.targetId && arriveWaitTime > 0//留一半阈值,&&remainTime > 2 * berth.loadingSpeed * realCount
////                            && curBerthGoodsComingTime * 2 > goodsComingTime//来货速度的两倍小于平均来货速度，直接切目标
////                            && remainTime > 2 * berth.transportTime - buyTime
////                    ) {
////                        continue;//不是同一个泊位，且到达之后需要等待，那么先不去先,有问题，最后还是不会切泊位的
////                    }
////                    double profit = realCount + 1.0 * realCount / (buyTime + max(arriveWaitTime, loadTime) + sellTime);
////                    //增益超过保持因子，则切换目标
////                    stat.add(new Stat(boat, berth, min(realCount, goodsNumList[berth.id]), totalWaitTime, profit));
////                }
////            }
////        }
////        if (!stat.isEmpty()) {
////            Collections.sort(stat);
////            //同一个目标不用管
//////            boatRealDecision(goodsNumList, goodComingTimes, stat.get(0).berth
//////                    , stat.get(0).boat, stat.get(0).updateTime, stat.get(0).count);
////            return true;
////        }
////        return false;
////
////    }
//
////    private static void boatRealDecision(int[] goodsNumList, int[] goodComingTimes, Berth berth, Boat boat, int updateTime, int count) {
////        goodsNumList[berth.id] -= count;
////        goodComingTimes[berth.id] = updateTime;
////        //boat去移动
////        if (boat.targetId != berth.id && (!(boat.status == 0 && boat.targetId == -1))) {
////            boat.ship(berth.id);//不在往原点运输中
////            if (boat.lastArriveTargetId == -1) {
////                //在虚拟点，或者虚拟点到泊位
////                boat.remainTime = berth.transportTime;
////            } else {
////                boat.remainTime = BERTH_CHANGE_TIME;
////            }
////            //printERROR("boat:" + boat.id + ",原来目标:" + boat.targetId + ",现在目标:" + berth.id + ",time:" + boat.remainTime);
////            boat.targetId = berth.id;
////            boat.status = 0;
////        }
////        boat.assigned = true;
////        //更新泊位的货物列表和前缀和,说明这个船消耗了这么多货物
////        boat.estimateComingBerthId = berth.id;
////    }
//
//
////    private boolean updateSumTimes(int[] goodsNumList, int[] goodComingTimes, Boat boat, double goodsComingSpeed, int needCount) {
////        int comingBerthId = boat.estimateComingBerthId;
////        if (comingBerthId != -1) {
////            int comingCount = max(0, needCount - goodsNumList[comingBerthId]);
////            int addComingTime = 0;
////            if (comingCount > 0) {
////                addComingTime = (int) ceil(comingCount * goodsComingSpeed);
////            }
////            goodsNumList[comingBerthId] -= min(goodsNumList[comingBerthId], needCount);
////            goodComingTimes[comingBerthId] += addComingTime;
////            boat.assigned = true;
////            return true;
////        }
////        return false;
////    }
//
//
//    @SuppressWarnings("all")
////    private int getCanReachBerthsCount() {
////        int count = 0;
////        for (Berth berth : berths) {
////            for (Boat boat : boats) {
////                int distance = getBoatToBerthDistance(berth, boat);
////                if (frameId + distance + berth.transportTime < GAME_FRAME) {
////                    count++;
////                    break;
////                }
////            }
////        }
////        return count;
////    }
//
////    private static int getBoatToBerthDistance(Berth buyBerth, Boat boat) {
////        int dist;
////        if (boat.targetId == buyBerth.id) {
////            dist = boat.remainTime;
////            if (boat.exactStatus == IN_BERTH_WAIT) {
////                dist = 1;//泊位外等待为1
////            }
////        } else if (boat.lastArriveTargetId != -1 && boat.targetId != -1) {
////            //泊位内，泊位等待，泊位切换中
////            dist = BERTH_CHANGE_TIME;
////        } else if (boat.targetId == -1 && boat.status == 0) {//往原点运输中
////            dist = boat.remainTime + buyBerth.transportTime;//虚拟点到泊位时间
////        } else {
////            dist = buyBerth.transportTime;//虚拟点到泊位时间
////        }
////        return dist;
////    }
//
    private void robotDoAction() {


        for (HashSet<Integer> set : robotLock) {
            set.clear();
        }
        while (true) {
            if (!greedySell()) {
                break;
            }  //决策
        }
        long l = System.currentTimeMillis();
        while (true) {
            if (!greedyBuy()) {
                break;
            }
        }
        long r = System.currentTimeMillis();
        printDebug("frame:" + frameId + ",robotDecision:" + (r - l));
        l = System.currentTimeMillis();
        //选择路径，碰撞避免
        Robot[] tmpRobots = new Robot[ROBOTS_PER_PLAYER];
        for (int i = 0; i < tmpRobots.length; i++) {
            tmpRobots[i] = robots.get(i);
        }
        sortRobots(tmpRobots);
        //1.选择路径,和修复路径
        ArrayList<ArrayList<Point>> otherPaths = new ArrayList<>();
        for (int i = 0; i < tmpRobots.length; i++) {
            Robot robot = tmpRobots[i];
            if (!robot.assigned || robot.isPending) {
                continue;
            }
            ArrayList<Point> path;

            if (robot.carry) {
                //启发式寻路,如果保存的话太多了
                path = robotToBerthHeuristic(robot, robot.targetBerthId, null, null);
            } else {
                path = workbenches.get(robot.targetWorkBenchId).moveFrom(robot.pos);
            }
            //检查是否与前面机器人相撞，如果是，则重新搜一条到目标点的路径，极端情况，去到物品消失不考虑
            if (robotCheckCrash(gameMap, path, otherPaths)) {
                if (robot.carry) {
                    //to berth
                    ArrayList<Point> avoidPath = robotToBerthHeuristic(robot, robot.targetBerthId, otherPaths, null);
                    if (!avoidPath.isEmpty()) {
                        path = avoidPath;
                    }
                } else {
                    ArrayList<Point> avoidPath = robotToWorkBenchHeuristic(robot, robot.targetWorkBenchId, otherPaths, null);
                    if (!avoidPath.isEmpty()) {
                        path = avoidPath;
                    }
                    if (avoidPath.isEmpty() && !robot.redundancy) {
                        robot.assigned = false;
                        robot.buyAssign = false;
                        robotLock[robot.id].add(robot.targetWorkBenchId);
                        greedyBuy();
                        i--;
                        //重开
                        continue;
                    }
                }
            }
            if (robot.avoidOtherTime > 0) {
                ArrayList<Point> avoidOtherPoints = robotsAvoidOtherPoints[robot.id];
                if (avoidOtherPoints.isEmpty()) {
                    printError("error in avoid OtherTime");
                }
                for (Point avoidPoint : avoidOtherPoints) {
                    gameMap.commonConflictPoints[avoidPoint.x][avoidPoint.y] = true;
                }
                ArrayList<Point> avoidPath;
                if (robot.carry) {
                    //to berth
                    avoidPath = robotToBerthHeuristic(robot, robot.targetBerthId, otherPaths, gameMap.commonConflictPoints);
                } else {
                    avoidPath = robotToWorkBenchHeuristic(robot, robot.targetWorkBenchId, otherPaths, gameMap.commonConflictPoints);
                }
                if (!avoidPath.isEmpty()) {
                    path = avoidPath;
                }
                for (Point avoidPoint : avoidOtherPoints) {
                    gameMap.commonConflictPoints[avoidPoint.x][avoidPoint.y] = false;
                }
            }
            //撞到其他人避让，会锁死某个位置再寻路
            robot.path.clear();
            robot.path.addAll(path);
            otherPaths.add(path);
        }

        //2.碰撞避免
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            //固定只预测未来一个格子，就是两个小格子，
            //预测他未来两个格子就行，四下，如果冲突，则他未来一个格子自己不能走，未来第二个格子自己尽量也不走
            Robot robot = tmpRobots[i];
            robotsPredictPath[robot.id] = new ArrayList<>();
            if (!robot.assigned || robot.isPending) {
                //未来一格子在这里不动，如果别人撞过来，则自己避让
                robot.path.add(gameMap.posToDiscrete(robot.pos));
                for (int j = 1; j <= 2; j++) {
                    robotsPredictPath[robot.id].add(gameMap.posToDiscrete(robot.pos));
                    robot.path.add(gameMap.posToDiscrete(robot.pos));
                }
            } else {
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
                    if (point.equal(robotsPredictPath[tmpRobots[j].id].get(k)) && !gameMap.isRobotDiscreteMainChannel(point.x, point.y)) {
                        crashId = tmpRobots[j].id;
                        break;
                    }
                }
                if (crashId != -1) {
                    break;
                }
            }

            if (crashId != -1) {
                //看看自己是否可以避让，不可以的话就说明被夹住了,让冲突点去让,并且自己强制提高优先级50帧
                robots.get(crashId).beConflicted = FPS;
                ArrayList<Point> candidates = new ArrayList<>();
                candidates.add(robot.pos);
                for (int j = 0; j < DIR.length / 2; j++) {
                    candidates.add(robot.pos.add(DIR[j]));
                }
                Point result = new Point(-1, -1);
                int bestDist = Integer.MAX_VALUE;

                for (Point candidate : candidates) {
                    if (!gameMap.robotCanReach(candidate.x, candidate.y)) {
                        continue;
                    }
                    boolean crash = false;
                    for (Robot tmpRobot : tmpRobots) {
                        if (tmpRobot.id == robot.id || robotsPredictPath[tmpRobot.id] == null) {
                            continue;//后面的
                        }
                        Point start = gameMap.posToDiscrete(robot.pos);
                        Point end = gameMap.posToDiscrete(candidate);
                        Point mid = start.add(end).div(2);
                        if ((mid.equal(robotsPredictPath[tmpRobot.id].get(0)) && !gameMap.isRobotDiscreteMainChannel(mid.x, mid.y)) || (end.equal(robotsPredictPath[tmpRobot.id].get(1)) && !gameMap.isRobotDiscreteMainChannel(end.x, end.y))) {
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
                        if (robot.targetWorkBenchId == -1) {
                            assert !robot.assigned;
                            dist = MAP_FILE_ROW_NUMS * MAP_FILE_COL_NUMS;
                        } else {
                            dist = workbenches.get(robot.targetWorkBenchId).getMinDistance(candidate);
                        }
                    } else {
                        dist = berths.get(robot.targetBerthId).getRobotMinDistance(candidate);
                    }
                    assert dist != Integer.MAX_VALUE;
                    Point tmp = gameMap.posToDiscrete(candidate);
                    for (Point point : robotsPredictPath[crashId]) {
                        if (tmp.equal(point) && !gameMap.isRobotDiscreteMainChannel(candidate.x, candidate.y)) {
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
                    robotsPredictPath[robot.id].clear();
                    robots.get(robot.id).avoid = true;
                    Point start = gameMap.posToDiscrete(robot.pos);
                    Point end = gameMap.posToDiscrete(result);
                    Point mid = start.add(end).div(2);
                    robotsPredictPath[robot.id].add(mid);//中间
                    robotsPredictPath[robot.id].add(end);//下一个格子
                } else {
                    robot.beConflicted = FPS * 2;
                    robot.forcePri += 1;
                    sortRobots(tmpRobots);
                    i = -1;
                }
            }
        }

        for (Robot robot : robots) {
            if (robot.avoid) {
                Point start = gameMap.posToDiscrete(robot.pos);
                robot.path.clear();
                robot.path.add(start);
                robot.path.add(robotsPredictPath[robot.id].get(0));
                robot.path.add(robotsPredictPath[robot.id].get(1));
                //在避让，所以路径改变了，稍微改一下好看一点
            }
        }

        //todo 机器人避让其他人
        for (Robot robot : robots) {
            boolean avoidOther = false;
            Point point = gameMap.discreteToPos(robot.path.get(2));
            if (!point.equal(robot.pos)) {
                for (SimpleRobot other : totalRobots) {
                    if (other.belongToMe) {
                        continue;
                    }
                    if (other.noMoveTime > FPS / 4 && other.p.equal(point)) {
                        avoidOther = true;
                        break;
                    }
                }
            }
            if (robot.noMoveTime >= ROBOT_AVOID_OTHER_TRIGGER_THRESHOLD) {
                avoidOther = true;
            }
            if (!avoidOther) {
                //对面超过10帧没动，或者自己没动超过2帧，则避让
                continue;
            }
            //大于判断是否其他人也不动
            //我的下一个点四周有至少一个机器人不动

            if (point.equal(robot.pos)) {
                if (!robot.avoid) {
                    printError("error in avoid other");
                }
                continue;
            }
            ArrayList<Point> avoidPoints = new ArrayList<>();
            avoidPoints.add(point);//前一个点是避让点
            for (int j = 0; j < DIR.length / 2; j++) {
                Point e = robot.pos.add(DIR[j]);
                if (e.equal(point) ||
                        !gameMap.robotCanReach(e.x, e.y)
                        || gameMap.isRobotMainChannel(e.x, e.y)) {
                    continue;
                }
                //检查是否又机器人超过5帧不动
                for (SimpleRobot simpleRobot : totalRobots) {
                    if (simpleRobot.belongToMe) {
                        continue;
                    }
                    if (simpleRobot.noMoveTime < FPS / 4) {
                        continue;
                    }
                    if (simpleRobot.p.equal(e)) {
                        avoidPoints.add(e);
                        break;
                    }
                }
            }
            //得到需要避让的点,重新寻路

            if (robot.path.size() == 3) {
                //下一个点就是目标点,等着吧
                robot.path.clear();
                robot.path.add(gameMap.posToDiscrete(robot.pos));
                robot.path.add(gameMap.posToDiscrete(robot.pos));
                robot.path.add(gameMap.posToDiscrete(robot.pos));
                continue;
            }
            //加入避让路径进去
            for (Point avoidPoint : avoidPoints) {
                boolean contain = false;
                for (Point point2 : robotsAvoidOtherPoints[robot.id]) {
                    if (avoidPoint.equal(point2)) {
                        contain = true;
                        break;
                    }
                }
                if (!contain) {
                    //加入进去
                    robotsAvoidOtherPoints[robot.id].add(avoidPoint);
                }
            }
            for (Point point2 : robotsAvoidOtherPoints[robot.id]) {
                gameMap.commonConflictPoints[point2.x][point2.y] = true;
            }
            //保持目标不变，重新寻路,启发式搜
            ArrayList<Point> avoidPath = new ArrayList<>();
            if (robot.assigned) {
                if (robot.carry) {
                    avoidPath = robotToBerthHeuristic(robot, robot.targetBerthId, null, gameMap.commonConflictPoints);
                } else {
                    avoidPath = robotToWorkBenchHeuristic(robot, robot.targetWorkBenchId, null, gameMap.commonConflictPoints);
                }
                if (!avoidPath.isEmpty()) {
                    robot.path = avoidPath;
                }
            }
            if (avoidPath.isEmpty()) {
                //随机找一个位置去避让，没办法了
                printError("frame:" + frameId + "robotId:" + robot.id + ",can not avoid suiji");
                int index = random.nextInt(4);
                Point next = robot.pos.add(DIR[index]);
                while (!gameMap.robotCanReach(next.x, next.y)) {
                    index = random.nextInt(4);
                    next = robot.pos.add(DIR[index]);
                }
                Point mid = next.add(robot.pos).div(2);
                robot.path.clear();
                robot.path.add(gameMap.posToDiscrete(robot.pos));
                robot.path.add(gameMap.posToDiscrete(mid));
                robot.path.add(gameMap.posToDiscrete(next));
            }
            //回退
            for (Point point2 : robotsAvoidOtherPoints[robot.id]) {
                gameMap.commonConflictPoints[point2.x][point2.y] = false;
            }
            //避让
            robot.avoidOtherTime = ROBOT_AVOID_OTHER_DURATION;

        }


        for (Robot robot : robots) {
            robot.finish();
            if (robot.avoidOtherTime-- < 0) {
                //清空锁死点
                robotsAvoidOtherPoints[robot.id].clear();
            }
            if (robot.beConflicted-- < 0 && robot.forcePri != 0) {
                robot.forcePri = 0;
            }
        }
        r = System.currentTimeMillis();
        printDebug("frame:" + frameId + ",robotOther:" + (r - l));
    }

    private ArrayList<Point> robotToWorkBenchHeuristic(Robot robot, int targetWorkBenchId, ArrayList<ArrayList<Point>> otherPaths, boolean[][] conflictPoints) {
        short[][] robotCommonHeuristicCs;
        //复用逻辑判断，保证一个workbench几乎只走一次
        if (gameMap.robotUseHeuristicCsWbIds.containsKey(targetWorkBenchId)) {
            int index = gameMap.robotUseHeuristicCsWbIds.get(targetWorkBenchId);
            robotCommonHeuristicCs = gameMap.robotCommonHeuristicCs[index];
        } else {
            Integer pre = gameMap.robotUseHeuristicCsWbIdList.poll();
            int index = gameMap.robotUseHeuristicCsWbIds.get(pre);
            gameMap.robotUseHeuristicCsWbIds.remove(pre);
            gameMap.robotUseHeuristicCsWbIds.put(targetWorkBenchId, index);
            gameMap.robotUseHeuristicCsWbIdList.offer(targetWorkBenchId);
            robotCommonHeuristicCs = gameMap.robotCommonHeuristicCs[index];
            workbenches.get(targetWorkBenchId).setHeuristicCs(robotCommonHeuristicCs);
        }
        short maxDeep = robotCommonHeuristicCs[robot.pos.x][robot.pos.y];
        if (conflictPoints != null) {
            maxDeep += 5;
        }
        return robotMoveToPointBerth(gameMap, robot.pos, -1, workbenches.get(targetWorkBenchId).pos
                , maxDeep, otherPaths, robotCommonHeuristicCs, conflictPoints);
    }

    private void sortRobots(Robot[] robots) {
        Arrays.sort(robots, (o1, o2) -> {
            if (o1.isPending ^ o2.isPending) {//暴力优先级最高
                return o1.isPending ? -1 : 1;//pending在前面，因为此时不动是最好的，等他pending结束再让
            }
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
        for (Robot robot : robots) {
            robot.avoid = false;
        }
    }

    //
//
    int robotMinToWorkbenchDistance(Robot robot, Workbench2 workbench) {
        if (robot.carry) {
            assert (robot.assigned);
            int toBerth = berths.get(robot.targetBerthId).getRobotMinDistance(robot.pos);
            int toWorkBench = berths.get(robot.targetBerthId).getRobotMinDistance(workbench.pos);
            toWorkBench += 2;//最坏需要再走两帧
            return toWorkBench + toBerth;
        }
        return workbench.getMinDistance(robot.pos);
    }

    private boolean greedyBuy() {
        Robot bestRobot = null;
        Workbench2 bestWorkBench = null;
        Berth bestWorkBerth = null;
        double bestProfit = -GAME_FRAME;
        HashSet<Integer> lastTimeBuyId = new HashSet<>();
        for (Robot robot : robots) {
            if (robot.buyAssign) {
                continue;
            }
            if (robot.isPending) {
                //答题状态优先决策
                assert robot.targetWorkBenchId != -1;
                assert robot.targetBerthId != -1;
                bestWorkBench = workbenches.get(robot.targetWorkBenchId);
                bestWorkBerth = berths.get(robot.targetBerthId);
                bestRobot = robot;
                assignRobot(bestWorkBench, bestWorkBerth, bestRobot, RA_BUY);
                return true;
            }
            if (robot.targetWorkBenchId != -1 && !robot.carry) {
                lastTimeBuyId.add(robot.targetWorkBenchId);
            }
        }

        //选择折现价值最大的
        for (Workbench2 buyWorkbench : workbenches.values()) {
            //存在就一定有产品
            if (2.0 * buyWorkbench.value / buyWorkbench.minSellDistance < bestProfit
                    && !lastTimeBuyId.contains(buyWorkbench.id)) {
                continue;
            }

            if (workbenchesPermanentLock.contains(buyWorkbench.id)) {
                continue;//回答错误的workbench；
            }
            if (workbenchesLock.contains(buyWorkbench.id)) {
                continue;//别人选择过了
            }
            //贪心，选择最近的机器人

            //选距离最近的，如果是没到泊位的有好几个，选离泊位最近的
            Robot selectRobot = null;
            int minDist = Integer.MAX_VALUE;

            for (Robot robot : robots) {
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
                    toBerthTime = berths.get(robot.targetBerthId).getRobotMinDistance(robot.pos);
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
                toBerthTime = berths.get(selectRobot.targetBerthId).getRobotMinDistance(selectRobot.pos);
            }

            if (toBerthTime + minDist > buyWorkbench.remainTime) {
                continue;//去到货物就消失了,不去
            }
            int arriveBuyTime = minDist;
            if (buyWorkbench.minSellBerthId == -1) {
                continue;
            }
            //只卖最近的
            Berth sellBerth = berths.get(buyWorkbench.minSellBerthId);
            if (!sellBerth.robotCanReach(buyWorkbench.pos)) {
                continue; //不能到达
            }
            int arriveSellTime = sellBerth.getRobotMinDistance(buyWorkbench.pos);//机器人买物品的位置开始
            int collectTime = toBerthTime + arriveBuyTime + arriveSellTime;
            int sellTime = collectTime + sellBerth.minSellDistance;
            double profit;
            if (frameId + sellTime >= GAME_FRAME) {
                profit = -sellTime;//最近的去决策，万一到了之后能卖就ok，买的时候检测一下
            } else {
                double value = buyWorkbench.value;
                if (buyWorkbench.value < PRECIOUS_WORKBENCH_BOUNDARY) {
                    //高价值不会消失，但是需要跟别人抢
                    value += DISAPPEAR_REWARD_FACTOR * value * (WORKBENCH_EXIST_TIME - buyWorkbench.remainTime) / WORKBENCH_EXIST_TIME;
                }
                profit = value / (arriveSellTime + arriveBuyTime * 2);
                //考虑注释掉，可能没啥用，因为所有泊位都可以卖，可能就应该选最近的物品去买
                if (selectRobot.targetWorkBenchId == buyWorkbench.id && !selectRobot.carry) {
                    profit *= (1 + SAME_TARGET_REWARD_FACTOR);
                }
            }
            if (profit > bestProfit) {
                bestRobot = selectRobot;
                bestWorkBench = buyWorkbench;
                bestWorkBerth = sellBerth;
                bestProfit = profit;
            }

        }
        if (bestRobot == null)
            return false;
        //锁住买家
        assignRobot(bestWorkBench, bestWorkBerth, bestRobot, RA_BUY);
        return true;
    }


    private boolean greedySell() {
        Robot bestRobot = null;
        Berth bestBerth = null;
        double bestProfit = -GAME_FRAME;
        for (Robot robot : robots) {
            if (robot.assigned || !robot.carry) {
                continue;
            }

            //选择折现价值最大的
            Berth select = null;
            double maxProfit = -GAME_FRAME;
            for (Berth sellBerth : berths) {
                if (!sellBerth.robotCanReach(robot.pos)) {
                    continue; //不能到达
                }
                //assert fixTime != Integer.MAX_VALUE;
                int arriveTime = sellBerth.getRobotMinDistance(robot.pos);
                double profit;
                //包裹被揽收的最小需要的时间
                int sellTime = arriveTime + sellBerth.minSellDistance;
                if (frameId + sellTime >= GAME_FRAME) {
                    //如果不能到达，收益为负到达时间
                    profit = -sellTime;
                } else {
                    //越近越好
                    profit = -arriveTime;
                }
                if (profit > maxProfit) {
                    maxProfit = profit;
                    select = sellBerth;
                }
            }
            if (select != null && maxProfit > bestProfit) {
                bestRobot = robot;
                bestBerth = select;
                bestProfit = maxProfit;
            }
        }
        if (bestRobot == null) return false;
        assignRobot(null, bestBerth, bestRobot, RA_SELL);
        return true;
    }

    private void assignRobot(Workbench2 workbench, Berth berth, Robot robot, RobotAction action) {
        robot.assigned = true;
        if (!robot.carry || action == RA_SELL) {
            //没携带物品，或者是卖，需要改变目标
            robot.targetBerthId = berth.id;
        }
        if (action == RA_BUY) {
            robot.buyAssign = true;

            workbenchesLock.add(workbench.id);//锁住，别人不准选择
            int robotLastFrameId = robot.targetWorkBenchId;
            robot.targetWorkBenchId = workbench.id;
            if (!robot.carry) {
                //携带了物品不管
                int toWorkbenchDist = robotMinToWorkbenchDistance(robot, workbench);
                robot.redundancy = toWorkbenchDist != workbench.remainTime;
            }
            robot.priority = workbench.value;
            if (!robot.carry && robot.pos.equal(workbench.pos) && !robot.isPending) {
                //没带物品，且到目标，一定买，然后重新决策一个下一个目标买,已经pending没必要再get
                //上一帧移动成功，且发了buy指令，这种时候没有买到，说明一定被别人拿了
                if (workbench.id == robotLastFrameId) {
                    //上一帧的目标还是他，因为一定提前buy，此时如果没进入pending或者买到了，说明一定是别人拿走了
                    workbenches.remove(workbench.id);//重新决策
                    gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] = -1;
                    robot.buyAssign = false;
                    return;
                }
                robot.buy();
                if (workbench.value < PRECIOUS_WORKBENCH_BOUNDARY) {
                    robot.buyAssign = false;
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

    //
//    private int getFastestCollectTime(int goodArriveTime, Berth berth) {
//        //这个泊位这个物品到达的时候泊位的数量
////        return goodArriveTime;
//        int estimateGoodsNums = berth.goodsNums;
//        estimateGoodsNums++;//加上这个货物时间
//        int minDistance = Integer.MAX_VALUE;
//        for (Boat boat : boats) {
//            if (boat.targetBerthId == berth.id) {
//                //来的是同一搜
//                int comingTime = boatMinToBerthDistance(boat, berth);
//                int remainSpace = boat.carry ? boat.capacity : boat.capacity - boat.num;
//                if (remainSpace >= estimateGoodsNums) {
//                    minDistance = min(minDistance, comingTime);
//                } else {
//                    minDistance = min(minDistance, comingTime + 2 * berth.minSellDistance);
//                }
//            } else {
//                int remainSpace = boat.carry ? boat.capacity : boat.capacity - boat.num;
//                int id = boat.targetBerthId == -1 ? 0 : boat.targetBerthId;
//                int comingTime = boatMinToBerthDistance(boat, berths.get(id));
//                int changeBerthTime = boatMinBerthToBethDistance(berths.get(id), berth);
//                remainSpace -= berths.get(id).goodsNums;
//                //去到估计的位置，在加一来一回
//                if (remainSpace >= estimateGoodsNums) {
//                    //可能切换，也可能不切换
//                    minDistance = min(minDistance, comingTime + changeBerthTime);
//                } else {
//                    //去另一个泊位装满，再回去，再去你这里
//                    minDistance = min(minDistance, comingTime + berths.get(id).minSellDistance + berth.minSellDistance);
//                }
//
//            }
//        }
//        //检查所有的船，找出这个泊位的这个物品最快被什么时候消费
//        //没到达也没法消费
//        return max(goodArriveTime, minDistance);
//    }
//
//    private int boatMinBerthToBethDistance(Berth fromBerth, Berth toBerth) {
//        return toBerth.getBoatMinDistance(fromBerth.corePoint, fromBerth.coreDirection);
//    }
//
    int totalCount = 0;


    int[][] fastQueue = new int[MAP_FILE_ROW_NUMS * MAP_FILE_COL_NUMS][2];
    long frameStartTime = 0;

    private boolean input() throws IOException {
        frameStartTime = System.currentTimeMillis();
        String line = inStream.readLine();
        printMost(line);
        if (line == null) {
            return false;
        }

        String[] parts = line.split(" ");
        int tmp = frameId;
        frameId = Integer.parseInt(parts[0]);
        curFrameDiff = frameId - tmp;
        if (tmp + 1 != frameId) {
            jumpCount += frameId - tmp - 1;
            printError("frame:" + frameId + ",jumpCount");
        }
        money = Integer.parseInt(parts[1]);

        //场上新增工作台
        int num = getIntInput();
        for (Map.Entry<Integer, Workbench2> entry : workbenches.entrySet()) {
            Workbench2 workbench = entry.getValue();
            workbench.remainTime -= curFrameDiff;//跳帧也要减过去
        }
        ArrayList<Integer> deleteIds = new ArrayList<>();//满足为0的删除
        ArrayList<Point> deletePos = new ArrayList<>();//满足为0的删除
        ArrayList<Integer> deleteValue = new ArrayList<>();//满足为0的删除
        for (Workbench2 workbench : workbenches.values()) {
            if (workbench.remainTime <= 0) {
                assert gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] == workbench.id;
                deleteIds.add(gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]);
                deletePos.add(workbench.pos);
                deleteValue.add(workbenches.get(gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]).value);
                gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] = -1;//说明删除了
            }
        }
        long l = System.currentTimeMillis();
        for (int i = 1; i <= num; i++) {
            if (workbenchCache.isEmpty()) {
                workbenchCache.offer(new Workbench2(-1));
            }
            Workbench2 workbench = workbenchCache.poll();
            assert workbench != null;
            workbench.id = workbenchId;
            workbench.input(gameMap, fastQueue);
            if (workbench.value > 0) {
                totalCount++;
            } else {
                totalCount--;
            }
            if (workbench.value == 0) {
                //被别人拿了，或者消失
                if (gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] != -1) {
                    deleteIds.add(gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]);
                    deletePos.add(workbench.pos);
                    deleteValue.add(workbenches.get(gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]).value);
                }
                gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] = -1;
                workbenchCache.offer(workbench);
                //回收
                continue;
            }
            workbench.remainTime = workbench.value > PRECIOUS_WORKBENCH_BOUNDARY ? GAME_FRAME : WORKBENCH_EXIST_TIME;
            //最小卖距离
            int minDistance = Integer.MAX_VALUE;
            for (Berth berth : berths) {
                if (berth.getRobotMinDistance(workbench.pos) < minDistance) {
                    minDistance = berth.getRobotMinDistance(workbench.pos);
                    workbench.minSellBerthId = berth.id;
                    workbench.minSellDistance = minDistance;
                }
            }
            if (minDistance != Integer.MAX_VALUE) {
                totalValidWorkBenchCount++;
                totalWorkbenchLoopDistance += 2 * workbench.minSellDistance;//买了卖
            }
            totalValue += workbench.value;
            workbenches.put(workbenchId, workbench);
            //掉帧导致的消失,因为一个位置只能有一个物品，现在有了肯定有问题
            if (gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] != -1) {
                deleteIds.add(gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]);
                deletePos.add(workbench.pos);
                deleteValue.add(workbenches.get(gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y]).value);
            }
            gameMap.curWorkbenchId[workbench.pos.x][workbench.pos.y] = workbenchId;
            workbenchId++;
            goodAvgValue = totalValue / workbenchId;
        }
        long r = System.currentTimeMillis();
        printDebug("workbech:" + (r - l));
        for (Integer deleteId : deleteIds) {
            workbenchCache.offer(workbenches.get(deleteId));
            workbenches.remove(deleteId);
        }
        deleteIds.clear();
        avgWorkBenchLoopDistance = 1.0 * totalWorkbenchLoopDistance / max(1, totalValidWorkBenchCount);

        workbenchesLock.clear();//动态需要解锁
        //场上机器人
        int totalRobotsCount = getIntInput();
        while (totalRobots.size() < totalRobotsCount) {
            totalRobots.add(new SimpleRobot());
        }
        for (int i = 0; i < totalRobotsCount; i++) {
            SimpleRobot simpleRobot = totalRobots.get(i);
            simpleRobot.input();
            //5帧不动且是贵重物品
//            if (simpleRobot.noMoveTime > FPS / 4 && !simpleRobot.belongToMe) {
//                //5帧不动，很可能对面进入了pending状态，此时自己暂时放弃这个工作台
//                int id = gameMap.curWorkbenchId[simpleRobot.p.x][simpleRobot.p.y];
//                if (id != -1 && workbenches.get(id).value > PRECIOUS_WORKBENCH_BOUNDARY) {
//                    //对面机器人在处于检索问答环节，自己先走了,对面一直不动我也没法走
//                    workbenchesLock.add(id);//暂时锁死这个工作台，先走
//                }
//            }
//            //200帧不动且有物品，说明对面应该是卡死了，这个位置所有工作台锁死
//            if (simpleRobot.noMoveTime > FPS * 5 && !simpleRobot.belongToMe) {
//                //5帧不动，很可能对面进入了pending状态，此时自己暂时放弃这个工作台，对面超过100帧不动,选择这个目标的我直接换
//                int id = gameMap.curWorkbenchId[simpleRobot.p.x][simpleRobot.p.y];
//                if (id != -1) {
//                    //对面机器人在处于检索问答环节，自己先走了,对面一直不动我也没法走
//                    workbenchesLock.add(id);//暂时锁死这个工作台，先走
//                }
//            }
//            if (simpleRobot.num == 2) {
//                simpleRobot.type = 1;
//            }
//            if (simpleRobot.num != simpleRobot.lastNum) {
//                Point lastP = simpleRobot.lastP;
//                Point curP = simpleRobot.p;
//                if (simpleRobot.num < simpleRobot.lastNum && !simpleRobot.goodList.isEmpty()) {
//                    //卸货
//                    while (!simpleRobot.goodList.isEmpty()) {
//                        int value = simpleRobot.goodList.pop();
//                        int berthId = gameMap.getBelongToBerthId(lastP);
//                        if (berthId == -1) {
//                            berthId = gameMap.getBelongToBerthId(curP);
//                        }
//                        if (berthId == -1) {
//                            int minDistance = Integer.MAX_VALUE;
//                            for (Berth berth : berths) {
//                                if (berth.getRobotMinDistance(simpleRobot.p) < minDistance) {
//                                    berthId = berth.id;
//                                }
//                            }
//                        }
//                        if (berthId == -1) {
//                            printError("error no find berth");
//                            continue;//不卸货了
//                        }
//                        if (simpleRobot.belongToMe) {
//                            totalPullGoodsCount++;
//                            totalPullGoodsValues += value;
//                            avgPullGoodsValue = 1.0 * totalPullGoodsValues / totalPullGoodsCount;
//                            pullScore += value;
//                        }
//                        berths.get(berthId).goods.add(value);
//                        berths.get(berthId).goodsNums++;
//                        berths.get(berthId).totalGoodsNums++;
//                    }
//                    if (simpleRobot.num > 0) {
//                        simpleRobot.goodList.push(50);
//                    }
//                } else {
//                    //装货
//                    //移动前拿的货，or 移动后拿的货
//                    boolean find = false;
//                    for (int j = 0; j < deletePos.size(); j++) {
//                        Point point = deletePos.get(j);
//                        //至少应该有一个点能匹配上
//                        if (point.equal(lastP) || point.equal(curP)) {
//                            simpleRobot.goodList.push(deleteValue.get(j));
//                            find = true;
//                            break;
//                        }
//                    }
//                    if (!find) {
//                        //假设拿了50的价值
//                        simpleRobot.goodList.push(50);
//                    }
//                    if (simpleRobot.goodList.size() < simpleRobot.num) {
//                        simpleRobot.goodList.push(50);
//                    }
//                }

//            }
//
//            //强制一样
//            if (simpleRobot.goodList.size() != simpleRobot.num) {
//                printError("error list count no equal num");
//                if (simpleRobot.goodList.size() < simpleRobot.num) {
//                    int load = simpleRobot.num - simpleRobot.goodList.size();
//                    for (int j = 0; j < load; j++) {
//                        simpleRobot.goodList.push(50);
//                    }
//                } else {
//                    int deLoad = simpleRobot.goodList.size() - simpleRobot.num;
//                    for (int j = 0; j < deLoad; j++) {
//                        simpleRobot.goodList.pop();
//                    }
//                }
//            }
        }

        //场上船
        int totalBoatsCount = getIntInput();
        while (totalBoats.size() < totalBoatsCount) {
            totalBoats.add(new SimpleBoat());
        }
        for (int i = 0; i < totalBoatsCount; i++) {
            SimpleBoat simpleBoat = totalBoats.get(i);
            simpleBoat.input();
            //掉帧解锁泊位
//            for (Berth berth : berths) {
//                if (berth.curBoatId == simpleboat.id) {
//                    if (!(simpleboat.corePoint.equal(berth.corePoint)
//                            && //不在泊位上，一定是离开了泊位
//                            simpleboat.direction == berth.coreDirection)) {
//                        berth.curBoatId = -1;
//                    }
//                    //行驶状态也没到达泊位
//                    if (simpleboat.status == 0) {
//                        berth.curBoatId = -1;
//                    }
//                }
//            }
//            if (simpleboat.status != 2 && simpleboat.lastStatus == 2) {
//                //从装货到进入恢复状态或者行驶状态，说明船舶离开泊位，解锁,别人可以闪现过去
//                for (Berth berth : berths) {
//                    if (berth.curBoatId == simpleboat.id) {
//                        berth.curBoatId = -1;
//                        break;
//                    }
//                }
//            }
//            //悲观估计，只要别人所在的方向和位置都和泊位核心点一样，且状态不为0，则认为别人在泊位
//            for (Berth berth : berths) {
//                if (simpleboat.status != 0 &&
//                        simpleboat.corePoint.equal(berth.corePoint)
//                        && simpleboat.direction == berth.coreDirection
//                        && berth.curBoatId == -1) {
//                    //此时我不能发berth指令，因为会导致慢
//                    berth.curBoatId = simpleboat.id;
//                }
//            }
//
//            if (simpleboat.status == 2) {
//                int berthId = gameMap.getBelongToBerthId(simpleboat.corePoint);
//                assert berthId != -1;
//                berths.get(berthId).curBoatId = simpleboat.id;//一定进入泊位
//            }
//            if (simpleboat.lastNum < simpleboat.num) {
//                //在装货
//                int berthId = gameMap.getBelongToBerthId(simpleboat.corePoint);
//                if (berthId == -1) {
//                    int minDistance = Integer.MAX_VALUE;
//                    for (Berth berth : berths) {
//                        if (berth.getBoatMinDistance(simpleboat.corePoint, simpleboat.direction) < minDistance) {
//                            minDistance = berth.getBoatMinDistance(simpleboat.corePoint, simpleboat.direction);
//                            berthId = berth.id;
//                        }
//                    }
//                }
//                if (berthId == -1) {
//                    printError("error in boat load");
//                    continue;//不装货了
//                }
//                int load = simpleboat.num - simpleboat.lastNum;
//                Berth berth = berths.get(berthId);
//                load = min(berth.goodsNums, load);
//                berth.goodsNums -= load;
//                for (int j = 0; j < load; j++) {
//                    assert !berth.goods.isEmpty();
//                    int poll = berth.goods.poll();
//                    simpleboat.value += poll;
//                }
//            }
        }

        //我方机器人
        ROBOTS_PER_PLAYER = getIntInput();
        if (robots.size() < ROBOTS_PER_PLAYER) {
            int size = robots.size();
            for (int i = size; i < ROBOTS_PER_PLAYER; i++) {
                robots.add(pendingRobots.poll());
            }
        }
        //机器人
        if (ROBOTS_PER_PLAYER != 0) {
            line = inStream.readLine();
            printMost(line);
            parts = line.trim().split(" ");
            for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
                int id = Integer.parseInt(parts[i]);
                totalRobots.get(id).belongToMe = true;
                Robot robot = robots.get(i);
                robot.id = i;
                robot.globalId = id;
                robot.input(totalRobots.get(id));
            }
        }
        PENDING_ROBOT_NUMS = getIntInput();
        HashSet<Integer> pendingIds = new HashSet<>();
        for (int i = 0; i < PENDING_ROBOT_NUMS; i++) {
            line = inStream.readLine();
            parts = line.split(" ");
            int id = Integer.parseInt(parts[0]);
            pendingIds.add(id);
            String question = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
            Robot robot = robots.get(id);
            if (!robot.isPending) {
                //上一帧不处于答题状态，则发送题目给背景线程回答
                robot.questionId = BackgroundThread.Instance().sendQuestion(question);
                robot.question = question;
                robot.ans = -1;
            } else {
                //上一帧处于答题状态，检查是否有答案了;
                robot.ans = BackgroundThread.Instance().getAnswer(robot.questionId);
            }
            robot.isPending = true;
        }
        if (robots.size() > ROBOTS_PER_PLAYER) {
            int size = robots.size();
            for (int i = ROBOTS_PER_PLAYER; i < size; i++) {
                pendingRobots.offer(robots.get(i));
            }
            for (int i = ROBOTS_PER_PLAYER; i < size; i++) {
                robots.remove(robots.size() - 1);
            }
            printError("error,no buy robot");
        }
        for (Robot robot : robots) {//回答完毕的重置为false
            if (!pendingIds.contains(robot.id) && robot.isPending) {
                //回答完毕，锁死
                workbenchesPermanentLock.add(robot.targetWorkBenchId);
                robot.isPending = false;
            }
        }

        //我方船
        BOATS_PER_PLAYER = getIntInput();
        if (boats.size() < BOATS_PER_PLAYER) {
            int size = boats.size();
            for (int i = size; i < BOATS_PER_PLAYER; i++) {
                boats.add(pendingBoats.poll());
            }
        }
        //船
        if (BOATS_PER_PLAYER != 0) {
            line = inStream.readLine();
            printMost(line);
            parts = line.trim().split(" ");
            for (int i = 0; i < BOATS_PER_PLAYER; i++) {
                int id = Integer.parseInt(parts[i]);
                Boat boat = boats.get(i);
                boat.id = i;
                boat.globalId = id;
                boat.input(totalBoats.get(id));
            }
        }
        if (boats.size() > BOATS_PER_PLAYER) {
            int size = boats.size();
            for (int i = BOATS_PER_PLAYER; i < size; i++) {
                pendingBoats.offer(boats.get(i));
            }
            for (int i = BOATS_PER_PLAYER; i < size; i++) {
                boats.remove(boats.size() - 1);
            }
            printError("error,no buy boats");
        }
        String okk = inStream.readLine();
        printMost(okk);
        long e = System.currentTimeMillis();
//        printError("frameId:" + frameId + ",inputTime:" + (e - frameStartTime));
//        printError("frame:" + frameId + ",size:" + workbenches.size());
        return true;
    }
}
