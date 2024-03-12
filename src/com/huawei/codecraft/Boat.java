package com.huawei.codecraft;

import java.io.IOException;

import static com.huawei.codecraft.Utils.*;

public class Boat {

    //todo 定义船四种状态
    //1.和虚拟点移动中
    //2.泊位之间移动中
    //3.泊位外面等待
    //4.进入泊位
    int id;
    int status;//状态
    BoatStatus exactStatus;//状态
    int originStatus;//原始状态,因为可能会被变，导致上一个id不一样
    int remainTime;//到达目标剩余时间
    int targetId = -1;//泊位id
    int lastArriveTargetId = -1;//泊位id
    int num;//目前货物数量
    final int capacity;//容量
    int value;//货物金额
    int estimateComingBerthId;//估计要去的id

    boolean assigned;


    public Boat(int capacity) {
        num = 0;
        this.capacity = capacity;
    }

    public void input() throws IOException {
        String line = inStream.readLine();
        printMOST(line);
        String[] parts = line.trim().split(" ");
        status = Integer.parseInt(parts[0]);
        targetId = Integer.parseInt(parts[1]);
        if (originStatus == 0 && status != 0) {
            //不到达变到达状态
            this.lastArriveTargetId = targetId;
        }
        originStatus = status;


        //计算泊位状态
        if (targetId == -1) {
            if (status != 0) {
                exactStatus = BoatStatus.IN_ORIGIN_POINT;
            } else {
                //虚拟点没有等待状态
                exactStatus = BoatStatus.IN_ORIGIN_TO_BERTH;
            }
        } else {
            if (status == 1) {
                exactStatus = BoatStatus.IN_BERTH_INTER;
            } else if (status == 2) {
                exactStatus = BoatStatus.IN_BERTH_WAIT;
            } else {
                //运输中，可能是泊位到泊位，可能是虚拟点到泊位
                if (lastArriveTargetId == -1) {
                    exactStatus = BoatStatus.IN_ORIGIN_TO_BERTH;
                } else {
                    exactStatus = BoatStatus.IN_BERTH_TO_BERTH;
                }
            }
        }
        if (targetId == -1 && status == 1) {
            num = 0;//空闲了，销货完毕
            value = 0;
        }
        assigned = false;
    }

    public void ship(int id) {
        outStream.printf("ship %d %d\n", this.id, id);
    }

    public void go() {
        outStream.printf("go %d\n", id);
    }
}
