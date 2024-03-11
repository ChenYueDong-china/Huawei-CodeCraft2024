package com.huawei.codecraft;

import java.io.IOException;

import static com.huawei.codecraft.Utils.*;

public class Boat {
    int id;
    int status;//状态
    int remainTime;//到达目标剩余时间
    int targetId = -1;//泊位id
    int lastTargetId = -1;//泊位id
    int num;//目前货物数量
    final int capacity;//容量
    int value;//货物金额

    boolean assigned;


    public Boat(int capacity) {
        num = 0;
        this.capacity = capacity;
    }

    public void input() throws IOException {
        String line = inStream.readLine();
        printMOST(line);
        String[] parts = line.trim().split(" ");
        int lastStatus = status;
        status = Integer.parseInt(parts[0]);
        int lastTargetId = targetId;
        targetId = Integer.parseInt(parts[1]);
        if (lastStatus != 1 && status == 0 && lastTargetId != targetId) {
            //从完成或者等待变道运输中
            this.lastTargetId = targetId;
        }

        if (targetId == -1 && status == 1) {
            num = 0;//空闲了，销货完毕
            value = 0;
        }
        assigned = false;
    }

    public void ship(int id) {
        outStream.printf("ship %d %d", this.id, id);
    }

    public void go() {
        outStream.printf("go %d", id);
    }
}
