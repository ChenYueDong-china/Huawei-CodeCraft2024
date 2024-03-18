package com.huawei.codecraft;

import java.io.IOException;
import java.util.*;

import static com.huawei.codecraft.BoatDecisionType.DECISION_ON_ORIGIN;
import static com.huawei.codecraft.BoatDecisionType.DECISION_ON_ORIGIN_BERTH;
import static com.huawei.codecraft.BoatStatus.*;
import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.RobotAction.RA_BUY;
import static com.huawei.codecraft.RobotAction.RA_SELL;
import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.*;

public class Strategy {


    public int pullScore;//船把所有货物都收了的极限分
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
    public int remainBerthsAvgValue = 0;//没被船只选择的泊位的平均价值，用来计算消除价值
    public int totalPullGoodsValues = 0;//总的卖的货物价值
    public int totalPullGoodsCount = 0;//总的卖的货物价值
    public double avgPullGoodsValue = 150;//总的卖的货物价值
    public double avgPullGoodsTime = 15;//10个泊位平均卖货时间

    @SuppressWarnings("unchecked")
    public ArrayList<Point>[] robotsPredictPath = new ArrayList[ROBOTS_PER_PLAYER];

    public int curFrameDiff = 1;

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

    int count = 0;

    public void mainLoop() throws IOException {


        while (input()) {
            long l = System.currentTimeMillis();
            dispatch();
            long e = System.currentTimeMillis();
            if (e - l > 15) {
                printERROR("frameId:" + frameId + ",time:" + (e - l));
            }
            if (frameId == 15000) {
                printERROR("跳帧：" + count + ",pullScore:" + pullScore + ",boatWaitTime:" + boatWaitTime);
            }
            outStream.print("OK\n");
            outStream.flush();
        }


    }

    long time1, time2, time3;

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
                boat.estimateComingBerthId = -1;
                boat.exactStatus = IN_ORIGIN_TO_BERTH;
            } else if (boat.num > 0 && boat.targetId != -1) {
                int minSellTime;
                if (boat.status != 0) {
                    //说明到达目标了
                    minSellTime = berths[boat.targetId].transportTime;
                } else {
                    //没到目标,如果是船运过来，num一定为0
                    minSellTime = berths[boat.lastArriveTargetId].transportTime;
                }
                if (frameId + minSellTime >= GAME_FRAME - FPS / 5) {//会掉帧，预留10帧
                    //极限卖
                    boat.go();
                    boat.remainTime = minSellTime;
                    boat.status = 0;//移动中
                    boat.targetId = -1;
                    boat.assigned = true;
                    boat.estimateComingBerthId = -1;
                    boat.exactStatus = IN_ORIGIN_TO_BERTH;
                }
            }
        }
        //船只贪心去买卖，跟机器人做一样的决策
        //先保存一下泊位的价值列表
        //1当前价值列表+机器人运过来的未来价值列表
        @SuppressWarnings("unchecked")
        int[] goodsNumList = new int[BERTH_PER_PLAYER];
        for (int i = 0; i < goodsNumList.length; i++) {
            goodsNumList[i] = berths[i].goodsNums;
        }
        for (Robot robot : robots) {
            if (!robot.assigned) {
                continue;
            }
            if (robot.targetBerthId != -1) {
                goodsNumList[robot.targetBerthId]++;
            }
        }

        int[] goodComingTimes = new int[BERTH_PER_PLAYER];//货物开始来泊位的最早时间
        Arrays.fill(goodComingTimes, 0);//这一帧开始到来
        //贪心切换找一个泊位
