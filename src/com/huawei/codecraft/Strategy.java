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

    public final ArrayList<Robot> robots = new ArrayList<>();
    @SuppressWarnings("unchecked")
    public final HashSet<Integer>[] robotLock = new HashSet[MAX_ROBOTS_PER_PLAYER];
    public final ArrayList<Berth> berths = new ArrayList<>();
    public final ArrayList<Boat> boats = new ArrayList<Boat>();
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
        for (int i = 0; i < boatSellPoints.size(); i++) {
            boatSellPoints.get(i).init(i, gameMap);
        }

        BERTH_PER_PLAYER = getIntInput();
        //码头
        long maxUpdateTime = 0;
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
            long curTime = System.currentTimeMillis();
            if (curTime - startTime + maxUpdateTime > 1000 * 4 + 800) {
                berth.init(gameMap, true);
                continue;
            }
            berth.init(gameMap, false);
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
        long l1 = System.currentTimeMillis();
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
        long l2 = System.currentTimeMillis();
        printDebug("boat flash target update time:" + (l2 - l1));
        for (int i = 0; i < robotLock.length; i++) {
            robotLock[i] = new HashSet<>();
        }
        System.gc();//主动gc一下，防止后面掉帧
        outStream.print("OK\n");
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
            if (curDistance < minDistance || curRight > maxRight ||
                    (curRight == maxRight && curLeft > maxLeft) ||
                    (curRight == maxRight && curLeft == maxLeft && curTop > maxTop)
                    || (curRight == maxRight && curLeft == maxLeft
                    && curTop == maxTop && curBottom > maxBottom)) {
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
        for (int k = 0; k < DIR.length / 2; k++) {
            if (gameMap.boatIsAllInMainChannel(bestPoint, k)) {
                //找到传送点
                boatFlashMainChannelPoint[i][j] = new PointWithDirection(bestPoint, k);
            }
        }
    }

    public ArrayList<PointWithDirection> boatToBerth(Boat boat, Berth berth, int maxDeep, ArrayList<ArrayList<PointWithDirection>> otherPath, ArrayList<Integer> otherIds) {
        boat.targetBerthId = berth.id;
        return boatMoveToBerth(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction)
                , berth.id, new PointWithDirection(new Point(berth.corePoint), berth.coreDirection), maxDeep, berth.berthAroundPoints.size(), boat.id, otherPath, otherIds, boat.remainRecoveryTime);
    }

    public ArrayList<PointWithDirection> boatToBerthHeuristic(Boat boat, Berth berth) {
        boat.targetBerthId = berth.id;
        return boatMoveToBerthHeuristic(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction)
                , berth.id, new PointWithDirection(new Point(berth.corePoint), berth.coreDirection), boat.remainRecoveryTime
                , berth.boatMinDistance);
    }

    public ArrayList<PointWithDirection> boatToBerth(Boat boat, Berth berth, int maxDeep) {
        boat.targetBerthId = berth.id;
        return boatMoveToBerth(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction)
                , berth.id, new PointWithDirection(new Point(berth.corePoint), berth.coreDirection), maxDeep
                , berth.berthAroundPoints.size(), boat.remainRecoveryTime);
    }

    public ArrayList<PointWithDirection> boatToPoint(Boat boat, PointWithDirection pointWithDirection, int maxDeep) {
        return boatMoveToPoint(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction)
                , pointWithDirection, maxDeep
                , boat.remainRecoveryTime);
    }

    public ArrayList<PointWithDirection> boatToPoint(Boat boat, PointWithDirection pointWithDirection, int maxDeep
            , ArrayList<ArrayList<PointWithDirection>> otherPath, ArrayList<Integer> otherIds) {
        return boatMoveToPoint(gameMap, new PointWithDirection(new Point(boat.corePoint), boat.direction)
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

    long totalTime = 0;

    public ArrayList<Point> robotToPointHeuristic(Robot robot, Point end
            , ArrayList<ArrayList<Point>> otherPaths, int[][] heuristicCs) {
        return robotMoveToPointHeuristic(gameMap, robot.pos, end, otherPaths, heuristicCs);
    }


    public void mainLoop() throws IOException {
        while (input()) {
            dispatch();
            if (frameId == 15000) {
                printError("jumpTime:" + jumpCount + ",totalValue:"
                        + totalValue + ",pullValue:"
                        + pullScore + ",score:" + money);
            }
            outStream.print("OK\n");
            outStream.flush();
        }
    }


    private void dispatch() {
        robotDoAction();
        boatDoAction();
        if (frameId == 1) {
            for (int i = 0; i < 8; i++) {
                outStream.printf("lbot %d %d\n", robotPurchasePoint.get(0).x, robotPurchasePoint.get(0).y);
                robots.add(new Robot(this));
            }
            for (int i = 0; i < 1; i++) {
                outStream.printf("lboat %d %d\n", boatPurchasePoint.get(0).x, boatPurchasePoint.get(0).y);
                boats.add(new Boat(this, boatCapacity));
                outStream.printf("lboat %d %d\n", boatPurchasePoint.get(1).x, boatPurchasePoint.get(1).y);
                boats.add(new Boat(this, boatCapacity));
            }
        }
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
            if (!boatGreedyBuy(goodsNumList)) {
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
                boat.path = boatSellPoints.get(boat.targetSellId).moveFrom(boat.corePoint, boat.direction);
                if (boatCheckCrash(gameMap, boat.id, boat.path, otherPaths, otherIds, Integer.MAX_VALUE) != -1) {
                    //撞了
                    Point point = boatSellPoints.get(boat.targetSellId).point;
                    int minDeep = boatSellPoints.get(boat.targetSellId).getMinDistance(boat.corePoint, boat.direction);
                    ArrayList<PointWithDirection> avoidPath = boatToPoint(boat
                            , new PointWithDirection(point, -1), minDeep + BOAT_FIND_PATH_DEEP, otherPaths, otherIds);
                    if (!avoidPath.isEmpty()) {
                        boat.path.clear();
                        boat.path.addAll(avoidPath);
                    }
                }
            } else {
                //买
                assert boat.targetBerthId != -1;
                Berth targetBerth = berths.get(boat.targetBerthId);
                int deep = targetBerth.getBoatMinDistance(boat.corePoint, boat.direction);
                boat.path = boatToBerthHeuristic(boat, targetBerth);
                assert !boat.path.isEmpty();
                //不然肯定dij不对
                if (boatCheckCrash(gameMap, boat.id, boat.path, otherPaths, otherIds, Integer.MAX_VALUE) != -1) {
                    //撞了
                    ArrayList<PointWithDirection> avoidPath = boatToBerth(boat, targetBerth
                            , deep + BOAT_FIND_PATH_DEEP, otherPaths, otherIds);
                    if (!avoidPath.isEmpty()) {
                        boat.path.clear();
                        boat.path.addAll(avoidPath);
                    }
                }
                //目标有船，且自己里目标就差5帧了，此时直接搜到目标，后面再闪现
                if (targetBerth.curBoatId != -1 &&
                        targetBerth.curBoatId != boat.id) {
                    for (int i = 0; i < min(boat.path.size(), 5); i++) {
                        PointWithDirection next = boat.path.get(i);
                        if (next.point.equal(targetBerth.corePoint)) {
                            //有船且不是你,先去核心点等着闪现
                            ArrayList<PointWithDirection> toCorePath = boatToPoint(boat
                                    , new PointWithDirection(targetBerth.corePoint, -1)
                                    , 9999);
                            if (!toCorePath.isEmpty()) {
                                boat.path.clear();
                                boat.path.addAll(toCorePath);
                            }
                            break;
                        }
                    }
                }
            }
            otherPaths.add(boat.path);
            otherIds.add(boat.id);
        }

        otherPaths = new ArrayList<>();
        otherIds = new ArrayList<>();
        for (int i = 0; i < tmpBoats.length; i++) {
            Boat boat = tmpBoats[i];
            ArrayList<PointWithDirection> myPath = new ArrayList<>();
            if (boat.assigned) {
                for (int j = 0; j < min(BOAT_PREDICT_DISTANCE, boat.path.size()); j++) {
                    myPath.add(boat.path.get(j));
                }
            } else {
                //没assign
                for (int j = 0; j < BOAT_PREDICT_DISTANCE; j++) {
                    myPath.add(new PointWithDirection(boat.corePoint, boat.direction));
                }
                boat.path.clear();
                boat.path.addAll(myPath);
            }
            int crashId = boatCheckCrash(gameMap, boat.id, myPath, otherPaths, otherIds, BOAT_AVOID_DISTANCE);
            if (crashId != -1) {
                boats.get(crashId).beConflicted = FPS;
            }
            if (crashId != -1 && boat.status != 1) {
                //此时可以避让，则开始避让
                //避让
                for (boolean[] conflictPoint : gameMap.commonConflictPoints) {
                    Arrays.fill(conflictPoint, false);
                }
                for (boolean[] commonNoResultPoint : gameMap.commonNoResultPoints) {
                    Arrays.fill(commonNoResultPoint, false);
                }
                for (ArrayList<PointWithDirection> otherPath : otherPaths) {
                    //别人所在位置锁住
                    PointWithDirection pointWithDirection = otherPath.get(0);
                    ArrayList<Point> points = gameMap.getBoatPoints(pointWithDirection.point, pointWithDirection.direction);
                    for (Point point : points) {
                        if (!gameMap.isBoatMainChannel(point.x, point.y)) {
                            gameMap.commonConflictPoints[point.x][point.y] = true;
                        }
                    }
                }
                for (int j = 0; j < otherPaths.size(); j++) {
                    if (otherIds.get(j) == crashId) {
                        //撞到那个人的预测路径不是结果点
                        ArrayList<PointWithDirection> pointWithDirections = otherPaths.get(j);
                        for (PointWithDirection pointWithDirection : pointWithDirections) {
                            ArrayList<Point> points = gameMap.getBoatPoints(pointWithDirection.point, pointWithDirection.direction);
                            for (Point point : points) {
                                if (!gameMap.isBoatMainChannel(point.x, point.y)) {
                                    gameMap.commonNoResultPoints[point.x][point.y] = true;
                                }
                            }
                        }
                    }
                }
                ArrayList<PointWithDirection> result = boatGetSafePoints(gameMap, gameMap.boatCommonCs, myPath.get(0)
                        , gameMap.commonConflictPoints, gameMap.commonNoResultPoints, BOAT_AVOID_CANDIDATE_SIZE);
                if (!result.isEmpty()) {
                    PointWithDirection selectPoint = null;
                    double minDistance = Integer.MAX_VALUE;
                    for (PointWithDirection pointWithDirection : result) {
                        double curDis = (gameMap.boatCommonCs[pointWithDirection.point.x]
                                [pointWithDirection.point.y][pointWithDirection.direction] >> 2);
                        //到目标距离
                        if (boat.assigned) {
                            double toTargetDistance;
                            if (boat.carry) {
                                //买
                                toTargetDistance = boatSellPoints.get(boat.targetSellId).getMinDistance(pointWithDirection.point, pointWithDirection.direction);
                            } else {
                                toTargetDistance = berths.get(boat.targetBerthId).getBoatMinDistance(pointWithDirection.point, pointWithDirection.direction);
                            }
                            curDis = curDis + toTargetDistance / 2;
                        }
                        if (curDis < minDistance) {
                            selectPoint = pointWithDirection;
                            minDistance = curDis;
                        }
                    }
                    if (selectPoint != null) {
                        int oneDistance = (gameMap.boatCommonCs[selectPoint.point.x]
                                [selectPoint.point.y][selectPoint.direction] >> 2);
                        if (boat.assigned) {
                            if (boat.carry) {
                                oneDistance += boatSellPoints.get(boat.targetSellId).getMinDistance(selectPoint.point, selectPoint.direction);
                            } else {
                                oneDistance += berths.get(boat.targetBerthId).getBoatMinDistance(selectPoint.point, selectPoint.direction);
                            }
                        }
                        //计算闪现需要时间
                        PointWithDirection mid = getBoatFlashDeptPoint(boat.corePoint);
                        //计算到达时间
                        int waitTime = 1 + abs(boat.corePoint.x - mid.point.x) + abs(boat.corePoint.y - mid.point.y);
                        int twoDistance = waitTime;
                        if (boat.assigned) {
                            if (boat.carry) {
                                //买
                                twoDistance += boatSellPoints.get(boat.targetSellId).getMinDistance(mid.point, mid.direction);
                            } else {
                                twoDistance += berths.get(boat.targetBerthId).getBoatMinDistance(mid.point, mid.direction);
                            }
                        }

                        if (twoDistance <= oneDistance) {
                            //闪现避让
                            myPath.clear();
                            myPath.add(new PointWithDirection(boat.corePoint, boat.direction));
                            for (int j = 0; j < waitTime; j++) {
                                myPath.add(mid);
                            }
                        } else {
                            //正常避让
                            myPath = backTrackPath(gameMap, result.get(0), gameMap.boatCommonCs, 0);
                            if (myPath.size() == 1) {
                                myPath.add(myPath.get(0));
                            }
                            while (myPath.size() > BOAT_PREDICT_DISTANCE) {
                                myPath.remove(myPath.size() - 1);
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
                        myPath.add(new PointWithDirection(boat.corePoint, boat.direction));
                        for (int j = 0; j < waitTime; j++) {
                            myPath.add(mid);
                        }
                        boat.avoid = true;
                    } else {
                        boat.beConflicted = FPS;
                        boat.forcePri += 1;
                        otherPaths.clear();
                        otherIds.clear();
                        sortBoats(tmpBoats);
                        i = -1;
                        continue;
                    }
                }
            }
            //自己不动，切高优先级的撞到你，则不动
            PointWithDirection cur = myPath.get(0);
            PointWithDirection next = myPath.get(1);
            //不是整个船在主隧道上，且自己不动，则需要别人检查是否这个帧id小于他，或者id大于这一帧撞你移动后,都是同一个位置
            if (cur.equals(next) && !gameMap.boatIsAllInMainChannel(cur.point, cur.direction)) {
                for (int j = 0; j < otherPaths.size(); j++) {
                    assert otherPaths.get(j).size() >= 2;
                    PointWithDirection myStart = myPath.get(0);
                    assert myStart.equals(myPath.get(1));
                    PointWithDirection otherNext = otherPaths.get(j).get(1);
                    if (boatCheckCrash(gameMap, otherNext, myStart)) {
                        PointWithDirection otherStart = otherPaths.get(j).get(0);
                        otherPaths.get(j).clear();
                        for (int k = 0; k < 2; k++) {
                            otherPaths.get(j).add(otherStart);
                        }
                        boats.get(otherIds.get(j)).avoid = true;
                    }
                }
            }
            otherPaths.add(myPath);
            otherIds.add(boat.id);
        }
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

    public PointWithDirection getBoatFlashDeptPoint(Point point) {
        if (boatFlashMainChannelPoint[point.x][point.y] == null) {
            boatUpdateFlashPoint(point.x, point.y);
        }
        return boatFlashMainChannelPoint[point.x][point.y];
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
                for (Boat otherBoat : boats) {
                    if (otherBoat.assigned && otherBoat.targetSellId == boatSellPoint.id) {
                        distance++;
                    }
                }
                if (distance < minSellDistance) {
                    minSellDistance = distance;
                    selectPoint = boatSellPoint;
                }
            }
            if (selectPoint == null) {
                continue;
            }
            if (boat.num == boat.capacity || frameId + minSellDistance >= GAME_FRAME) {
                //卖
                int value = boat.num;
                double profit = 1.0 * value / minSellDistance;
                stat.add(new Stat(boat, selectPoint, profit));
            }
        }
        if (stat.isEmpty()) {
            return false;
        }
        Collections.sort(stat);
        Boat boat = stat.get(0).boat;
        BoatSellPoint boatSellPoint = stat.get(0).boatSellPoint;
        boat.assigned = true;
        boat.targetSellId = boatSellPoint.id;
        boat.carry = true;
        if (boat.status == 2) {
            //下一个状态不知道,现在状态可以变为0，直接不动,如果不变会认为仍然装货，不动
            boat.status = 0;
            boat.targetBerthId = -1;//重新买
        }
        return true;
    }

    private boolean boatGreedyBuy(int[] goodsNumList) {
        class Stat implements Comparable<Stat> {
            final Boat boat;
            final Berth berth;

            final BoatSellPoint sellPoint;

            final double profit;

            public Stat(Boat boat, Berth berth, BoatSellPoint sellPoint, double profit) {
                this.boat = boat;
                this.berth = berth;
                this.sellPoint = sellPoint;
                this.profit = profit;
            }

            @Override
            public int compareTo(Stat b) {
                return Double.compare(b.profit, profit);
            }
        }
        for (Berth berth : berths) {
            //停靠在泊位上了
            if (berth.curBoatId != -1
                    && berth.goodsNums > 0
                    && !boats.get(berth.curBoatId).buyAssign
                    && !boats.get(berth.curBoatId).carry) {
                //没去卖
                Boat boat = boats.get(berth.curBoatId);
                assert boat.targetBerthId == berth.id;
                //继续装货
                int needCount = boat.capacity - boat.num;
                boat.buyAssign = true;
                goodsNumList[berth.id] -= needCount;
                return true;
            }
        }
        ArrayList<Stat> stat = new ArrayList<>();
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
                toSellDistance = boatSellPoints.get(selectBoat.targetSellId)
                        .getMinDistance(selectBoat.corePoint, selectBoat.direction);
            }
            if (frameId + toSellDistance + minBuyDistance + minSellDistance >= GAME_FRAME) {
                continue;//不做决策
            }
            //价值是船上的个数加泊位个数
            int needCount = selectBoat.capacity - selectBoat.num;
            int value = min(needCount, berthGoodNum);
            value += selectBoat.num;
            double profit = 1.0 * value / (minSellDistance + minBuyDistance);
            stat.add(new Stat(selectBoat, berth, selectSellPoint, profit));
        }
        if (stat.isEmpty())
            return false;
        Collections.sort(stat);
        Boat boat = stat.get(0).boat;
        Berth berth = stat.get(0).berth;
        BoatSellPoint sellPoint = stat.get(0).sellPoint;
        boat.targetBerthId = berth.id;
        boat.targetSellId = sellPoint.id;
        boat.assigned = true;
        boat.buyAssign = true;
        int needCount = boat.capacity - boat.num;
        goodsNumList[berth.id] -= needCount;
        return true;
    }

    private int boatMinToBerthDistance(Boat boat, Berth berth) {
        int recoveryTime = boat.remainRecoveryTime;//核心，这个很重要
        if (boat.carry) {
            //卖
            assert boat.targetSellId != -1;
            int toSellDistance = boatSellPoints.get(boat.targetSellId).getMinDistance(boat.corePoint, boat.direction);
            int toBerthDistance = berth.getBoatMinDistance(boatSellPoints.get(boat.targetSellId).point
                    , boatSellPoints.get(boat.targetSellId).getEndDir(boat.corePoint, boat.direction));
            return recoveryTime + toSellDistance + toBerthDistance;
        }
        return recoveryTime + berth.getBoatMinDistance(boat.corePoint, boat.direction);
    }

