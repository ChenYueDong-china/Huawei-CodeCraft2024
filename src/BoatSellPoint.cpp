//
// Created by sg12q on 2024/4/4.
//

#include "BoatSellPoint.h"
#include "BoatDijkstra.h"
#include "BoatUtils.h"

void BoatSellPoint::init(int id_, GameMap &gameMap_) {
    this->gameMap = &gameMap_;
    this->id = id_;
    static BoatDijkstra dijkstra;
    dijkstra.init(point, gameMap_);
    dijkstra.update(SELL_POINT_MAX_SEARCH_DEEP);
    memset(boatMinDistance, 0x7f, sizeof boatMinDistance);
    for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
        for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
            for (int k = 0; k < (sizeof DIR / sizeof DIR[0]) / 2; k++) {
                if (gameMap_.boatCanReach(i, j, k)) {
                    Point point1 = BoatDijkstra::map2RelativePoint({i, j}, k);
                    int dir = (k ^ 1);
                    if (dijkstra.minDistance[point1.x][point1.y][dir] == SHORT_INF) {
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

vector<PointWithDirection> BoatSellPoint::boatMoveFrom(Point pos, int dir, int recoveryTime) {
    vector<PointWithDirection> tmp;
    PointWithDirection t = {pos, dir};
    tmp.push_back(t);
    while ((boatMinDistance[t.point.x][t.point.y][t.direction] >> 2) != 0) {
        //特殊标记过
        int lastDir = boatMinDistance[t.point.x][t.point.y][t.direction] & 3;
        //求k,相当于从这个位置走到上一个位置
        int k;
        if (lastDir == t.direction) {
            k = 0;
        } else {
            k = GameMap::getRotationDir(t.direction, lastDir) == 0 ? 1 : 2;
        }
        t = BoatUtils::getNextPoint(t, k);
        tmp.push_back(t);
    }
    vector<PointWithDirection> result = BoatUtils::boatFixedPath(*gameMap, recoveryTime, tmp);
    if (result.size() == 1) {
        printError("error sellPoint start equal end");
        result.push_back(result[0]);
    }
    return result;//添加恢复时间
}

short BoatSellPoint::getMinDistance(Point pos, int dir) {
    return boatMinDistance[pos.x][pos.y][dir] == SHORT_INF ?
           SHORT_INF : (short) (boatMinDistance[pos.x][pos.y][dir] >> 2);
}

bool BoatSellPoint::canReach(Point pos, int dir) {
    return getMinDistance(pos, dir) != SHORT_INF;
}
