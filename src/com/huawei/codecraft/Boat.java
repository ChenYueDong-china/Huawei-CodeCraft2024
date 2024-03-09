package com.huawei.codecraft;

import java.io.IOException;

import static com.huawei.codecraft.Utils.inStream;
import static com.huawei.codecraft.Utils.printMOST;

public class Boat {
    int id;
    int status;//状态
    int remainTime;//到达目标剩余时间
    int targetId;//泊位id
    int num;//目前货物数量
    final int capacity;//容量


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
    }
}
