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

    public final Berth[] berths = new Berth[BERTH_PER_PLAYER];

    public final Boat[] boats = new Boat[BOATS_PER_PLAYER];

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
            berths[id].leftTopPos.x = Integer.parseInt(parts[1]);
            berths[id].leftTopPos.y = Integer.parseInt(parts[2]);
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
        while (greedyBuy()) ;


        //todo 选择路径，修复路径，碰撞避免

        //todo 船只选择去哪个泊位


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
                //int arriveTime = getArriveTime(sellWorkBench, STAT_CARRY_ROBOT, m_workbench.get(robot.lastBuyId).pos); //机器人买物品的位置开始
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
        } else {
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
                    boatNum[boat.id] = boat.capacity;
                    timePairs.offer(new Pair(boat.remainTime + loadTime + 2 * berth.transportTime, boat.id));
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
        }
        String okk = inStream.readLine();
        printMOST(okk);
        return true;
    }
}
