package com.huawei.codecraft;

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
}
