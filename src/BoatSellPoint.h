//
// Created by sg12q on 2024/4/4.
//

#include "Utils.h"
#include "GameMap.h"
struct BoatSellPoint {

    int id{};
    Point point;
    GameMap *gameMap{};
    short boatMinDistance[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][(sizeof DIR / sizeof DIR[0]) / 2]{};

    BoatSellPoint() = default;

    explicit BoatSellPoint(Point point) {
        this->point = point;
    }

    void init(int id_, GameMap &gameMap_);

    vector<PointWithDirection> boatMoveFrom(Point pos, int dir, int recoveryTime);

    //获得这个workbench到任意一个位置的最小距离
    short getMinDistance(Point pos, int dir);

    bool canReach(Point pos, int dir);


};