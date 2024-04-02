package com.huawei.codecraft;

import java.io.IOException;

import static com.huawei.codecraft.Utils.*;

public class Boat {

    //todo 定义船四种状态
    //1.和虚拟点移动中
    //2.泊位之间移动中
    //3.泊位外面等待
    //4.进入泊位

    Point corePoint = new Point(-1, -1);
    int id;
    int direction;
    int status;//状态
    BoatStatus exactStatus;//状态
    int originStatus;//原始状态,因为可能会被变，导致上一个id不一样
    int remainTime;//到达目标剩余时间
    int targetId = -1;//泊位id
    int lastArriveTargetId = -1;//泊位id
    int num;//目前货物数量
    final int capacity;//容量
    int value;//货物金额
    int estimateComingBerthId = -1;//估计要去的id,第一帧都默认0吧

    boolean assigned;


    public Boat(int capacity) {
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
        assigned = false;
    }

    public void ship(int id) {
        outStream.printf("ship %d %d\n", this.id, id);
    }

    public void go() {
        outStream.printf("go %d\n", id);
    }
}