//        while (boatGreedyBuy(goodsNumList, goodComingTimes)) ;
        while (boatGreedyBuy2(goodsNumList, goodComingTimes)) ;


        //装载货物
        for (Boat boat : boats) {
            //移动立即生效
            if (boat.remainTime > 0) {
                boat.remainTime--;
            }
            //移动完之后的下一帧才能开始装货
            if (boat.status == 1 && boat.targetId != -1 && berths[boat.targetId].goodsNums > 0) {
                //正常运行,开始装货，如果跳帧，什么时候到达已经不知道了
                //如果remainTime不为0，说明中间跳帧了，多装几次货就好了
                for (int i = 0; i < (boat.remainTime + 1); i++) {
                    int load = min(berths[boat.targetId].loadingSpeed, berths[boat.targetId].goodsNums);
                    load = min(load, boat.capacity - boat.num);//剩余空间
                    boat.num += load;
                    berths[boat.targetId].goodsNums -= load;
                    for (int j = 0; j < load; j++) {
                        assert !berths[boat.targetId].goods.isEmpty();
                        Integer value = berths[boat.targetId].goods.poll();
                        assert value != null;
                        boat.value += value;
                        berths[boat.targetId].totalValue -= value;
                    }
                }
                boat.remainTime = 0;
            }
        }


        //更新泊位到来船只状态，平均剩余泊位价值
        int totalValue = 0;
        int remainCount = 0;
        for (Berth berth : berths) {
            berth.comingBoats.clear();
            for (Boat boat : boats) {
                if (boat.estimateComingBerthId == berth.id) {
                    berth.comingBoats.offer(boat.id);
                }
            }
            if (berth.comingBoats.isEmpty()) {
                totalValue += berth.totalValue;
                remainCount++;
            }
        }
        assert remainCount != 0;
        remainBerthsAvgValue = totalValue / remainCount;
        //commingboats按照到来时间排个序,此时已经保证了每个船都有一个目标泊位。
        for (Berth berth : berths) {
            class Pair {
                final int remainTime;
                final int boatId;

                public Pair(int remainTime, int boatId) {
                    this.remainTime = remainTime;
                    this.boatId = boatId;
                }
            }
            ArrayList<Pair> tmp = new ArrayList<>(berth.comingBoats.size());
            for (int id : berth.comingBoats) {
                //估计到达时间
                if (boats[id].targetId == berth.id) {
                    tmp.add(new Pair(boats[id].remainTime, id));
                } else {
                    //估计的
                    assert boats[id].targetId == -1;
                    tmp.add(new Pair(boats[id].remainTime + berth.transportTime, id));
                }
            }
            //时间早的先
            tmp.sort(Comparator.comparingInt(o -> o.remainTime));
            berth.comingBoats.clear();
            for (Pair pair : tmp) {
                boats[pair.boatId].estimateComingBerthId = berth.id;
                berth.comingBoats.offer(pair.boatId);
            }

        }

    }

    private boolean boatGreedyBuy2(int[] goodsNumList, int[] goodComingTimes) {
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
        double goodsComingSpeed = avgPullGoodsTime * BERTH_PER_PLAYER;
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
            int needCount = boat.targetId == -1 ? boat.capacity : boat.capacity - boat.num;
            if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingSpeed, needCount))
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
            //只能装那么多，尽量去装吧
            double profit = 1e10 + goodsNumList[buyBerth.id] * 1e10 - goodComingTimes[buyBerth.id] * 1e5 + minDistance;
            int totalWaitTime = goodComingTimes[buyBerth.id] +
                    (int) ceil(max(0, selectBoat.capacity - goodsNumList[buyBerth.id])
                            * goodsComingSpeed);
            stat.add(new Stat(selectBoat, buyBerth, min(goodsNumList[buyBerth.id], selectBoat.capacity)
                    , totalWaitTime, profit));
        }
        if (!stat.isEmpty()) {
            Collections.sort(stat);
            //同一个目标不用管
            boatRealDecision(goodsNumList, goodComingTimes, stat.get(0).berth
                    , stat.get(0).boat, stat.get(0).updateTime, stat.get(0).count);
            return true;
        }

        //第三种，在泊位上或者等待的,货物数量和,单位时间价值
        for (Boat boat : threeTypesBoats) {
            //到每个泊位距离一定是500
            int needCount = boat.capacity - boat.num;
            if (boatDecisionType == DECISION_ON_ORIGIN) {
                //不可以切换泊位,
                assert boat.estimateComingBerthId == boat.targetId;
                if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingSpeed, needCount))
                    return true;
            }
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
                realCount = min(realCount, goodsNumList[berth.id] + (int) (remainTime / goodsComingSpeed));//来货
                int loadTime = (int) ceil(1.0 * realCount / berth.loadingSpeed);
                int totalWaitTime = goodComingTimes[berth.id] + (int) ceil(max(0, realCount - goodsNumList[berth.id]) * goodsComingSpeed);
                int arriveWaitTime = max(0, totalWaitTime - buyTime);//到达之后的等待时间
                if (berth.id != boat.targetId && arriveWaitTime > 0) {
                    continue;//不是同一个泊位，且到达之后需要等待，那么先不去先
                }
                double profit = 1.0 * realCount / (buyTime + max(arriveWaitTime, loadTime) + sellTime);
                //增益超过保持因子，则切换目标
                stat.add(new Stat(boat, berth, min(realCount, goodsNumList[berth.id]), totalWaitTime, profit));
            }
        }
        if (stat.isEmpty()) {
            return false;
        }
        Collections.sort(stat);
        //同一个目标不用管
        boatRealDecision(goodsNumList, goodComingTimes, stat.get(0).berth
                , stat.get(0).boat, stat.get(0).updateTime, stat.get(0).count);
        return true;

    }

    private static void boatRealDecision(int[] goodsNumList, int[] goodComingTimes, Berth berth, Boat boat, int updateTime, int count) {
        goodsNumList[berth.id] -= count;
        goodComingTimes[berth.id] = updateTime;
        //boat去移动
        if (boat.targetId != berth.id && (!(boat.status == 0 && boat.targetId == -1))) {
            boat.ship(berth.id);//不在往原点运输中
            if (boat.lastArriveTargetId == -1) {
                //在虚拟点，或者虚拟点到泊位
                boat.remainTime = berth.transportTime;
            } else {
                boat.remainTime = BERTH_CHANGE_TIME;
            }
            //printERROR("boat:" + boat.id + ",原来目标:" + boat.targetId + ",现在目标:" + berth.id + ",time:" + boat.remainTime);
            boat.targetId = berth.id;
            boat.status = 0;
        }
        boat.assigned = true;
        //更新泊位的货物列表和前缀和,说明这个船消耗了这么多货物
        boat.estimateComingBerthId = berth.id;
    }


    static class BoatProfitAndCount {
        double profit;
        int count;

        int updateStartTime;

        public BoatProfitAndCount(double profit, int count, int updateStartTime) {
            this.profit = profit;
            this.count = count;
            this.updateStartTime = updateStartTime;
        }
    }

