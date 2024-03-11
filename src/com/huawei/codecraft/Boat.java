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
    int originStatus;//原始状态,因为可能会被变，导致上一个id不一样
    int originTargetId = -1;//原始目标id,因为代码可能漏洞，导致改变
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
        int lastStatus = originStatus;
        status = Integer.parseInt(parts[0]);
        originStatus = status;
        int lastTargetId = originTargetId;
        targetId = Integer.parseInt(parts[1]);
        originTargetId = targetId;
        if (lastStatus != 0 && status == 0 && lastTargetId != targetId) {
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
        outStream.printf("ship %d %d\n", this.id, id);
    }

    public void go() {
        outStream.printf("go %d\n", id);
    }
}
