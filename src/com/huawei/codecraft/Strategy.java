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
    public int totalValue = 0;
    public int goodAvgValue = 0;

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
        }
        //机器人
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            robots[i] = new Robot(this);
            robots[i].id = i;
            robotLock[i] = new HashSet<>();
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
        while (greedySell()) ; //决策
        workbenchesLock.clear();//动态需要解锁
        for (HashSet<Integer> set : robotLock) {
            set.clear();
        }
        while (greedyBuy()) ;

        robotDoAction();


        doBoatAction();
        //todo 测试机器人能不能取货立即装货
//        if (frameId < 50) {
//            Dijkstra dijkstra = new Dijkstra();
//            dijkstra.init(new Point(14, 189), gameMap);
//            dijkstra.update();
//            robots[8].path.addAll(dijkstra.moveFrom(robots[8].pos));
//            robots[8].finish();
//        }
//        if (frameId == 50) {
//            outStream.printf("get %d\n", 8);
//        }
//        if (frameId >= 50 && frameId < 100) {
//            Dijkstra dijkstra = new Dijkstra();
//            dijkstra.init(new Point(4, 174), gameMap);
//            dijkstra.update();
//            robots[8].path.addAll(dijkstra.moveFrom(robots[8].pos));
//            robots[8].finish();
//        }
//        if (frameId == 1) {//开船
//            //开船过来
//            boats[0].ship(9);
////            outStream.printf("ship %d %d", 1, 9);
//        }
//        if (frameId == 1 + 1219 + 10) {//卸货
//            outStream.printf("move %d %d\n", 8, 1);
//            outStream.printf("pull %d\n", 8);
//        }
//
//        if (frameId == 1 + 1219 + 6) {
//            boats[0].ship(9);
//        }
//        if (frameId == 1 + 1219 + 20) {
//            boats[0].go();
//        }
//        if (frameId == 1 + 1219 + 20 + 1219) {
//            boats[0].ship(9);
//        }
//        if (frameId == 1 + 1219 + +20 + 1219 + 1219 + 1) {
//            boats[0].go();
//        }
//        if (frameId == 2) {
//            //开船过来
//            outStream.printf("ship %d %d", 0, 9);
////            outStream.printf("ship %d %d", 1, 9);
//        }
//        if (frameId == 1 + 1219 + 2) {//到达之后
//            //1220帧到达，然后开始装货，。，至少到达一帧才能走
//            outStream.printf("ship %d %d", 0, 9);
//        }


    }

    private void doBoatAction() {
        //船只选择回家
        for (Boat boat : boats) {
            if (boat.targetId != -1 && boat.num == boat.capacity) {//满了回家
                boat.go();
                boat.remainTime = berths[boat.targetId].transportTime;
                boat.status = 0;//移动中
                boat.targetId = -1;
                boat.assigned = true;
            } else if (boat.num > 0 && boat.targetId != -1) {
                int minSellTime;
                if (boat.status != 0) {
                    //说明到达目标了
                    minSellTime = berths[boat.targetId].transportTime;
                } else {
                    //没到目标,如果是船运过来，num一定为0
                    minSellTime = berths[boat.lastTargetId].transportTime;
                }
                if (frameId + minSellTime >= GAME_FRAME) {
                    //极限卖
                    boat.go();
                    boat.remainTime = minSellTime;
                    boat.status = 0;//移动中
                    boat.targetId = -1;
                    boat.assigned = true;
                }
            } else if (boat.targetId == -1 && boat.status == 0) {
                boat.assigned = true;//运输中
            }
        }
        //船只贪心去买卖，跟机器人做一样的决策
        //先保存一下泊位的价值列表
        //1当前价值列表+机器人运过来的未来价值列表
        @SuppressWarnings("unchecked")
        LinkedList<Integer>[] goodsList = new LinkedList[BERTH_PER_PLAYER];
        for (int i = 0; i < goodsList.length; i++) {
            goodsList[i] = new LinkedList<>();
            goodsList[i].addAll(berths[i].goods);
        }

        //未来价值列表，直接加进去
        for (Robot robot : robots) {
            if (!robot.assigned) {
                continue;
            }
            if (robot.carry) {
                goodsList[robot.targetBerthId].offer(robot.carryValue);
            } else {
                goodsList[robot.targetBerthId].offer(workbenches.get(robot.targetWorkBenchId).value);
            }
        }
        //计算前缀和
        @SuppressWarnings("unchecked")
        ArrayList<Integer>[] prefixSum = new ArrayList[BERTH_PER_PLAYER];
        for (int i = 0; i < BERTH_PER_PLAYER; i++) {
            prefixSum[i] = new ArrayList<>();
            prefixSum[i].add(0);
            for (Integer value : goodsList[i]) {
                prefixSum[i].add(prefixSum[i].get(prefixSum[i].size() - 1) + value);
            }
        }


        //贪心切换找一个泊位
        while (boatGreedyBuy(goodsList, prefixSum)) ;

        //装载货物
        for (Boat boat : boats) {
            //移动立即生效
            if (boat.remainTime > 0) {
                boat.remainTime--;
            }
            //移动完之后的下一帧才能开始装货
            if (boat.status == 1 && boat.targetId != -1 && berths[boat.targetId].goodsNums > 0) {
                //正常运行,开始装货
                assert boat.remainTime == 0;
                int load = min(berths[boat.targetId].loadingSpeed, berths[boat.targetId].goodsNums);
                load = min(load, boat.capacity - boat.num);//剩余空间
                boat.num += load;
                berths[boat.targetId].goodsNums -= load;
                for (int i = 0; i < load; i++) {
                    assert !berths[boat.targetId].goods.isEmpty();
                    Integer value = berths[boat.targetId].goods.poll();
                    assert value != null;
                    boat.value += value;
                    berths[boat.targetId].totalValue -= value;
                }
            }
        }
    }

    private boolean boatGreedyBuy(LinkedList<Integer>[] goodsList, ArrayList<Integer>[] prefixSum) {
        class Stat implements Comparable<Stat> {
            final Boat boat;
            final Berth berth;
            final int count;//消费个数
            final double profit;

            public Stat(Boat boat, Berth berth, int count, double profit) {
                this.boat = boat;
                this.berth = berth;
                this.count = count;
                this.profit = profit;
            }

            @Override
            public int compareTo(Stat b) {
                return Double.compare(b.profit, profit);
            }
        }
        //todo 切到一半不能反悔
        //考虑在移动，运输中也算不能返回，移动完才可以动？
        int[] berthBoatCount = new int[BERTH_PER_PLAYER];
        Arrays.fill(berthBoatCount, -1);
        for (Boat boat : boats) {
            if (boat.assigned && boat.targetId != -1) {
                berthBoatCount[boat.targetId]++;
            }
        }


        ArrayList<Stat> stat = new ArrayList<>();
        //选择折现价值最大的
        for (Berth buyBerth : berths) {
            //存在就一定有产品
            //贪心，选择最近的机器人
            ArrayList<Boat> selectBoats = new ArrayList<>();
            int minDist = Integer.MAX_VALUE;
            for (Boat boat : boats) {
                if (boat.assigned || (boat.lastTargetId == buyBerth.id && boat.status == 0)) {
                    continue;
                }
                int dist;
                if (boat.targetId == buyBerth.id) {
                    dist = boat.remainTime;
                    if (boat.status == 1) {
                        dist = 1;//泊位外等待为1
                    }
                } else if (boat.lastTargetId != -1) {
                    //切换泊位移动中，或者在另一个泊位等待或者进入
                    dist = BERTH_CHANGE_TIME;
                } else {
                    dist = buyBerth.transportTime;//虚拟点到泊位时间
                }
                if (dist < minDist) {
                    minDist = dist;
                    selectBoats.clear();
                    selectBoats.add(boat);
                } else if (dist == minDist) {
                    selectBoats.add(boat);
                }
            }
            //判断是否是最近的去买的,因为贪心，可以让他卖了再买，如果当前有卖的且距离更近，直接赋值null
            for (Boat boat : boats) {
                //在卖
                if (boat.assigned && boat.targetId == -1 && boat.status == 0) {
                    int dist = boat.remainTime + buyBerth.transportTime;//去到距离
                    //来到距离

                    if (dist < minDist) { //此时另一个机器人卖完了再买，比选择的机器人直接去买更划算
                        selectBoats.clear();
                        break;
                    }
                }
            }
            if (selectBoats.isEmpty()) {
                continue;
            }

            //考虑优先选择没在运行中的船?
            if (minDist == BERTH_CHANGE_TIME) {
                //在切换中的船换不如到达泊位的换，因为可能他到达之后就有货物了
                ArrayList<Boat> noRunningBoats = new ArrayList<>();
                for (Boat selectBoat : selectBoats) {
                    if (selectBoat.status != 0) {
                        noRunningBoats.add(selectBoat);
                    }
                }
                if (noRunningBoats.isEmpty()) {
                    selectBoats.clear();
                    selectBoats.addAll(noRunningBoats);
                }
            }

            int buyTime = minDist;
            int sellTime = buyBerth.transportTime;
            for (Boat boat : selectBoats) {
                double profit;
                int maxLoadCount = 0;
                if (frameId + buyTime + sellTime >= GAME_FRAME) {
                    profit = -buyTime;
                } else {
                    //估计未来货物个数，计算装货时间
                    maxLoadCount = min(boat.capacity - boat.num, goodsList[buyBerth.id].size());
                    maxLoadCount = min((GAME_FRAME - buyTime - sellTime) * buyBerth.loadingSpeed, maxLoadCount);
                    double value = prefixSum[buyBerth.id].get(maxLoadCount);
                    //消除价值
                    if (frameId + buyTime + sellTime + 2 * sellTime < GAME_FRAME) {
                        //买了再卖，再买，在卖，此时存在消除价值,说明携带越贵物品的船优先级最高，感觉数量更好？
                        value += goodAvgValue * boat.num;//数量越多，消除价值越高
                    }
                    if (berthBoatCount[buyBerth.id] >= 1) {
                        value -= 1;//稍微降低点价值，让最开始能分散开来
                    }
                    int loadTime = (int) floor(maxLoadCount * 1.0 / buyBerth.loadingSpeed);
                    profit = value / (buyTime + sellTime + loadTime);//买的时间卖的时间，和最后的时间
                    //相同泊位增加一下价值？
                    if (boat.targetId == buyBerth.id) {
                        profit *= (1 + SAME_TARGET_REWARD_FACTOR);
                    }

                }
                stat.add(new Stat(boat, buyBerth, maxLoadCount, profit));
            }
        }
        if (stat.isEmpty())
            return false;
        Collections.sort(stat);
        //同一个目标不用管
        Berth berth = stat.get(0).berth;
        Boat boat = stat.get(0).boat;
        //boat去移动
        if (boat.targetId != berth.id) {
            boat.ship(berth.id);
            if (boat.targetId == -1) {
                //在虚拟点
                assert boat.status == 1;
                boat.remainTime = berth.transportTime;
            } else {
                if (boat.status != 0 || boat.lastTargetId != -1) {
                    //在已经到达泊位,或者切换泊位（上一个目标不是虚拟点），一般情况下都是虚拟点
                    boat.remainTime = BERTH_CHANGE_TIME;
                } else {
                    //在从虚拟点移动到泊位或者
                    boat.remainTime = berth.transportTime;
                }
            }
            boat.targetId = berth.id;
            boat.status = 0;
        }
        boat.assigned = true;
        //更新泊位的货物列表和前缀和,说明这个船消耗了这么多货物
        int count = stat.get(0).count;
        for (int i = 0; i < count; i++) {
            goodsList[berth.id].poll();
        }
        prefixSum[berth.id].clear();
        prefixSum[berth.id].add(0);
        for (int value : goodsList[berth.id]) {
            prefixSum[berth.id].add(prefixSum[berth.id].get(prefixSum[berth.id].size() - 1) + value);
        }
        return true;
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

                for (int j = 1; j < candidate.size(); j++) {
                    Point point = candidate.get(j);
                    ArrayList<Point> candidatePath = berths[robot.targetBerthId].dijkstras[point.x][point.y].moveFrom(robot.pos);
                    if (!checkCrash(candidatePath, robot.id)) {
                        path = candidatePath;
                        break;
                    } else {
                        //尝试找不撞的
                        heuristicPoints = berths[robot.targetBerthId].dijkstras[candidate.get(j).x][candidate.get(j).y].cs;
                        boolean result = findABestPath(candidatePath, heuristicPoints, robot.id);
                        if (result) {
                            path = candidatePath;
                            break;
                        }
                    }
                }
                heuristicPoints = berths[robot.targetBerthId].dijkstras[candidate.get(0).x][candidate.get(0).y].cs;
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
//                assert robot.path.size() >= 3;//包含起始点
                if (robot.path.size() == 1) {
                    //原点不动，大概率有漏洞
                    robotsPredictPath[robot.id].add(robot.path.get(0));
                    robotsPredictPath[robot.id].add(robot.path.get(0));
                    printERROR("error robot.path.size()==1");
                }
                //至少有未来一个格子
                for (int j = 1; j <= min(4, robot.path.size() - 1); j++) {
                    robotsPredictPath[robot.id].add(robot.path.get(j));
                }
            }
            int crashId = -1;
            for (int j = 0; j < i; j++) {
                //一个格子之内撞不撞
                for (int k = 0; k < 2; k++) {
                    if (robotsPredictPath[robot.id].get(k).equal(robotsPredictPath[tmpRobots[j].id].get(k))) {
                        crashId = tmpRobots[j].id;
                        break;
                    }
                }
                if (crashId != -1) {
                    break;
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
                Point result = new Point(-1, -1);
                int bestDist = Integer.MAX_VALUE;
                HashSet<Integer> crashIds = new HashSet<>();
                for (Point candidate : candidates) {
                    if (!gameMap.canReach(candidate.x, candidate.y)) {
                        continue;
                    }
                    //todo 检查是否会撞到任意一个其他人,会的话也不是候选点
                    //细化成两个去判断
                    boolean crash = false;
                    for (Robot tmpRobot : tmpRobots) {
                        if (tmpRobot.id == avoidId || robotsPredictPath[tmpRobot.id] == null) {
                            continue;//后面的
                        }
                        Point start = gameMap.posToDiscrete(robots[avoidId].pos);
                        Point end = gameMap.posToDiscrete(candidate);
                        Point mid = start.add(end).div(2);
                        if (mid.equal(robotsPredictPath[tmpRobot.id].get(0)) || end.equal(robotsPredictPath[tmpRobot.id].get(1))) {
                            crashIds.add(tmpRobot.id);
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
                        dist = berths[robots[avoidId].targetBerthId].getMinDistance(candidate);
                    }
                    assert dist != Integer.MAX_VALUE;
                    if (robotsPredictPath[crashId].size() == 4 && candidate.equal
                            (gameMap.discreteToPos(robotsPredictPath[crashId].get(3)))) {
                        dist += 2;//不在对面路径上认为更好一点
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
                Point start = robots[i].pos;
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
                    continue; // 下个点不可达
                }
                if (!gameMap.canReachDiscrete(dx + dir.x, dy + dir.y) || discreteCs[dx + dir.x][dy + dir.y] != Integer.MAX_VALUE) {
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
        for (Robot robot : robots) {
            if (!robot.assigned || robot.id == robotId) {
                continue;
            }
            if (deep > robot.path.size() - 1) {
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
                if (!buyWorkbench.canReach(robot.pos)) {
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

                    //消除价值计算,至少要一来一会才会出现消除价值
                    value += estimateEraseValue(arriveSellTime + sellTime, selectRobot, sellBerth, goodAvgValue * 2);

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


    private double estimateEraseValue(int beginFrame, Robot robot, Berth berth, int basicValue) {
        if (beginFrame + 3 * berth.transportTime > GAME_FRAME) {
            return 0;
        }
        //基本价值越高，机器人越容易打配合
        int berthTotalNum = berth.goodsNums;
        int remainSpace = 0;
        for (Boat boat : boats) {
            if (boat.status == 1 && boat.targetId == berth.id) {
                remainSpace = boat.capacity - boat.num;
                break;
            }
        }
        if (remainSpace == 0) {
            return 0;
        }
        for (Robot other : robots) {
            if (other.id == robot.id || !other.assigned) {
                continue;
            }
            if (other.targetBerthId == berth.id) {
                berthTotalNum++;
            }
        }
        int needNum = remainSpace - berthTotalNum;
        if (needNum <= 0) {
            return 0;//不用消除，还有更多
        }
        //需要的越少，消除价值越高
        return basicValue * 1.0 / needNum;
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
                if (!sellBerth.canReach(robot.pos)) {
                    continue; //不能到达
                }
                int fixTime = sellBerth.getMinDistance(robot.lastBuyPos); //机器人买物品的位置开始
                //assert fixTime != Integer.MAX_VALUE;
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
                    value += estimateEraseValue(sellTime, robot, sellBerth, goodAvgValue * 2);
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
        consumeTime += estimateGoodsNums / berth.loadingSpeed;
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
            totalValue += workbench.value;
            workbenches.put(workbenchId, workbench);
            workbenchId++;
            goodAvgValue = totalValue / workbenchId;
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
