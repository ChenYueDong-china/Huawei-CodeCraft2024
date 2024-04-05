package com.huawei.codecraft;

import java.io.IOException;
import java.util.ArrayList;

import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.abs;


public class Boat {
    Strategy strategy;


    //todo 定义船四种状态
    //1.和虚拟点移动中
    //2.泊位之间移动中
    //3.泊位外面等待
    //4.进入泊位
    public ArrayList<PointWithDirection> path = new ArrayList<>();
    Point corePoint = new Point(-1, -1);
    int id;
    int direction;

    int targetBerthId = -1;//泊位id
    int status;//状态
    BoatStatus exactStatus;//状态
    int originStatus;//原始状态,因为可能会被变，导致上一个id不一样
    int remainTime;//到达目标剩余时间
    int remainRecoveryTime = 0;//到达目标剩余时间
    int targetId = -1;//泊位id
    int lastArriveTargetId = -1;//泊位id
    int num;//目前货物数量
    final int capacity;//容量
    int value;//货物金额
    int estimateComingBerthId = -1;//估计要去的id,第一帧都默认0吧

    boolean assigned;


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
        num = Integer.parseInt(parts[1]);
        corePoint.x = Integer.parseInt(parts[2]);
        corePoint.y = Integer.parseInt(parts[3]);
        direction = Integer.parseInt(parts[4]);
        status = Integer.parseInt(parts[5]);
        if (status == 1) {
            //恢复状态
            remainRecoveryTime--;
        } else {
            //可能掉帧，一般情况下可能为1或者0
            remainRecoveryTime = 0;
        }
        assigned = false;
        path.clear();
    }

    public void finish() {
        //按照path来看做什么操作
        if (path == null || path.size() < 2) {//啥都不动，可能有问题，就算避让也会多走一帧
            printError("error");
            return;
        }
        if (status == 1) {
            return;//恢复状态，啥都干不了
        }
        PointWithDirection next = path.get(1);
        if (targetBerthId != -1 && next.point.equal(strategy.berths.get(targetBerthId).corePoint)) {
            flashBerth();//去泊位
        }
        if (next.point.equal(corePoint)) {
            assert next.direction == direction;//避让或者已经在目标了
            return;
        }

        if (next.direction == direction) {
            ship();//前进
        } else {
            int rotaDir = strategy.gameMap.getRotationDir(direction, next.direction);
            rotation(rotaDir);//旋转
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
    }

    public void flashDept() {
        assert status != 1;
        outStream.printf("dept %d\n", id);
        PointWithDirection next = strategy.boatFlashMainChannelPoint[corePoint.x][corePoint.y];
        assert next != null;
        corePoint = next.point;
        direction = next.direction;
        //计算恢复时间
        remainRecoveryTime = 2;//下一帧就变1了
    }
}
