package com.huawei.codecraft;

import java.io.IOException;

import static com.huawei.codecraft.Constants.WORKBENCH_EXIST_TIME;
import static com.huawei.codecraft.Utils.*;

public class Workbench {

    int id;
    Point pos = new Point();
    int value;
    Dijkstra dijkstra;//工作台到场上所有点的路径

    int remainTime;

    public Workbench(int id) {
        this.id=id;
        remainTime = WORKBENCH_EXIST_TIME;
    }

    public void input() throws IOException {
        String line = inStream.readLine();
        printMOST(line);
        String[] parts = line.trim().split(" ");
        pos.x = Integer.parseInt(parts[0]);
        pos.y = Integer.parseInt(parts[1]);
        value = Integer.parseInt(parts[2]);
        //初始化dij
    }
}
