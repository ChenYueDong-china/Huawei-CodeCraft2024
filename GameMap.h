//
// Created by sg12q on 2024/3/9.
//
#pragma once

#include <queue>
#include "Utils.h"
#include "Constants.h"

class GameMap {
private:
    char mapData[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]{};
    bool robotDiscreteMapData[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH]{};
    bool robotDiscreteMainChannel[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH]{};//robot离散化之后是不是主干道
    bool boatCanReach_[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2]{};//船是否能到达
    bool boatIsAllInMainChannel_[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2]{};//整艘船都在主航道上
    bool boatHasOneInMainChannel_[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2]{};//有至少一个点在主航道上
    int boatAroundBerthId[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]{};//船闪现能到达得泊位,只有在靠泊区和泊位有值
    int partOfBerthId[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]{};//这个点如果是泊位，那么id是啥
    void initRobotsDiscrete();

public:
    int robotCommonDiscreteCs[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH]{};//寻路得时候复用cs
    short boatCommonCs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2]{};//寻路得时候复用cs
    bool commonConflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]{};//寻路得时候复用cs
    bool commonNoResultPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]{};//寻路得时候复用cs
    int curWorkbenchId[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]{};//寻路得时候复用cs
    int robotVisits[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]{};//寻路得时候复用cs
    short robotCommonCs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]{};
    short robotCommonHeuristicCs[ROBOT_FOR_WORKBENCH_HEURISTIC_SIZE][MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];//寻路得时候复用cs
    int boatVisits[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2]{};//寻路得时候复用cs
    unordered_map<int, int> robotUseHeuristicCsWbIds;
    std::queue<int> robotUseHeuristicCsWbIdList;
    int curVisitId = 0;

    bool robotCanReachDiscrete(int x, int y);

    bool setMap(char mapData[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]);

    static Point posToDiscrete(Point point);

    static Point posToDiscrete(int x, int y);

    static Point discreteToPos(Point point);

    static Point discreteToPos(int x, int y);

    static vector<Point> toDiscretePath(vector<Point> &tmp);

    static vector<Point> toRealPath(vector<Point> &tmp);

    bool isBerth(Point target);

    static bool isLegalPoint(int x, int y);

    bool boatCanReach(int x, int y);

    bool isBoatMainChannel(int x, int y);

    bool boatIsAllInMainChannel(Point corePoint, int direction);

    bool boatHasOneInMainChannel(Point corePoint, int direction);

    static vector<Point> getBoatPoints(Point corePoint, int direction);

    bool robotCanReach(int x, int y);

    bool boatCanReach(int x, int y, int dir);

    bool boatCanReach(Point corePoint, int dir);

    static int getRotationDir(int curDir, int nextDir);

    bool isBerthAround(int x, int y);

    bool isBerth(int x, int y);

    int boatGetFlashBerthId(int x, int y);

    void updateBerthAndAround(const vector<Point> &AroundPoints, const vector<Point> &corePoints, int berthId);

    int getDiscreteBelongToBerthId(int x, int y);

    bool isRobotMainChannel(int x, int y);

    bool isRobotDiscreteMainChannel(int x, int y);

    int getBelongToBerthId(Point point);


};