//    private boolean boatGreedyBuy(int[] goodsNumList, int[] goodComingTimes) {
//        //三种决策，在去目标中的，
//        //在回家或者在家的
//        //在泊位的或者在泊位等待的
//        class Stat implements Comparable<Stat> {
//            final Boat boat;
//            final Berth berth;
//            final int count;//消费个数
//            final int updateTime;//消费个数
//            final double profit;
//
//            public Stat(Boat boat, Berth berth, int count, int updateTime, double profit) {
//                this.boat = boat;
//                this.berth = berth;
//                this.count = count;
//                this.profit = profit;
//                this.updateTime = updateTime;
//            }
//
//            @Override
//            public int compareTo(Stat b) {
//                return Double.compare(b.profit, profit);
//            }
//        }
//        double goodsComingTime = avgPullGoodsTime * BERTH_PER_PLAYER;
//        ArrayList<Boat> oneTypesBoats = new ArrayList<>();
//        ArrayList<Boat> twoTypesBoats = new ArrayList<>();
//        ArrayList<Boat> threeTypesBoats = new ArrayList<>();
//        for (Boat boat : boats) {
//            if (boat.assigned) {
//                continue;
//            }
//            if (boat.targetId != -1 && boat.status == 0) {
//                oneTypesBoats.add(boat);
//            } else if (boat.targetId == -1) {
//                twoTypesBoats.add(boat);//回家或者在家的
//            } else {
//                threeTypesBoats.add(boat);
//            }
//        }
//        //第一种，在移动向泊位的，不切换目标，直接消耗
//        for (Boat boat : oneTypesBoats) {
//            int needCount = boat.capacity - boat.num;
//            if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
//                return true;
//        }
//
//        //第二种,选择泊位货物最多的，一样多则选updateTime最早的，一样早选择最远的
//        //考虑在移动，运输中也算不能返回，移动完才可以动？
//        ArrayList<Stat> stat = new ArrayList<>();
//        //选择折现价值最大的
//        for (Berth buyBerth : berths) {
//            Boat selectBoat = null;//最近的先决策
//            int minDistance = Integer.MAX_VALUE;
//            for (Boat boat : twoTypesBoats) {
//                int distance = getBoatToBerthDistance(buyBerth, boat);
//                if (distance < minDistance) {
//                    minDistance = distance;
//                    selectBoat = boat;
//                }
//            }
//            if (selectBoat == null) {
//                continue;
//            }
//
//            if (frameId + minDistance + buyBerth.transportTime >= GAME_FRAME) {
//                continue;//货物装不满，则不管
//            }
//
////            int buyTime = minDistance;
////            int sellTime = buyBerth.transportTime;
////            int needCount = selectBoat.capacity;
////            int remainTime = GAME_FRAME - buyTime - sellTime - frameId;
////            int realCount = min(needCount, remainTime / buyBerth.loadingSpeed);//装货
////            realCount = min(realCount, goodsNumList[buyBerth.id] + (int) (remainTime / goodsComingTime));//来货
////            int loadTime = (int) ceil(1.0 * realCount / buyBerth.loadingSpeed);
////            int totalWaitTime = goodComingTimes[buyBerth.id] + (int) ceil(max(0, realCount - goodsNumList[buyBerth.id]) * goodsComingTime);
////            int arriveWaitTime = max(0, totalWaitTime - buyTime);//到达之后的等待时间
////            double profit = 1.0 * realCount / (buyTime + max(arriveWaitTime, loadTime) + sellTime);
//
//            //只能装那么多，尽量去装吧,可以切泊位可能上面决策好一点，不可以切泊位，还是看数量高分
//            int realCount = selectBoat.capacity;
//            double profit = 1e10 + goodsNumList[buyBerth.id] * 1e10 - goodComingTimes[buyBerth.id] * 1e5 + minDistance;
//            int totalWaitTime = goodComingTimes[buyBerth.id] +
//                    (int) ceil(max(0, selectBoat.capacity - goodsNumList[buyBerth.id])
//                            * goodsComingTime);
//            stat.add(new Stat(selectBoat, buyBerth, min(goodsNumList[buyBerth.id], realCount)
//                    , totalWaitTime, profit));
//        }
//        if (!stat.isEmpty()) {
//            Collections.sort(stat);
//            //同一个目标不用管
////            boatRealDecision(goodsNumList, goodComingTimes, stat.get(0).berth
////                    , stat.get(0).boat, stat.get(0).updateTime, stat.get(0).count);
//            return true;
//        }
//
//        //第三种，在泊位上或者等待的,货物数量和,单位时间价值
//        for (Boat boat : threeTypesBoats) {
//            //到每个泊位距离一定是500
//            int needCount = boat.capacity - boat.num;
//            if (boatDecisionType == DECISION_ON_ORIGIN) {
//                //不可以切换泊位,
//                assert boat.estimateComingBerthId == boat.targetId;
//                if (!BOAT_NO_WAIT || frameId + berths.get(boat.targetId).transportTime * 3 > GAME_FRAME || berths.get(boat.targetId).goodsNums > 0) {
//                    if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
//                        return true;
//                } else {
//                    //回家
//                    boat.remainTime = berths.get(boat.targetId).transportTime;
//                    boat.status = 0;//移动中
//                    boat.targetId = -1;
//                    boat.estimateComingBerthId = -1;
//                    boat.exactStatus = IN_ORIGIN_TO_BERTH;
//                    return true;
//                }
//            }
//
//            if (BOAT_NO_WAIT && frameId + berths.get(boat.targetId).transportTime * 3 < GAME_FRAME) {
//                if (berths.get(boat.targetId).goodsNums > 0) {
//                    if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
//                        return true;
//                }
//                //没货物了，选择切泊位买了走人，或者回家
//                double maxProfit = 1.0 * boat.num / berths.get(boat.targetId).transportTime / 2;
//                Berth changeBerth = null;
//                int maxWaitTime = -1;
//                int maxCount = 0;
//
//                //来回一趟基本价值，如果有人超过了，则切泊位，否则回家
//                for (Berth berth : berths) {
//                    if (boat.targetId == berth.id) {
//                        continue;
//                    }
//                    int buyTime = BERTH_CHANGE_TIME;
//                    int sellTime = berth.transportTime;
//                    if (frameId + buyTime + sellTime >= GAME_FRAME) {
//                        continue;//不干
//                    }
//                    int remainTime = GAME_FRAME - buyTime - sellTime - frameId;
//                    int realCount = min(needCount, remainTime / berth.loadingSpeed);//装货
//                    realCount = min(realCount, goodsNumList[berth.id] + (int) ((buyTime - goodComingTimes[berth.id]) / goodsComingTime));//来货
//                    int loadTime = (int) ceil(1.0 * realCount / berth.loadingSpeed);
//                    int totalWaitTime = goodComingTimes[berth.id] + (int) ceil(max(0, realCount - goodsNumList[berth.id]) * goodsComingTime);
//                    double profit = 1.0 * (realCount + boat.num) / (berths.get(boat.targetId).transportTime + 1.0 * boat.num
//                            / berths.get(boat.targetId).loadingSpeed + buyTime + loadTime + sellTime);
//                    if (profit > maxProfit) {
//                        changeBerth = berth;
//                        maxProfit = profit;
//                        maxWaitTime = totalWaitTime;
//                        maxCount = min(realCount, goodsNumList[berth.id]);
//                    }
//
//                }
//                if (changeBerth != null) {
//                    stat.add(new Stat(boat, changeBerth, min(maxCount, goodsNumList[changeBerth.id]), maxWaitTime, maxProfit));
//                } else {
//                    //回家
//                    if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingTime, needCount))
//                        return true;
//                }
//            } else {
//                //目前船所在的泊位的来货速度,如果来货速度实在太慢，不需要等，直接切目标
//                int curBerthGoodsComingTime = frameId / max(1, berths.get(boat.targetId).totalGoodsNums);
//                for (Berth berth : berths) {
//                    int buyTime = boat.targetId == berth.id ? 0 : BERTH_CHANGE_TIME;
//                    int sellTime = berth.transportTime;
//                    if (frameId + buyTime + sellTime >= GAME_FRAME) {
//                        continue;//不干
//                    }
//                    if (berth.id != boat.targetId && berths.get(boat.targetId).goodsNums > 0) {
//                        continue;//有货物了，船只能选择这个泊位不变
//                    }
//
//                    int remainTime = GAME_FRAME - buyTime - sellTime - frameId;
//                    int realCount = min(needCount, remainTime / berth.loadingSpeed);//装货
//                    realCount = min(realCount, goodsNumList[berth.id] + (int) ((remainTime + buyTime - goodComingTimes[berth.id]) / goodsComingTime));//来货
//                    int loadTime = (int) ceil(1.0 * realCount / berth.loadingSpeed);
//                    int totalWaitTime = goodComingTimes[berth.id] + (int) ceil(max(0, realCount - goodsNumList[berth.id]) * goodsComingTime);
//                    int arriveWaitTime = max(0, totalWaitTime - buyTime);//到达之后的等待时间
//                    //todo 可调参
//                    if (berth.id != boat.targetId && arriveWaitTime > 0//留一半阈值,&&remainTime > 2 * berth.loadingSpeed * realCount
//                            && curBerthGoodsComingTime * 2 > goodsComingTime//来货速度的两倍小于平均来货速度，直接切目标
//                            && remainTime > 2 * berth.transportTime - buyTime
//                    ) {
//                        continue;//不是同一个泊位，且到达之后需要等待，那么先不去先,有问题，最后还是不会切泊位的
//                    }
//                    double profit = realCount + 1.0 * realCount / (buyTime + max(arriveWaitTime, loadTime) + sellTime);
//                    //增益超过保持因子，则切换目标
//                    stat.add(new Stat(boat, berth, min(realCount, goodsNumList[berth.id]), totalWaitTime, profit));
//                }
//            }
//        }
//        if (!stat.isEmpty()) {
//            Collections.sort(stat);
//            //同一个目标不用管
////            boatRealDecision(goodsNumList, goodComingTimes, stat.get(0).berth
////                    , stat.get(0).boat, stat.get(0).updateTime, stat.get(0).count);
//            return true;
//        }
//        return false;
//
//    }

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


