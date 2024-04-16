package com.huawei.codecraft;

import java.io.IOException;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;

public class Workbench {

    int id;
    Point pos = new Point();
    int value;
    int minSellDistance;
    Dijkstra dijkstra;//工作台到场上所有点的路径

    int remainTime;

    public Workbench(int id) {
        this.id = id;
        remainTime = WORKBENCH_EXIST_TIME;
    }

    public void input(GameMap map) throws IOException {
        String line = inStream.readLine();
        printMost(line);
        String[] parts = line.trim().split(" ");
        pos.x = Integer.parseInt(parts[0]);
        pos.y = Integer.parseInt(parts[1]);
        value = Integer.parseInt(parts[2]);
        if (value == 0) {
            return;
        }
        //初始化dij
        dijkstra = new Dijkstra();
        dijkstra.init(pos, map);
        dijkstra.update(WORKBENCH_MAX_SEARCH_DEEP,WORKBENCH_MAX_SEARCH_COUNT);
    }

    //获得这个workbench到任意一个位置的最小距离
    int getMinDistance(Point point) {
        return dijkstra.getMoveDistance(point);
    }

    public boolean canReach(Point pos) {
        return getMinDistance(pos) != Integer.MAX_VALUE;
    }
}
