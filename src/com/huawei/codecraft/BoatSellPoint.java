package com.huawei.codecraft;

import java.util.ArrayList;

import static com.huawei.codecraft.Utils.*;

public class BoatSellPoint {
    public Point point;
    BoatDijkstra dijkstra;

    public BoatSellPoint(Point point) {
        this.point = point;
    }

    public void init(GameMap gameMap) {
        dijkstra = new BoatDijkstra();
        dijkstra.init(point, gameMap);
        dijkstra.update();
    }

    public ArrayList<PointWithDirection> moveFrom(Point pos, int dir) {
        return dijkstra.moveFrom(pos, dir);
    }

    //获得这个workbench到任意一个位置的最小距离
    int getMinDistance(Point pos, int dir) {
        return dijkstra.getMoveDistance(pos, dir);
    }

    public boolean canReach(Point pos, int dir) {
        return getMinDistance(pos, dir) != Integer.MAX_VALUE;
    }
}
