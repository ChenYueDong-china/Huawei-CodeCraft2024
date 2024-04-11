package com.huawei.codecraft;

import java.util.ArrayList;
import java.util.Arrays;

import static com.huawei.codecraft.Constants.MAP_FILE_COL_NUMS;
import static com.huawei.codecraft.Constants.MAP_FILE_ROW_NUMS;
import static com.huawei.codecraft.Utils.*;

public class BoatSellPoint {

    int id;
    public Point point;
    public BoatDijkstra dijkstra;
    public int[][][] boatMinDistance = new int[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][DIR.length / 2];//船上的

    public BoatSellPoint(Point point) {
        this.point = point;
    }

    public void init(int id, GameMap gameMap) {
        this.id = id;
        dijkstra = new BoatDijkstra();
        dijkstra.init(point, gameMap);
        dijkstra.update();
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                Arrays.fill(boatMinDistance[i][j], Integer.MAX_VALUE);
            }
        }
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                Point point = new Point(i, j);
                for (int k = 0; k < DIR.length / 2; k++) {
                    int distance = dijkstra.getMoveDistance(point, k);
                    if (distance < boatMinDistance[i][j][k]) {
                        boatMinDistance[i][j][k] = distance;
                    }
                }
            }
        }
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
