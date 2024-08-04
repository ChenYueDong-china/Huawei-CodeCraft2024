#pragma once


#include <algorithm>
#include <queue>
#include "Utils.h"
#include "GameMap.h"

// 最短路
//struct Robot;
using std::queue;

class Dijkstra {
private:
    Point mTarget;//目标点
    GameMap *mGameMap{};
public:

    void init(Point target, GameMap &gameMap) {
        this->mTarget = target;
        this->mGameMap = &gameMap;
    }

    void update(int maxDeep,  short cs[MAP_FILE_ROW_NUMS / 2 + 1][MAP_FILE_COL_NUMS / 2 + 1]);

    void update(const vector<Point> &berthPoints, short cs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]);


};
