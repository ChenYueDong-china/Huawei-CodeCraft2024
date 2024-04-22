package com.huawei.codecraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.min;

public class Robot {
    public int avoidOtherTime = 0;//在避让别人时间

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
    boolean lastFrameMove = true; //上一帧是否移动
    int noMoveTime = 0;

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
        simpleRobot.type = type;
        assigned = false;
        carryValue = 0;
        if (lastFrameMove && simpleRobot.lastP.equal(simpleRobot.p)) {
            noMoveTime++;
        } else {
            noMoveTime = 0;
        }
        if (num != 0) {
            for (Integer i : simpleRobot.goodList) {
                carryValue += i;
            }
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
            num += 1;
            carry = num == maxNum;
            lastBuyPos = new Point(strategy.workbenches.get(targetWorkBenchId).pos);
            strategy.workbenchesLock.add(targetWorkBenchId);//锁住不能再次决策
            //不一定能买的到，不能删除，也不能把自己得目标改了
        }
    }

    public void finish() {
        //按照path来看做什么操作

        if (path.size() <= 2) {
            printError("frameId:" + frameId + ",error");
            lastFrameMove = false;
            return;
        }
        Point target = strategy.gameMap.discreteToPos(path.get(2));
        assert strategy.gameMap.robotCanReach(target.x, target.y);
        if (target.equal(pos)) {
            if (isPending && ans != -1) {
                //回答问题
                outStream.printf("ans %d %d\n", id, ans);
            }
            lastFrameMove = false;
            return;
        }
        int dir = getDir(target.sub(pos));
        outStream.printf("move %d %d\n", id, dir);
        lastFrameMove = true;
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
        targetBerthId = -1;
        targetWorkBenchId = -1;
        carry = false;
        num = 0;
        outStream.printf("pull %d\n", id);
    }
}



