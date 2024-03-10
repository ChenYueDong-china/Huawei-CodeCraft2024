package com.huawei.codecraft;

import java.io.IOException;

import static com.huawei.codecraft.Utils.*;

public class Boat {
    int id;
    int status;//状态
    int remainTime;//到达目标剩余时间
    int targetId;//泊位id
    int num;//目前货物数量
    final int capacity;//容量

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
        if (targetId == -1 && status == 1) {
            num = 0;//空闲了，销货完毕
        }
        assigned = false;
        if (targetId == -1 && status == 0) {
            assigned = true;//在回家，不管；
        }
    }

    public void ship(int id) {
        outStream.printf("ship %d %d", this.id, id);
    }

    public void go() {
        outStream.printf("go %d", id);
    }
}