//    private boolean boatGreedyBuy(int[] goodsNumList, int[] goodComingTimes) {
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
//        double goodsComingSpeed = avgPullGoodsTime * BERTH_PER_PLAYER;
//        for (Boat boat : boats) {
//            if (boat.assigned) {
//                continue;
//            }
//            int needCount = boat.targetId == -1 ? boat.capacity : boat.capacity - boat.num;
//            if (boatDecisionType == DECISION_ON_ORIGIN
//                    && (boat.exactStatus != IN_ORIGIN_POINT)) {
//                //只要现在不在虚拟点,都认为赋值了
//                if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingSpeed, needCount))
//                    return true;
//
//            }
//            if (boatDecisionType == DECISION_ON_ORIGIN_BERTH
//                    && (boat.exactStatus != IN_ORIGIN_POINT//虚拟点
//                    && boat.exactStatus != IN_BERTH_INTER//泊位内
//                    && boat.exactStatus != IN_BERTH_WAIT && boat.targetId != -1)) {//泊位外等待
//                //只要在移动过程中，都认为不可以切换，只有在泊位上，或者在虚拟点，才开始重新决策
//                if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingSpeed, needCount))
//                    return true;
//            }
//            //todo 在泊位上，且有物品没消耗，则不换泊位
//            if (boat.targetId != -1 && boat.status == 1 && berths[boat.targetId].goodsNums != 0) {
//                assert boat.targetId == boat.estimateComingBerthId;
//                if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingSpeed, needCount))
//                    return true;
//            }
//
//
//            if (boat.targetId != -1 && boat.status == 1) {
//                //有泊位到达之后可以不等待，则放开限制,否则直接赋值原来泊位
//                int minArriveWaitTime = Integer.MAX_VALUE;
//                for (Berth berth : berths) {
//                    int arriveTime = getBoatToBerthDistance(berth, boat);
//                    int sellTime = berth.transportTime;
//                    if (frameId + arriveTime + sellTime >= GAME_FRAME) {
//                        continue;
//                    }
//                    int myArriveCount = goodsNumList[berth.id] + (int) floor(max(0, arriveTime - goodComingTimes[berth.id]) / goodsComingSpeed);
//                    boolean canChange = true;
//                    //是否有别人正在去
//                    if (berth.id != boat.targetId) {
//                        canChange = getCanChangeBerth(goodsNumList, goodComingTimes, goodsComingSpeed, berth, boat, needCount, myArriveCount);
//                    }
//                    if (!canChange) {
//                        continue;
//                    }
//                    int remainTime = GAME_FRAME - arriveTime - sellTime - frameId;
//                    int realCount = min(needCount, remainTime / berth.loadingSpeed);//装货
//                    realCount = min(realCount, goodsNumList[berth.id] + (int) (remainTime / goodsComingSpeed));//来货
//                    int totalWaitTime = goodComingTimes[berth.id] + (int) ceil(max(0, realCount - goodsNumList[berth.id]) * goodsComingSpeed);
//                    int arriveWaitTime = max(0, totalWaitTime - arriveTime);//到达之后的等待时间
//                    minArriveWaitTime = min(arriveWaitTime, minArriveWaitTime);
//                }
//                if (minArriveWaitTime > 0) {
//                    //这种时候等待是更好的，直到别的泊位我去到就满
//                    assert boat.targetId == boat.estimateComingBerthId;
//                    if (updateSumTimes(goodsNumList, goodComingTimes, boat, goodsComingSpeed, needCount))
//                        return true;
//                }
//            }
//        }
//        //考虑在移动，运输中也算不能返回，移动完才可以动？
//        ArrayList<Stat> stat = new ArrayList<>();
//        //选择折现价值最大的
//        for (Berth buyBerth : berths) {
//            //存在就一定有产品
//            //贪心，选择最近的机器人
//
//            //选择最近的，如果有多个最近的，选择中途的和离起始点最近的，包括在起始点的
//            ArrayList<Boat> selectBoats = new ArrayList<>();
//            int minDistance = Integer.MAX_VALUE;
//            for (Boat boat : boats) {
//                if (boat.assigned || (boat.lastArriveTargetId == buyBerth.id && boat.status == 0 && boat.targetId != -1)) {
//                    continue;
//                }
//                selectBoats.add(boat);
//            }
//            if (selectBoats.isEmpty()) {
//                continue;
//            }
//
//            int sellTime = buyBerth.transportTime;
//            for (Boat boat : selectBoats) {
//                int arriveTime = getBoatToBerthDistance(buyBerth, boat);
//                int toOriginTime = boat.targetId == -1 ? boat.remainTime : 0;
//                int buyTime = arriveTime - toOriginTime;
//                if (frameId + arriveTime + sellTime >= GAME_FRAME) {
//                    continue;
//                }
//                int needCount = boat.targetId == -1 ? boat.capacity : boat.capacity - boat.num;
//                //是否有别人正在去
//                if (boat.targetId != -1 && boat.status == 1 && buyBerth.id != boat.targetId) {
//                    //切换泊位
//                    int myArriveCount = goodsNumList[buyBerth.id] + (int) floor(max(0, arriveTime - goodComingTimes[buyBerth.id]) / goodsComingSpeed);
//                    boolean canChange = getCanChangeBerth(goodsNumList, goodComingTimes, goodsComingSpeed, buyBerth, boat, needCount, myArriveCount);
//                    if (!canChange) {
//                        continue;
//                    }
//                }
//                int remainTime = GAME_FRAME - arriveTime - sellTime - frameId;
//                int realCount = min(needCount, remainTime / buyBerth.loadingSpeed);//装货
//                realCount = min(realCount, goodsNumList[buyBerth.id] + (int) (remainTime / goodsComingSpeed));//来货
//                int loadTime = (int) ceil(1.0 * realCount / buyBerth.loadingSpeed);
//                int totalWaitTime = goodComingTimes[buyBerth.id] + (int) ceil(max(0, realCount - goodsNumList[buyBerth.id]) * goodsComingSpeed);
//                int arriveWaitTime = max(0, totalWaitTime - arriveTime);//到达之后的等待时间
//                int punishTime = getPunishTime(boat, buyBerth);
//                double profit = realCount * 1.0 / (buyTime + max(arriveWaitTime, loadTime) + sellTime
//                        + BOAT_PUNISH_FACTOR * (punishTime + arriveWaitTime) + toOriginTime * 1e-8);
//                //增益超过保持因子，则切换目标
//                stat.add(new Stat(boat, buyBerth, min(realCount, goodsNumList[buyBerth.id]), totalWaitTime, profit));
//            }
//
//
//        }
//
//
//        if (stat.isEmpty())
//            return false;
//        Collections.sort(stat);
//        //同一个目标不用管
//        boatRealDecision(goodsNumList, goodComingTimes, stat.get(0).berth
//                , stat.get(0).boat, stat.get(0).updateTime, stat.get(0).count);
//        return true;
//    }

    private boolean getCanChangeBerth(int[] goodsNumList, int[] goodComingTimes, double goodsComingSpeed, Berth buyBerth, Boat boat, int needCount, int myArriveCount) {
        for (Boat other : boats) {
            if (boat.id == other.id) {
                continue;
            }
            if (!other.assigned && other.estimateComingBerthId == buyBerth.id && other.status != 1) {
                int otherNeedCount = other.targetId != -1 ? other.capacity - other.num : other.capacity;
                int otherArriveCount = goodsNumList[buyBerth.id] + (int) floor(max(0, other.remainTime - goodComingTimes[buyBerth.id]) / goodsComingSpeed);
                if (otherNeedCount + needCount > max(myArriveCount, otherArriveCount)) {
                    //两艘船需要的大于已有的，结束
                    return false;
                }
            }
        }
        return true;
    }

    private int getPunishTime(Boat boat, Berth berth) {
        int punishTime = 0;
        //在行驶过程中
        if (boat.status == 0 && boat.targetId != -1 && boat.targetId != berth.id) {
            if (boat.lastArriveTargetId == -1) {
                punishTime = berths[boat.targetId].transportTime - boat.remainTime;
            } else {
                //在泊位切换过程中
                punishTime = BERTH_CHANGE_TIME - boat.remainTime;
            }
            assert punishTime >= 0;
        }
        //在泊位上等待
        if (boat.status == 1 && boat.targetId != -1 && boat.targetId != berth.id) {
            //切换泊位有惩罚
            punishTime = BERTH_CHANGE_TIME + berth.transportTime - berths[boat.targetId].transportTime;
            punishTime = max(0, punishTime);
        }
        return punishTime;
    }

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


    BoatProfitAndCount estimateBoatProfitAndCount(Boat boat, Berth buyBerth, int buyTime, int sellTime, Queue<Integer>[] goodsList
            , ArrayList<Integer>[] prefixSum, int startComingTime) {
        double profit;
        int loadCount = 0;
        int updateStartComingTime = startComingTime;
        if (frameId + buyTime + sellTime >= GAME_FRAME) {
            profit = -1;
            updateStartComingTime = GAME_FRAME + 1;
        } else {
            //分两部分，到达前，到达后
            int needCount = boat.capacity - boat.num;
            if (boat.status == 0 && boat.targetId == -1) {
                needCount = boat.capacity;
            }
            int loadTime;
            double value = 0;
            double comingSpeed = avgPullGoodsTime * getCanReachBerthsCount();
//            double comingSpeed = avgPullGoodsTime * BERTH_PER_PLAYER;
            if (goodsList[buyBerth.id].size() >= needCount) {
                //不更新到达时间
                //计算装货时间,不需要来货，到达之后直接装货
                loadCount = min(needCount, (GAME_FRAME - buyTime - sellTime - frameId) * buyBerth.transportTime);
                loadTime = buyTime + (int) floor(1.0 * loadCount / buyBerth.transportTime);
                value = prefixSum[buyBerth.id].get(loadCount);
            } else {
                //船到达之前
                double beforeComingCount = (buyTime - (startComingTime - frameId)) / comingSpeed;//装货时间除以速度
                if (beforeComingCount < 0) {
                    beforeComingCount = 0;
                }

                //船到达之后
                double afterComingCount = (GAME_FRAME - frameId - sellTime - max(buyTime, startComingTime - frameId))
                        / comingSpeed;
                //最大时间除以装货速度
                if (afterComingCount + beforeComingCount > 0) {
                    //一个货都来不了
                    loadCount = min(needCount - goodsList[buyBerth.id].size(), (int) (afterComingCount + beforeComingCount));
                }
                loadCount = loadCount + goodsList[buyBerth.id].size();
                loadCount = min(loadCount, (GAME_FRAME - frameId - buyTime - sellTime) / buyBerth.loadingSpeed);

                //计算更新的comingTime
                if (loadCount > goodsList[buyBerth.id].size()) {
                    updateStartComingTime = 1 + (int) ((loadCount - goodsList[buyBerth.id].size()) * comingSpeed + startComingTime);
                    int waitGoodsTime = (int) (((loadCount - goodsList[buyBerth.id].size()) * comingSpeed)) + (startComingTime - frameId);
                    loadTime = max(buyTime + loadCount * buyBerth.loadingSpeed, waitGoodsTime);//到达之后立马装货时间，和等待货物时间，取一个最大值。
                    value += prefixSum[buyBerth.id].get(goodsList[buyBerth.id].size()) + avgPullGoodsValue * (loadCount - goodsList[buyBerth.id].size());
                } else {
                    value += prefixSum[buyBerth.id].get(loadCount);
                    loadTime = buyTime + loadCount * buyBerth.loadingSpeed;
                }

            }
            //profit = value / (loadTime + sellTime)-buyTime*1e-10;//近的优先决策，远得后决策
            profit = value / (loadTime + sellTime);
            //相同泊位增加一下价值？
        }
        return new BoatProfitAndCount(profit, min(loadCount, goodsList[buyBerth.id].size()), updateStartComingTime);
    }

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
                //至少有未来一个格子，如果有两个也要预测，因为两个可以让他往旁边避让，预测前面3个格子，避让机器人要尽量躲开他的三个格子。
                for (int j = 1; j <= min(6, robot.path.size() - 1); j++) {
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
                    for (Point point : robotsPredictPath[crashId]) {
                        if (candidate.equal(point)) {
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
                    //value += estimateEraseValue(arriveSellTime + sellTime, selectRobot, sellBerth);

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


    private double estimateEraseValue(int beginFrame, Robot robot, Berth berth) {
        //todo 因为四个船船如果回家，机器人很容易聚集在一起，因为此时平均剩余价值回爆表，
        // 此时得估计他回家之后要来的泊位，这些泊位价值也会很高，
        // 回家得时间段内，导致了机器人得聚集，是不对得。
        int remainSpace = 0;
        for (Integer comingBoat : berth.comingBoats) {
            //最多容量
            remainSpace += boats[comingBoat].capacity - boats[comingBoat].num;
        }

        int berthTotalNum = berth.goodsNums;

        for (Robot other : robots) {
            if (other.id == robot.id || !other.assigned) {
                continue;
            }
            if (other.targetBerthId == berth.id) {
                berthTotalNum++;
            }
        }
        berthTotalNum += 1;//加上自己
        if (remainSpace < berthTotalNum) {
            return 0;//都满了
        }
        //这种时候没法再去别的地方消除了，只能无穷大，让他不去别的地方卖
        if (beginFrame + 3 * berth.transportTime > GAME_FRAME) {
            return 0;
        }
        for (int id : berth.comingBoats) {
            //基本价值越高，机器人越容易打配合
            int curRemain = boats[id].capacity - boats[id].num;
            if (curRemain >= berthTotalNum) {
                //已经有的货物越多，价值越好，可能太大了，，需要除以一点
                return 1.0 * (boats[id].num + berthTotalNum) * remainBerthsAvgValue / 2;
            }
            berthTotalNum -= curRemain;
        }
        assert false;
        return 0;
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
                if (frameId + sellTime >= GAME_FRAME) {
                    //如果不能到达，收益为负到达时间
                    profit = -sellTime;
                } else {
                    double value = robot.carryValue;
                    //value += estimateEraseValue(sellTime, robot, sellBerth);
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
        return goodArriveTime;
//        int estimateGoodsNums = berth.goodsNums;
//        for (Robot robot : robots) {
//            if (robot.assigned && robot.targetBerthId == berth.id
//                    && frameId + goodArriveTime > robot.estimateUnloadTime) {
//                estimateGoodsNums++;
//            }
//        }
//        estimateGoodsNums++;//加上这个货物时间
//
//        //检查所有的船，找出这个泊位的这个物品最快被什么时候消费
//        int remainGoods = estimateGoodsNums;
//        class Pair implements Comparable<Pair> {
//            final int first;//消费时间
//            final int second;//船id
//
//            public Pair(int first, int second) {
//                this.first = first;
//                this.second = second;
//            }
//
//            @Override
//            public int compareTo(Pair o) {
//                return Integer.compare(first, o.first);
//            }
//        }
//        PriorityQueue<Pair> timePairs = new PriorityQueue<>();
//        int[] boatNum = new int[boats.length];
//        for (Boat boat : boats) {
//            if (boat.targetId == berth.id) {
//                timePairs.offer(new Pair(boat.remainTime, boat.id));
//                boatNum[boat.id] = boat.capacity - boat.num;
//            } else if (boat.targetId == -1) {
//                if (boat.estimateComingBerthId == berth.id) {
//                    timePairs.offer(new Pair(boat.remainTime + berth.transportTime, boat.id));
//                } else {
//                    int id = boat.estimateComingBerthId == -1 ? 0 : boat.estimateComingBerthId;
//                    timePairs.offer(new Pair(boat.remainTime +//去另一个泊位装满，再回去，再去你这里
//                            2 * berths[id].transportTime +
//                            berth.transportTime, boat.id));
//                }
//                boatNum[boat.id] = boat.capacity;
//            } else {
//                boatNum[boat.id] = boat.capacity;
//                timePairs.offer(new Pair(boat.remainTime
//                        + berths[boat.targetId].transportTime//回去
//                        + berth.transportTime, boat.id));//再会来
//            }
//        }
//        int consumeTime;//这个货物被消费的最短时间，没加装货时间，这个是直接减的
//        while (true) {
//            Pair top = timePairs.poll();
//            assert top != null;
//            remainGoods -= boatNum[top.second];
//            if (remainGoods <= 0) {
//                consumeTime = top.first;
//                break;
//            }
//            boatNum[top.second] = boats[top.second].capacity;
//            timePairs.offer(new Pair(top.first + 2 * berth.transportTime, top.second));
//        }
//
//        //补充一点装货时间
//        consumeTime += estimateGoodsNums / berth.loadingSpeed;
//        //没到达也没法消费
//        return max(goodArriveTime, consumeTime);
    }

    private boolean input() throws IOException {
        String line = inStream.readLine();
        printMOST(line);
        if (line == null) {
            return false;
        }
        String[] parts = line.trim().split(" ");
        int tmp = frameId;
        frameId = Integer.parseInt(parts[0]);
        curFrameDiff = frameId - tmp;
        if (tmp + 1 != frameId) {
            count += frameId - tmp - 1;
        }
        money = Integer.parseInt(parts[1]);

        line = inStream.readLine();
        printMOST(line);
        parts = line.trim().split(" ");


        //新增工作台
        int num = Integer.parseInt(parts[0]);
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
        }
        String okk = inStream.readLine();
        printMOST(okk);
        return true;
    }
}