//    private boolean updateSumTimes(int[] goodsNumList, int[] goodComingTimes, Boat boat, double goodsComingSpeed, int needCount) {
//        int comingBerthId = boat.estimateComingBerthId;
//        if (comingBerthId != -1) {
//            int comingCount = max(0, needCount - goodsNumList[comingBerthId]);
//            int addComingTime = 0;
//            if (comingCount > 0) {
//                addComingTime = (int) ceil(comingCount * goodsComingSpeed);
//            }
//            goodsNumList[comingBerthId] -= min(goodsNumList[comingBerthId], needCount);
//            goodComingTimes[comingBerthId] += addComingTime;
//            boat.assigned = true;
//            return true;
//        }
//        return false;
//    }


    @SuppressWarnings("all")
//    private int getCanReachBerthsCount() {
//        int count = 0;
//        for (Berth berth : berths) {
//            for (Boat boat : boats) {
//                int distance = getBoatToBerthDistance(berth, boat);
//                if (frameId + distance + berth.transportTime < GAME_FRAME) {
//                    count++;
//                    break;
//                }
//            }
//        }
//        return count;
//    }

//    private static int getBoatToBerthDistance(Berth buyBerth, Boat boat) {
//        int dist;
//        if (boat.targetId == buyBerth.id) {
//            dist = boat.remainTime;
//            if (boat.exactStatus == IN_BERTH_WAIT) {
//                dist = 1;//泊位外等待为1
//            }
//        } else if (boat.lastArriveTargetId != -1 && boat.targetId != -1) {
//            //泊位内，泊位等待，泊位切换中
//            dist = BERTH_CHANGE_TIME;
//        } else if (boat.targetId == -1 && boat.status == 0) {//往原点运输中
//            dist = boat.remainTime + buyBerth.transportTime;//虚拟点到泊位时间
//        } else {
//            dist = buyBerth.transportTime;//虚拟点到泊位时间
//        }
//        return dist;
//    }

    private void robotDoAction() {
        workbenchesLock.clear();//动态需要解锁
        for (HashSet<Integer> set : robotLock) {
            set.clear();
        }
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
        for (int i = 0; i < tmpRobots.length; i++) {
            tmpRobots[i] = robots.get(i);
        }
        sortRobots(tmpRobots);
        //1.选择路径,和修复路径
        ArrayList<ArrayList<Point>> otherPaths = new ArrayList<>();
        for (int i = 0; i < tmpRobots.length; i++) {
            Robot robot = tmpRobots[i];
            if (!robot.assigned) {
                continue;
            }

            ArrayList<Point> path;
            int[][] heuristicPoints;
            if (robot.carry) {
                ArrayList<Integer> candidate = berths.get(robot.targetBerthId).robotMinDistanceIndexes[robot.pos.x][robot.pos.y];
                //找到一条路径不与之前的路径相撞,找不到，选择第一条路径
                path = berths.get(robot.targetBerthId).robotDijkstra.get(candidate.get(0)).moveFrom(robot.pos);
                for (int j = 1; j < candidate.size(); j++) {
                    int index = candidate.get(j);
                    ArrayList<Point> candidatePath = berths.get(robot.targetBerthId).robotDijkstra.get(index).moveFrom(robot.pos);
                    if (!robotCheckCrash(gameMap, candidatePath, otherPaths)) {
                        path = candidatePath;
                        break;
                    } else {
                        //尝试找不撞的
                        Point end = berths.get(robot.targetBerthId).berthPoints.get(index);
                        heuristicPoints = berths.get(robot.targetBerthId).robotDijkstra.get(index).cs;
                        ArrayList<Point> avoidPath = robotToPointHeuristic(robot, end, otherPaths, heuristicPoints);
                        if (!avoidPath.isEmpty()) {
                            path = candidatePath;
                            break;
                        }
                    }
                }
                heuristicPoints = berths.get(robot.targetBerthId).robotDijkstra.get(0).cs;
            } else {
                path = workbenches.get(robot.targetWorkBenchId).dijkstra.moveFrom(robot.pos);
                heuristicPoints = workbenches.get(robot.targetWorkBenchId).dijkstra.cs;
            }
            //检查是否与前面机器人相撞，如果是，则重新搜一条到目标点的路径，极端情况，去到物品消失不考虑
            if (robotCheckCrash(gameMap, path, otherPaths)) {
                //尝试搜一条不撞的路径
                assert !path.isEmpty();
                Point end = gameMap.discreteToPos(path.get(path.size() - 1));
                ArrayList<Point> avoidPath = robotToPointHeuristic(robot, end, otherPaths, heuristicPoints);
                if (avoidPath.isEmpty() && !robot.redundancy) {
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
            otherPaths.add(path);
        }

        //2.碰撞避免
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            //固定只预测未来一个格子，就是两个小格子，
            //预测他未来两个格子就行，四下，如果冲突，则他未来一个格子自己不能走，未来第二个格子自己尽量也不走
            Robot robot = tmpRobots[i];
            robotsPredictPath[robot.id] = new ArrayList<>();
            if (!robot.assigned) {
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
                        if ((mid.equal(robotsPredictPath[tmpRobot.id].get(0)) && !gameMap.isRobotDiscreteMainChannel(mid.x, mid.y))
                                || (end.equal(robotsPredictPath[tmpRobot.id].get(1)) && !gameMap.isRobotDiscreteMainChannel(end.x, end.y))) {
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
                    robot.beConflicted = FPS;
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
            robot.finish();
            if (robot.beConflicted-- < 0 && robot.forcePri != 0) {
                robot.forcePri = 0;
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
        for (Robot robot : robots) {
            robot.avoid = false;
        }
    }


    int robotMinToWorkbenchDistance(Robot robot, Workbench workbench) {
        if (robot.carry) {
            assert (robot.assigned);
            int toBerth = berths.get(robot.targetBerthId).getRobotMinDistance(robot.pos);
            int k = berths.get(robot.targetBerthId).robotMinDistanceIndexes[robot.pos.x][robot.pos.y].get(0);
            int toWorkBench = berths.get(robot.targetBerthId).robotDijkstra.get(k).getMoveDistance(workbench.pos);
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
            double maxProfit = -GAME_FRAME;
            Berth selectSellBerth = null;
            for (Berth sellBerth : berths) {
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
            robot.priority = workbench.value;
            if (!robot.carry && robot.pos.equal(workbench.pos)) {
                //没带物品，且到目标，一定买，然后重新决策一个下一个目标买
                robot.buy();
                robot.buyAssign = false;
            }
        } else {
            robot.redundancy = true;
            robot.estimateUnloadTime = frameId + berth.getRobotMinDistance(robot.pos);
            if (gameMap.getBelongToBerthId(robot.pos) == berth.id) {
                printError("error,no pre sell");
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
            robots.get(i).input();
        }
        while (robots.size() > ROBOTS_PER_PLAYER) {
            robots.remove(robots.size() - 1);
            printError("error,no buy robot");
        }
        BOATS_PER_PLAYER = getIntInput();
        //船
        for (int i = 0; i < BOATS_PER_PLAYER; i++) {
            Boat boat = boats.get(i);
            boat.input();
            //装货
            int load = boat.num - boat.lastNum;
            if (load > 0) {
                assert boat.targetBerthId != -1;
                berths.get(boat.targetBerthId).goodsNums -= load;
                for (int j = 0; j < load; j++) {
                    assert !berths.get(boat.targetBerthId).goods.isEmpty();
                    Integer value = berths.get(boat.targetBerthId).goods.poll();
                    assert value != null;
                    boat.value += value;
                    berths.get(boat.targetBerthId).totalValue -= value;
                }
                boat.lastNum = boat.num;
            }
        }


        while (boats.size() > BOATS_PER_PLAYER) {
            boats.remove(boats.size() - 1);
            printError("error,no buy boats");
        }
        String okk = inStream.readLine();
        printMost(okk);
        return true;
    }
}
