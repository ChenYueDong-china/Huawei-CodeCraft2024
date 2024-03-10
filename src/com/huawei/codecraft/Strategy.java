package com.huawei.codecraft;

import java.io.IOException;
import java.util.*;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.RobotAction.RA_BUY;
import static com.huawei.codecraft.RobotAction.RA_SELL;
import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.*;

public class Strategy {


    private int money;

    public GameMap gameMap;

    public static int workbenchId = 0;
    public final HashMap<Integer, Workbench> workbenches = new HashMap<>();
    public final HashSet<Integer> workbenchesLock = new HashSet<>();//锁住某些工作台
    public final Robot[] robots = new Robot[ROBOTS_PER_PLAYER];
    @SuppressWarnings("unchecked")
    public final HashSet<Integer>[] robotLock = new HashSet[ROBOTS_PER_PLAYER];

    public final Berth[] berths = new Berth[BERTH_PER_PLAYER];

    public final Boat[] boats = new Boat[BOATS_PER_PLAYER];

    @SuppressWarnings("unchecked")
    public ArrayList<Point>[] robotsPredictPath = new ArrayList[ROBOTS_PER_PLAYER];

    public void init() throws IOException {
        char[][] mapData = new char[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
        gameMap = new GameMap();
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            fgets(mapData[i], inStream);
        }
        gameMap.setMap(mapData);
        //码头
        for (int i = 0; i < BERTH_PER_PLAYER; i++) {
            String s = inStream.readLine();
            printMOST(s);
            String[] parts = s.trim().split(" ");
            int id = Integer.parseInt(parts[0]);
            berths[id] = new Berth();
            berths[id].leftTopPos.y = Integer.parseInt(parts[1]);
            berths[id].leftTopPos.x = Integer.parseInt(parts[2]);
            berths[id].transportTime = Integer.parseInt(parts[3]);
            berths[id].loadingSpeed = Integer.parseInt(parts[4]);
            berths[id].id = i;
            berths[id].init(gameMap);

        }
        String s = inStream.readLine().trim();
        printMOST(s);
        int boatCapacity = Integer.parseInt(s);
        //船
        for (int i = 0; i < BOATS_PER_PLAYER; i++) {
            boats[i] = new Boat(boatCapacity);
            boats[i].id = i;
            robotLock[i] = new HashSet<>();
        }
        //机器人
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            robots[i] = new Robot(this);
            robots[i].id = i;
        }

