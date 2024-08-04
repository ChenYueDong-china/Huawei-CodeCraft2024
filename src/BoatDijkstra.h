//
// Created by sg12q on 2024/4/4.
//

#pragma once

#include <queue>
#include "Utils.h"
#include "GameMap.h"

class BoatDijkstra {
    Point mTarget{};//目标点
    GameMap *mGameMap{};
public:
    short minDistance[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]
    [(sizeof DIR / sizeof DIR[0]) / 2]{};

    void init(Point target, GameMap &gameMap) {
        this->mTarget = target;
        this->mGameMap = &gameMap;
    }

    static Point map2RelativePoint(Point corePoint, int dir);


    void update(int maxDeep=INT_INF);


    void adjustDistance();

    void berthUpdate(const vector<Point>& berthAroundPoints, Point berthCorePoint,  int maxDeep);
};