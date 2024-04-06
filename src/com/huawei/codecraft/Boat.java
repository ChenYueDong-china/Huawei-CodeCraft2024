package com.huawei.codecraft;

import java.io.IOException;
import java.util.ArrayList;

import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.abs;
import static com.huawei.codecraft.Constants.*;

public class Boat {

    public int beConflicted = 0;
    public boolean carry = false;
    int id;
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


    public Boat(Strategy strategy, int capacity) {
        this.strategy = strategy;
        num = 0;
        this.capacity = capacity;
    }

    public void input() throws IOException {
        String line = inStream.readLine();
        printMost(line);
        String[] parts = line.trim().split(" ");
        id = Integer.parseInt(parts[0]);
        lastNum = num;
        num = Integer.parseInt(parts[1]);
        if (num == 0) {
            targetSellId = -1;//卖完
            carry = false;
        }
        corePoint.x = Integer.parseInt(parts[2]);
        corePoint.y = Integer.parseInt(parts[3]);
        direction = Integer.parseInt(parts[4]);
        status = Integer.parseInt(parts[5]);
        if (status != 2 && originStatus == 2) {
            //从装货到进入恢复状态或者行驶状态，说明船舶离开泊位，解锁,别人可以闪现过去
            for (Berth berth : strategy.berths) {
                if (berth.curBoatId == id) {
                    berth.curBoatId = -1;
                }
            }
        }
        originStatus = status;
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
        if (!assigned && !strategy.gameMap.boatIsAllInMainChannel(corePoint, direction) && frameId > GAME_FRAME - 1000) {
            flashDept();
            return;
        }
        if (path == null || path.size() < 2) {//啥都不动，可能有问题，就算避让也会多走一帧
            printError("error");
            return;
        }
        if (status == 1 || (status == 2
                && targetBerthId != -1
                && strategy.berths.get(targetBerthId).curBoatId == id)) {
            //恢复状态，或者已经在目标泊位装货了
            return;
        }
        PointWithDirection next = path.get(1);
        if (!carry
                && targetBerthId != -1
                && strategy.berths.get(targetBerthId).curBoatId == -1
                && next.point.equal(strategy.berths.get(targetBerthId).corePoint)) {
            flashBerth();//去泊位
            return;
        }
        if (next.point.equal(corePoint)) {
            assert next.direction == direction;//避让或者已经在目标了
            return;
        }

        if (next.direction == direction && corePoint.add(DIR[direction]).equal(next.point)) {
            ship();//前进
        } else {
            int rotaDir = strategy.gameMap.getRotationDir(direction, next.direction);
            PointWithDirection rotationPoint = getBoatRotationPoint(new PointWithDirection(corePoint, direction)
                    , rotaDir == 0);
            if (rotationPoint.point.equal(next.point)) {
                rotation(rotaDir);//旋转
            } else {
                //闪现去主航道
                assert strategy.gameMap.boatIsAllInMainChannel(next.point, next.direction);
                flashDept();
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
        corePoint = next.point;
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
        remainRecoveryTime = 1 + 2 * (abs(next.point.x - corePoint.x) + abs(next.point.y - corePoint.y));
        corePoint = next.point;
        direction = next.direction;
        status = 1;//闪现成功
        strategy.berths.get(targetBerthId).curBoatId = id;
    }

    public void flashDept() {
        assert status != 1;
        outStream.printf("dept %d\n", id);
        PointWithDirection next = strategy.getBoatFlashDeptPoint(corePoint);
        remainRecoveryTime = 2;//下一帧就变1了
        corePoint = next.point;
        direction = next.direction;
        status = 1;//闪现成功
        //计算恢复时间

    }
}