        String okk = inStream.readLine();
        printMOST(okk);
        outStream.print("OK\n");
    }

    public void mainLoop() throws IOException {

        while (input()) {
            dispatch();
            outStream.print("OK\n");
        }

    }

    private void dispatch() {
//        while (greedySell()) ; //决策
//        workbenchesLock.clear();//动态需要解锁
//        for (HashSet<Integer> set : robotLock) {
//            set.clear();
//        }
//        while (greedyBuy()) ;
//
//        robotDoAction();
        //先做测试
        //9号去128,165
        if (frameId < 50) {
            Dijkstra dijkstra = new Dijkstra();
            dijkstra.init(new Point(14, 189), gameMap);
            dijkstra.update();
            robots[8].path.addAll(dijkstra.moveFrom(robots[8].pos));
            robots[8].finish();
        }
        if (frameId == 50) {
            outStream.printf("get %d\n", 8);
        }
        if (frameId >= 50 && frameId < 100) {
            Dijkstra dijkstra = new Dijkstra();
            dijkstra.init(new Point(4, 174), gameMap);
            dijkstra.update();
            robots[8].path.addAll(dijkstra.moveFrom(robots[8].pos));
            robots[8].finish();
        }
        if (frameId == 100) {
            outStream.printf("move %d %d\n", 8, 1);
            outStream.printf("pull %d\n", 8);
        }
        if (frameId == 1) {
            //开船过来
            outStream.printf("ship %d %d", 0, 9);
//            outStream.printf("ship %d %d", 1, 9);
        }
        if (frameId == 1 + 1219 + 1) {//到达之后
            //1220帧到达，然后开始装货，。，至少到达一帧才能走
            outStream.printf("go %d", 0);
        }
//        if (frameId == 1 + 1219 + 1219) {
//            printERROR("money" + money);
//        }
//        if (frameId > 50) {
//            outStream.printf("move %d %d\n", 9, 0);
//            outStream.printf("move %d %d\n", 7, 0);
//
//        }

        //船只选择去哪个泊位


        //装载货物
//        for (Boat boat : boats) {
//            if (boat.remainTime == 0 && boat.targetId != -1 && berths[boat.targetId].goodsNums > 0) {
//                //开始装货
//                int load = min(berths[boat.targetId].loadingSpeed, berths[boat.targetId].goodsNums);
//                load = min(load, boat.capacity - boat.num);//剩余空间
//                boat.num += load;
//                berths[boat.targetId].goodsNums -= load;
//            }
//        }


    }

    private void robotDoAction() {
        //todo 选择路径，修复路径
        Robot[] tmpRobots = new Robot[ROBOTS_PER_PLAYER];
        System.arraycopy(robots, 0, tmpRobots, 0, ROBOTS_PER_PLAYER);
        Arrays.sort(tmpRobots, (o1, o2) -> {
            if (o1.forcePri != o2.forcePri) {//暴力优先级最高
                return o2.forcePri - o1.forcePri;
            }
            if (o1.redundancy ^ o2.redundancy) {//没冗余时间
                return o2.redundancy ? -1 : 1;
            }
            if (o1.carry ^ o2.carry) {//携带物品高
                return o1.carry ? -1 : 1;
            }
            return o1.carryValue - o2.carryValue;//看价值
        });
        //1.选择路径,和修复路径
        for (int i = 0; i < tmpRobots.length; i++) {
            Robot robot = tmpRobots[i];
            if (!robot.assigned) {
                continue;
            }

            ArrayList<Point> path;
            int[][] heuristicPoints;
            if (robot.carry) {
                ArrayList<Point> candidate = berths[robot.targetBerthId].minDistancePos[robot.pos.x][robot.pos.y];
                //找到一条路径不与之前的路径相撞,找不到，选择第一条路径
                path = berths[robot.targetBerthId].dijkstras[candidate.get(0).x][candidate.get(0).y].moveFrom(robot.pos);
                heuristicPoints = berths[robot.targetBerthId].dijkstras[candidate.get(0).x][candidate.get(0).y].cs;
                for (int j = 1; j < candidate.size(); j++) {
                    Point point = candidate.get(j);
                    ArrayList<Point> candidatePath = berths[robot.targetBerthId].dijkstras[point.x][point.y].moveFrom(robot.pos);
                    if (!checkCrash(candidatePath, robot.id)) {
                        path = candidatePath;
                        break;
                    }
                }
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
        Arrays.fill(robotsPredictPath, null);
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            //固定只预测未来一个格子，就是两个小格子，
            //预测他未来两个格子就行，四下，如果冲突，则他未来一个格子自己不能走，未来第二个格子自己尽量也不走
            Robot robot = tmpRobots[i];
            robotsPredictPath[robot.id] = new ArrayList<>();
            if (!robot.assigned) {
                //未来一格子在这里不动，如果别人撞过来，则自己避让
                for (int j = 1; j <= 2; j++) {
                    robotsPredictPath[robot.id].add(robot.pos);
                }
            } else {
                assert robot.path.size() >= 3;//包含起始点
                //至少有未来一个格子
                for (int j = 1; j <= 4; j++) {
                    if (robot.path.size() > j + 1) {
                        robotsPredictPath[robot.id].add(robot.path.get(j));
                    }
                }
            }
            int crashId = -1;
            for (int j = 0; j < i; j++) {
                //一个格子之内撞不撞
                for (int k = 0; k < 2; k++) {
                    if (robotsPredictPath[robot.id].get(k) == robotsPredictPath[tmpRobots[j].id].get(k)) {
                        crashId = tmpRobots[j].id;
                        break;
                    }
                }
            }
            int avoidId = robot.id;
            int count = 0;
            //todo 这个不可能撞两，因为前面有做避让
            //todo 避让只选择上下左右四个格子，或者不动，如果四个都撞前面的，让前面那个人让。直到没人撞，都可以让时，
            // 先考虑自己离目标点距离，
            // 一样的话考虑不在别人路径上
            //这个预测路径包含了起始点，不管了
            boolean[] avoids = new boolean[ROBOTS_PER_PLAYER];
            Arrays.fill(avoids, false);
            while (crashId != -1) {
                if (count == ROBOTS_PER_PLAYER) {
                    printERROR("the code is worst");
                    assert false;
                    break;
                }
                //看看自己是否可以避让，不可以的话就说明被夹住了,让冲突点去让,并且自己强制提高优先级50帧
                robots[crashId].beConflicted = FPS;
                ArrayList<Point> candidates = new ArrayList<>();
                candidates.add(robots[avoidId].pos);
                for (int j = 0; j < DIR.length / 2; j++) {
                    candidates.add(robots[avoidId].pos.add(DIR[j]));
                }
                Point crashPoint = gameMap.discreteToPos(robotsPredictPath[crashId].get(1));
                Point result = new Point(-1, -1);
                int bestDist = Integer.MAX_VALUE;
                ArrayList<Integer> crashIds = new ArrayList<>();
                crashIds.add(crashId);
                for (Point candidate : candidates) {
                    if (candidate.equal(crashPoint)) {
                        continue;
                    }
                    //todo 检查是否会撞到任意一个其他人,会的话也不是候选点
                    //细化成两个去判断
                    boolean crash = false;
                    for (Robot tmpRobot : tmpRobots) {
                        if (tmpRobot.id == crashId || tmpRobot.id == avoidId) {
                            continue;
                        }
                        Point start = gameMap.posToDiscrete(robots[avoidId].pos);
                        Point end = gameMap.posToDiscrete(candidate);
                        Point mid = start.add(end).div(2);
                        if (mid.equal(robotsPredictPath[tmpRobot.id].get(0)) || end.equal(robotsPredictPath[tmpRobot.id].get(1))) {
                            crashIds.add(tmpRobot.id);
                            crash = true;
                            break;
                        }
                    }
                    if (crash) {
                        continue;
                    }


                    int dist;
                    if (!robots[avoidId].carry) {
                        dist = workbenches.get(robots[avoidId].targetWorkBenchId).getMinDistance(candidate);
                    } else {
                        dist = berths[robots[avoidId].targetBerthId].getMinDistance(candidate);
                    }
                    assert dist != Integer.MAX_VALUE;
                    if (robotsPredictPath[crashId].size() == 4 && !candidate.equal
                            (gameMap.discreteToPos(robotsPredictPath[crashId].get(3)))) {
                        dist -= 1;//不在对面路径上认为更好一点
                    }
                    if (dist < bestDist) {
                        result = candidate;
                        bestDist = dist;
                    }
                }
                if (!result.equal(-1, -1)) {
                    crashId = -1;
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
                    robots[avoidId].forcePri = 1;
                    //不可能没一个机器人没法避让，如果这样说明陷入了死锁，但是这个地图不可能思索
                    avoids[avoidId] = true;//下次这个机器人不避让，避免死循环
                    int tmp = avoidId;
                    //寻找避让id
                    for (Integer id : crashIds) {
                        if (!avoids[id]) {
                            avoidId = id;
                            break;
                        }
                    }
                    crashId = tmp;//撞到的让
                }
                count++;
            }
        }


        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            if (robots[i].avoid) {
                Point start = robots[i].path.get(0);
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
        int[][] discreteCs = new int[MAP_DISCRETE_WIDTH][MAP_DISCRETE_HEIGHT]; //前面2位是距离，后面的位数是距离0xdistdir
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
                ArrayList<Point> result = getPathByCs(discreteCs, top);
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
                if (!gameMap.canReachDiscrete(dx, dy) || discreteCs[dx][dy] != Integer.MAX_VALUE) {
                    continue; // 不可达或者访问过了
                }
                if (checkCrashInDeep(robotId, deep + 1, dx, dy)
                        || checkCrashInDeep(robotId, deep + 2, dx + dir.x, dy + dir.y)) {
                    continue;
                }
                Point cur = gameMap.discreteToPos(top.x, top.y);
                Point next = gameMap.discreteToPos(dx + dir.x, dy + dir.y);
                if (cs[cur.x][cur.y] != cs[next.x][next.y]) {
                    //启发式剪枝，不是距离更近则直接结束
                    continue;
                }
                discreteCs[dx][dy] = ((deep + 1) << 2) + dirIdx;//第一步
                dx += dir.x;
                dy += dir.y;
                assert (gameMap.canReachDiscrete(dx, dy) && discreteCs[dx][dy] == Integer.MAX_VALUE);//必定可达
                discreteCs[dx][dy] = ((deep + 2) << 2) + dirIdx;//第一步
                stack.push(new Pair(deep + 2, new Point(dx, dy)));
            }
        }
        //搜不到，走原路
        return false;
    }


    private boolean checkCrashInDeep(int robotId, int deep, int dx, int dy) {
        for (Robot robot : robots) {
            if (!robot.assigned || robot.id == robotId) {
                continue;
            }
            if (robot.path.size() < deep + 1) {
                continue;
            }
            if (robot.path.get(deep).equal(dx, dy)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkCrash(ArrayList<Point> path, int robotId) {
        return checkCrash(path, robotId, Integer.MAX_VALUE);
    }

    private boolean checkCrash(ArrayList<Point> path, int robotId, int maxDetectDistance) {
        //默认检测整条路径
        for (Robot robot : robots) {
            if (robot.id == robotId || robot.path.isEmpty()) {
                continue;
            }
            int detectDistance = min(maxDetectDistance, min(path.size(), robot.path.size()));
            for (int i = 0; i < detectDistance; i++) {
                if (robot.path.get(i).equal(path.get(i))) {
                    return true;
                }
            }
        }
        return false;
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
            Robot selectRobot = null;
            int minDist = Integer.MAX_VALUE;
            for (Robot robot : robots) {
                if (robot.assigned || robot.carry) {
                    continue;
                }
                if (buyWorkbench.canReach(robot.pos)) {
                    continue; //不能到达
                }
                if (robotLock[robot.id].contains(buyWorkbench.id)) {
                    continue;
                }
                int dist = buyWorkbench.getMinDistance(robot.pos);
                if (dist < minDist) {
                    minDist = dist;
                    selectRobot = robot;
                }
            }
            //判断是否是最近的去买的,因为贪心，可以让他卖了再买，如果当前有卖的且距离更近，直接赋值null
            for (Robot robot : robots) {
                if (robot.assigned && robot.carry) {
                    int dist = berths[robot.targetBerthId].getMinDistance(robot.pos);//去到距离
                    //来到距离
                    ArrayList<Point> candidates = berths[robot.targetBerthId].minDistancePos[robot.pos.x][robot.pos.y];
                    int addDist = Integer.MAX_VALUE;
                    for (Point candidate : candidates) {
                        int moveDistance = berths[robot.targetBerthId].dijkstras[candidate.x][candidate.y].getMoveDistance(buyWorkbench.pos);
                        if (moveDistance == Integer.MAX_VALUE) {
                            continue;
                        }
                        if (moveDistance < addDist) {
                            addDist = moveDistance;
                        }
                    }
                    if (addDist == Integer.MAX_VALUE) {
                        continue;
                    }
                    dist += addDist;
                    if (dist < minDist) { //此时另一个机器人卖完了再买，比选择的机器人直接去买更划算
                        selectRobot = null;
                        break;
                    }
                }
            }
            if (selectRobot == null) {
                continue;
            }
            if (minDist > buyWorkbench.remainTime) {
                continue;//去到货物就消失了,不去
            }

            int arriveBuyTime = buyWorkbench.getMinDistance(selectRobot.pos);


            double maxProfit = -GAME_FRAME;
            Berth selectSellBerth = null;
            for (Berth sellBerth : berths) {
                if (!sellBerth.canReach(buyWorkbench.pos)) {
                    continue; //不能到达
                }
                int arriveSellTime = sellBerth.getMinDistance(buyWorkbench.pos);//机器人买物品的位置开始
                int collectTime = getFastestCollectTime(arriveBuyTime + arriveSellTime, sellBerth);
                int sellTime = collectTime + sellBerth.transportTime;

                double profit;
                if (arriveSellTime + sellTime > GAME_FRAME) {
                    profit = -sellTime;//最近的去决策，万一到了之后能卖就ok，买的时候检测一下
                } else {
                    double value = buyWorkbench.value;
                    profit = value / (arriveSellTime + arriveBuyTime);
                    //考虑注释掉，可能没啥用，因为所有泊位都可以卖，可能就应该选最近的物品去买
                    if (selectRobot.targetWorkBenchId == buyWorkbench.id) {
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
            if (robot.assigned || robot.carry) {
                continue;
            }

            //选择折现价值最大的
            Berth select = null;
            double maxProfit = -GAME_FRAME;

            for (Berth sellBerth : berths) {
                if (!sellBerth.canReach(robot.pos)) {
                    continue; //不能到达
                }
                int fixTime = sellBerth.getMinDistance(robot.lastBuyPos); //机器人买物品的位置开始
                int arriveTime = sellBerth.getMinDistance(robot.pos);
                double profit;
                //包裹被揽收的最小需要的时间
                int collectTime = getFastestCollectTime(arriveTime - 1, sellBerth);
                int sellTime = collectTime + sellBerth.transportTime;
                if (frameId + sellTime > GAME_FRAME) {
                    //如果不能到达，收益为负到达时间
                    profit = -sellTime;
                } else {
                    double value = robot.carryValue;
                    //防止走的特别近马上切泊位了
                    profit = value / (arriveTime + fixTime);
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
        robot.targetBerthId = berth.id;
        if (action == RA_BUY) {
            workbenchesLock.add(workbench.id);//锁住，别人不准选择
            robot.targetWorkBenchId = workbench.id;
            robot.estimateUnloadTime = frameId + workbench.getMinDistance(robot.pos) + berth.getMinDistance(workbench.pos);
            assert workbench.getMinDistance(robot.pos) <= workbench.remainTime;
            robot.redundancy = workbench.getMinDistance(robot.pos) != workbench.remainTime;
        } else {
            robot.redundancy = true;
            robot.estimateUnloadTime = frameId + berth.getMinDistance(robot.pos);
        }
        if (action == RA_BUY && robot.pos.equal(workbench.pos)) {
            // 已经位于买工作台
            robot.buy();
        }
    }

    private int getFastestCollectTime(int goodArriveTime, Berth berth) {
        //这个泊位这个物品到达的时候泊位的数量
        int estimateGoodsNums = berth.goodsNums;
        for (Robot robot : robots) {
            if (robot.assigned && robot.targetBerthId == berth.id
                    && frameId + goodArriveTime > robot.estimateUnloadTime) {
                estimateGoodsNums++;
            }
        }
        estimateGoodsNums++;//加上这个货物时间

        //检查所有的船，找出这个泊位的这个物品最快被什么时候消费
        int remainGoods = estimateGoodsNums;
        class Pair implements Comparable<Pair> {
            final int first;//消费时间
            final int second;//船id

            public Pair(int first, int second) {
                this.first = first;
                this.second = second;
            }

            @Override
            public int compareTo(Pair o) {
                return Integer.compare(first, o.first);
            }
        }
        PriorityQueue<Pair> timePairs = new PriorityQueue<>();
        int[] boatNum = new int[boats.length];
        for (Boat boat : boats) {
            if (boat.targetId == berth.id) {
                timePairs.offer(new Pair(boat.remainTime, boat.id));
                boatNum[boat.id] = boat.capacity - boat.num;
            } else if (boat.targetId == -1) {
                timePairs.offer(new Pair(boat.remainTime + berth.transportTime, boat.id));
                boatNum[boat.id] = boat.capacity;
            } else {
                int loadTime = berths[boat.targetId].goodsNums / berths[boat.targetId].loadingSpeed;
                boatNum[boat.id] = max(0, boat.capacity - boat.num - berths[boat.targetId].goodsNums);
                if (boatNum[boat.id] > 0) {//有剩余空间
                    timePairs.offer(new Pair(boat.remainTime + loadTime + BERTH_CHANGE_TIME, boat.id));
                } else {//没剩余空间
                    //基本不会轮到？
                    boatNum[boat.id] = boat.capacity;
                    timePairs.offer(new Pair(boat.remainTime + loadTime
                            + berths[boat.targetId].transportTime
                            + berth.transportTime, boat.id));
                }
            }
        }
        int consumeTime;//这个货物被消费的最短时间，没加装货时间，这个是直接减的
        while (true) {
            Pair top = timePairs.poll();
            assert top != null;
            remainGoods -= boatNum[top.second];
            if (remainGoods <= 0) {
                consumeTime = top.first;
                break;
            }
            boatNum[top.second] = boats[top.second].capacity;
            timePairs.offer(new Pair(top.first + 2 * berth.transportTime, top.second));
        }

        //补充一点装货时间
        consumeTime += estimateGoodsNums / berth.transportTime;
        //没到达也没法消费
        return max(goodArriveTime, consumeTime);
    }

    private boolean input() throws IOException {
        String line = inStream.readLine();
        printMOST(line);
        if (line == null) {
            return false;
        }
        String[] parts = line.trim().split(" ");
        frameId = Integer.parseInt(parts[0]);
        money = Integer.parseInt(parts[1]);

        line = inStream.readLine();
        printMOST(line);
        parts = line.trim().split(" ");


        //新增工作台
        int num = Integer.parseInt(parts[0]);
        ArrayList<Integer> deleteIds = new ArrayList<>();//满足为0的删除
        for (Map.Entry<Integer, Workbench> entry : workbenches.entrySet()) {
            Workbench workbench = entry.getValue();
            workbench.remainTime--;
            if (workbench.remainTime == 0) {
                deleteIds.add(entry.getKey());
            }
        }
        for (Integer id : deleteIds) {
            workbenches.remove(id);
        }
        for (int i = 1; i <= num; i++) {
            Workbench workbench = new Workbench(workbenchId);
            workbench.input(gameMap);
            workbenches.put(workbenchId, workbench);
            workbenchId++;
        }

        //机器人
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            robots[i].input();
        }
        //船
        for (int i = 0; i < BOATS_PER_PLAYER; i++) {
            boats[i].input();
            if (boats[i].remainTime != 0) {
                boats[i].remainTime--;
            }
        }
        String okk = inStream.readLine();
        printMOST(okk);
        return true;
    }
}
