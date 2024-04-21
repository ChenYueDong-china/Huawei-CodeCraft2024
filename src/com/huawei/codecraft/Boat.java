package com.huawei.codecraft;

import java.io.IOException;
import java.util.ArrayList;

import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.abs;
import static com.huawei.codecraft.Constants.*;

public class Boat {

    public int beConflicted = 0;
    public int avoidOtherTime = 0;
    public int noMoveTime = 0;//发了移动指令但没移动的帧数
    boolean lastFrameMove = true; //上一帧是否移动
    public boolean carry = false;
    int id = -1;
    int globalId = -1;//需要，因为可能别人会占用泊位，会出问题
    public int sellCount = 0;
    Strategy strategy;
    public ArrayList<PointWithDirection> path = new ArrayList<>();
    Point corePoint = new Point(-1, -1);
    int direction;
    int targetBerthId = -1;//买的Id
    int targetSellId = -1;//卖的id
    int status;//状态
    int originStatus;//状态
    int remainRecoveryTime = 0;//恢复时间

    int lastNum;//目前货物数量
    int num;//目前货物数量
    int value;//货物金额

    final int capacity;//容量
    boolean assigned;
    boolean buyAssign;
    boolean avoid;
    public int forcePri;

    private boolean lastFlashBerth = false;
    private boolean lastFlashDept = false;


    public Boat(Strategy strategy, int capacity) {
        this.strategy = strategy;
        num = 0;
        this.capacity = capacity;
    }

    public void input(SimpleBoat simpleBoat) throws IOException {
        num = simpleBoat.num;
        corePoint.x = simpleBoat.corePoint.x;
        corePoint.y = simpleBoat.corePoint.y;
        direction = simpleBoat.direction;
        status = simpleBoat.status;
        for (BoatSellPoint boatSellPoint : strategy.boatSellPoints) {
            if (num == 0 && corePoint.equal(boatSellPoint.point)) {
                targetSellId = -1;//卖完
                carry = false;//不考虑金额，没必要
                value = 0;
            }
        }
        if (lastFrameMove && corePoint.equal(simpleBoat.lastPointWithDirection.point)
                &&
                direction == simpleBoat.lastPointWithDirection.direction) {
            noMoveTime++;
        } else {
            noMoveTime = 0;
        }

        if (lastFlashBerth && status == 0) {//闪现，且这一帧没等待或者出问题
            //没闪现成功
            assert strategy.berths.get(targetBerthId).curBoatId != -1;
            assert strategy.berths.get(targetBerthId).curBoatId < globalId;
            //不一定是代码问题，可能同时闪现，自己没成功
            printError(frameId + "last frame flash berth default");
        }
        if (lastFlashBerth && status != 0) {
            //闪现成功,这个泊位当前boat一定是我
            strategy.berths.get(targetBerthId).curBoatId = globalId;
        }
        lastFlashBerth = false;
        if (lastFlashDept && !strategy.gameMap.boatIsAllInMainChannel(corePoint, direction)) {
            //没闪现成功
            printError(frameId + "error last frame flash dept default");
        }
        lastFlashDept = false;
        if (remainRecoveryTime < 0) {
            printError("error remainRecoveryTime < 0");
            remainRecoveryTime = 0;
        }
        if (status == 1) {
            //恢复状态
            remainRecoveryTime--;
        } else {
            //可能掉帧，一般情况下可能为1或者0
            remainRecoveryTime = 0;
        }
        assigned = false;
        path.clear();
        buyAssign = false;
    }

    public void finish() {
        //按照path来看做什么操作
        if (status != 1
                && !assigned
                && !strategy.gameMap.boatIsAllInMainChannel(corePoint, direction)
                && frameId > GAME_FRAME - 1000) {
            flashDept();
            lastFrameMove = false;
            return;
        }
        if (path == null || path.size() < 2) {//啥都不动，可能有问题，就算避让也会多走一帧
            printError("error");
            lastFrameMove = false;
            return;
        }
        if (status == 1 || (status == 2
                && targetBerthId != -1
                && strategy.berths.get(targetBerthId).curBoatId == id)) {
            //恢复状态，或者已经在目标泊位装货了
            lastFrameMove = false;
            return;
        }
        PointWithDirection next = path.get(1);
        if (!carry
                && targetBerthId != -1
                && strategy.berths.get(targetBerthId).curBoatId == -1
                && next.point.equal(strategy.berths.get(targetBerthId).corePoint)) {
            flashBerth();//去泊位
            lastFrameMove = false;//闪现泊位不能算动，因为可以跟别人抢位置
            return;
        }
        if (next.point.equal(corePoint)) {
            assert next.direction == direction;//避让或者已经在目标了
            lastFrameMove = false;
            return;
        }

        if (next.direction == direction && corePoint.add(DIR[direction]).equal(next.point)) {
            ship();//前进
            lastFrameMove = true;
        } else {
            int rotaDir = strategy.gameMap.getRotationDir(direction, next.direction);
            PointWithDirection rotationPoint = getBoatRotationPoint(new PointWithDirection(corePoint, direction)
                    , rotaDir == 0);
            if (rotationPoint.point.equal(next.point)) {
                rotation(rotaDir);//旋转
                lastFrameMove = true;
            } else {
                //闪现去主航道
                assert strategy.gameMap.boatIsAllInMainChannel(next.point, next.direction);
                flashDept();
                lastFrameMove = false;
            }
        }
    }

    public void ship() {
        assert status != 1;
        outStream.printf("ship %d\n", id);
        //撞到主航道
        Point dir = DIR[direction];
        corePoint.x += dir.x;
        corePoint.y += dir.y;
        //检查移动后的位置
        if (strategy.gameMap.boatHasOneInMainChannel(corePoint, direction)) {
            remainRecoveryTime = 2;
        }

    }

    public void rotation(int rotaDir) {
        assert status != 1;
        outStream.printf("rot %d %d\n", id, rotaDir);
        //撞到主航道
        boolean clockwise = rotaDir == 0;
        PointWithDirection next = getBoatRotationPoint(new PointWithDirection(corePoint, direction), clockwise);
        corePoint.x = next.point.x;
        corePoint.y = next.point.y;
        direction = next.direction;
        //检查移动后的位置
        if (strategy.gameMap.boatHasOneInMainChannel(corePoint, direction)) {
            status = 1;
            remainRecoveryTime = 2;
        }
    }

    public void flashBerth() {
        assert status != 1;
        outStream.printf("berth %d\n", id);
        //计算恢复时间
        PointWithDirection next = new PointWithDirection(strategy.berths.get(targetBerthId).corePoint, 0);
        flashToNextPoint(next);
        lastFlashBerth = true;
    }

    public void flashDept() {
        assert status != 1;
        outStream.printf("dept %d\n", id);
        PointWithDirection next = strategy.getBoatFlashDeptPoint(corePoint);
        flashToNextPoint(next);
        lastFlashDept = true;
        //计算恢复时间

    }

    private void flashToNextPoint(PointWithDirection next) {
        remainRecoveryTime = 1 + 2 * (abs(next.point.x - corePoint.x) + abs(next.point.y - corePoint.y));
        corePoint.x = next.point.x;
        corePoint.y = next.point.y;
        direction = next.direction;
        status = 1;//闪现成功
    }
}
