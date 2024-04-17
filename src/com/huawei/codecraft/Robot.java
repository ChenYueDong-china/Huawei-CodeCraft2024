package com.huawei.codecraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.min;

public class Robot {
    public int type = 0;
    public int maxNum = 1;
    public int id = -1;
    public int globalId = -1;
    public int num = 0;
    public int buyFrame = 0;//购买时间
    public boolean carry;
    public boolean avoid;
    public int carryValue;
    public Point lastBuyPos = new Point();

    public Point pos = new Point();
    public int status;
    public boolean redundancy;//机器人是否有冗余时间去买，无的话最前面，第二携带物品的，最后看价值

    public boolean assigned;
    public boolean buyAssign;
    public int targetBerthId = -1;//目标卖工作台id,这个是berth了，其实都一样
    public int targetWorkBenchId = -1;//目标买id，其实都一样
    public ArrayList<Point> path = new ArrayList<>();

    public Strategy strategy;
    public int forcePri;
    int beConflicted = 0;  // 被冲突
    int priority = 0;
    boolean isPending;
    String question;
    int questionId;
    int ans = -1;

    public Robot(Strategy strategy, int type) {
        this.strategy = strategy;
        this.type = type;
        this.maxNum = type == 0 ? 1 : 2;
    }


    public void input(SimpleRobot simpleRobot) throws IOException {
        carry = simpleRobot.num == maxNum;
        this.num = simpleRobot.num;
        pos.x = simpleRobot.p.x;
        pos.y = simpleRobot.p.y;
        assigned = false;
        if (num == 0) {
            carryValue = 0;
        }
        redundancy = true;//到目标点有冗余时间
        avoid = false;
        path.clear();
        buyAssign = false;
    }

    public void buy() {
        outStream.printf("get %d\n", id);
        int value = strategy.workbenches.get(targetWorkBenchId).value;
        if (value < PRECIOUS_WORKBENCH_BOUNDARY) {
            //珍贵物品，啥都不干，会进入问答状态
            carry = true;
            lastBuyPos = new Point(strategy.workbenches.get(targetWorkBenchId).pos);
            carryValue = value;
            //不销毁，可以加锁
            targetWorkBenchId = -1;
        }
    }

    public void finish() {
        //按照path来看做什么操作

        if (path.size() <= 2) {
            printError("frameId:" + frameId + ",error");
            return;
        }
        Point target = strategy.gameMap.discreteToPos(path.get(2));
        assert strategy.gameMap.robotCanReach(target.x, target.y);
        if (target.equal(pos)) {
            if (isPending && ans != -1) {
                //回答问题
                outStream.printf("ans %d %d\n", id, ans);
                printDebug("frame:" + frameId + ",question:" + question + ",id:" + id + ",ans:" + ans);
                strategy.workbenchesPermanentLock.add(targetWorkBenchId);
                //可能没有成功回答？，理论上pending下一帧一定能回答，判题器有问题，下一帧继续回答一下
            }
            return;
        }
        int dir = getDir(target.sub(pos));
        outStream.printf("move %d %d\n", id, dir);
        if (!assigned) {
            //没任务，但是避让了
            return;
        }
        //此时是移动后，可以提前做一些事情
        if (carry) {
            //要去卖
            assert targetBerthId != -1;
            //在他这个berth范围内
            if (strategy.berths.get(targetBerthId).inBerth(target)) {
                //提前卖，移动完毕卖,货物这种时候可以增加
                pull();
            }
        } else {
            //要去买
            assert targetWorkBenchId != -1;
            if (strategy.workbenches.get(targetWorkBenchId).pos.equal(target)) {
                //提前买，移动完毕买,机器人可以移动后立即取货
                buy();
            }
        }
    }

    public void pull() {
        strategy.totalPullGoodsCount++;
        strategy.totalPullGoodsValues += carryValue;
        strategy.avgPullGoodsValue = 1.0 * strategy.totalPullGoodsValues / strategy.totalPullGoodsCount;
        strategy.pullScore += carryValue;
        targetBerthId = -1;
        targetWorkBenchId = -1;
        carry = false;
        num = 0;
        outStream.printf("pull %d\n", id);
    }
}



