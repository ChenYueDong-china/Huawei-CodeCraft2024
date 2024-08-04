//
// Created by sg12q on 2024/3/9.
//

#pragma once

#include <list>
#include "Utils.h"
#include "Constants.h"
#include "Dijkstra.h"
#include "BoatDijkstra.h"

using std::queue;

class Berth {
public:
    int id;
    int curBoatGlobalId = -1;
    int lastBoatGlobalId = -1;
    int curBoatIdNoChangeTime = 0;
    Point corePoint;
    int coreDirection;
    int loadingSpeed;
    int minSellDistance;
    int minSellPointId = -1;
    int goodsNums;
    int totalGoodsNums;//来的总货，判断泊位好坏

    queue<int> goods;

    int totalValue = 0;

    short robotMinDistance[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
    short boatMinDistance[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][(sizeof DIR / sizeof DIR[0]) / 2];
    vector<Point> berthPoints;
    vector<Point> berthAroundPoints;
    GameMap *gameMap;

    void init(GameMap &gameMap_);

    bool robotCanReach(Point pos);

    short getRobotMinDistance(Point pos);

    bool inBerth(Point point) const;

    bool boatCanReach(Point pos, int dir);

    vector<PointWithDirection> boatMoveFrom(Point pos, int dir, int recoveryTime, bool comeBreak);

    vector<Point> robotMoveFrom(Point point);

    short getBoatMinDistance(Point pos, int dir);
};
