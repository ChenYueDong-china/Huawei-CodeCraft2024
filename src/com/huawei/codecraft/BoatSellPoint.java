package com.huawei.codecraft;

import java.util.ArrayList;
import java.util.Arrays;

import static com.huawei.codecraft.BoatDijkstra.map2RelativePoint;
import static com.huawei.codecraft.BoatUtils.*;
import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.abs;

public class BoatSellPoint {

    int id;
    public Point point;
    GameMap gameMap;

    public short[][][] boatMinDistance = new short[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][DIR.length / 2];//船上的

    public BoatSellPoint(Point point) {
        this.point = point;
    }

    public void init(int id, GameMap gameMap, BoatDijkstra dijkstra) {
        this.gameMap = gameMap;
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
                    if (gameMap.boatCanReach(i, j, k)) {
                        Point point1 = map2RelativePoint(point, k);
                        int dir = (k ^ 1);
                        if (dijkstra.minDistance[point1.x][point1.y][dir] == Short.MAX_VALUE) {
                            continue;
                        }
                        int deep = (dijkstra.minDistance[point1.x][point1.y][dir] >> 2);
                        int lastDir = (dijkstra.minDistance[point1.x][point1.y][dir] & 3);
                        lastDir ^= 1;//回溯用
                        boatMinDistance[i][j][k] = (short) ((deep << 2) + lastDir);
                    }
                }
            }
        }
    }


    //获得这个workbench到任意一个位置的最小距离
    short getMinDistance(Point pos, int dir) {
        return boatMinDistance[pos.x][pos.y][dir] == Short.MAX_VALUE ?
                Short.MAX_VALUE : (short)( boatMinDistance[pos.x][pos.y][dir] >> 2);
    }

    public ArrayList<PointWithDirection> boatMoveFrom(Point pos, int dir, int recoveryTime) {
        ArrayList<PointWithDirection> tmp = new ArrayList<>();
        PointWithDirection t = new PointWithDirection(new Point(pos), dir);
        tmp.add(t);
        while ((boatMinDistance[t.point.x][t.point.y][t.direction] >> 2) != 0) {
            //特殊标记过
            int lastDir = boatMinDistance[t.point.x][t.point.y][t.direction] & 3;
            //求k,相当于从这个位置走到上一个位置
            int k;
            if (lastDir == t.direction) {
                k = 0;
            } else {
                k = gameMap.getRotationDir(t.direction, lastDir) == 0 ? 1 : 2;
            }
            t = getNextPoint(t, k);
            tmp.add(t);
        }
        ArrayList<PointWithDirection> result = boatFixedPath(gameMap, recoveryTime, tmp);
        if(result.size()==1){
            printError("error sellPoint start equal end");
            result.add(result.get(0));
        }
        return result;//添加恢复时间
    }


    public boolean canReach(Point pos, int dir) {
        return getMinDistance(pos, dir) != Short.MAX_VALUE;
    }
}
