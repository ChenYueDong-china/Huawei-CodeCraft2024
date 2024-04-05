package com.huawei.codecraft;

import java.util.ArrayList;

import static com.huawei.codecraft.Utils.*;

public class BoatSellPoint {

    int id;
    public Point point;
    BoatDijkstra dijkstra;

    public BoatSellPoint(Point point) {
        this.point = point;
    }

    public void init(int id, GameMap gameMap) {
        this.id = id;
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

    int getEndDir(Point pos, int dir) {
        return dijkstra.getEndDir(pos, dir);
    }

    public boolean canReach(Point pos, int dir) {
        return getMinDistance(pos, dir) != Integer.MAX_VALUE;
    }
}
