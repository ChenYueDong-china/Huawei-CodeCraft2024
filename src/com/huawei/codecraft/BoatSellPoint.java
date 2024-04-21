package com.huawei.codecraft;

import java.util.ArrayList;
import java.util.Arrays;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;

public class BoatSellPoint {

    int id;
    public Point point;

    public short[][][] boatMinDistance = new short[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][DIR.length / 2];//船上的

    public BoatSellPoint(Point point) {
        this.point = point;
    }

    public void init(int id, GameMap gameMap,BoatDijkstra dijkstra) {
        this.id = id;
        dijkstra.init(point, gameMap);
        dijkstra.update(SELL_POINT_MAX_SEARCH_DEEP);
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                Arrays.fill(boatMinDistance[i][j], Short.MAX_VALUE);
            }
        }
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                Point point = new Point(i, j);
                for (int k = 0; k < DIR.length / 2; k++) {
                    int distance = dijkstra.getMoveDistance(point, k);
                    if (distance < boatMinDistance[i][j][k]) {
                        boatMinDistance[i][j][k] = (short) distance;
                    }
                }
            }
        }
    }


    //获得这个workbench到任意一个位置的最小距离
    int getMinDistance(Point pos, int dir) {
        return boatMinDistance[pos.x][pos.y][dir];
    }


    public boolean canReach(Point pos, int dir) {
        return getMinDistance(pos, dir) != Integer.MAX_VALUE;
    }
}
