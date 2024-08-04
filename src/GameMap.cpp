//
// Created by sg12q on 2024/3/9.
//

#include "GameMap.h"

using std::string;

bool GameMap::robotCanReachDiscrete(int x, int y) {
    return (x > 0 && x < MAP_DISCRETE_HEIGHT - 1 && y > 0 && y < MAP_DISCRETE_WIDTH - 1 &&
            (robotDiscreteMapData[x][y]));
}

bool GameMap::setMap(char mapData_[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
    for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
        memcpy(this->mapData[i], mapData_[i], MAP_FILE_COL_NUMS);
    }
    initRobotsDiscrete();

    for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
        for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
            for (int k = 0; k < ((sizeof DIR / sizeof DIR[0]) / 2); k++) {
                bool canReach = true, oneIn = false, allIn = true;
                vector<Point> boatPoints = getBoatPoints(Point(i, j), k);
                //都是主航道点
                for (const Point &point: boatPoints) {
                    if (isBoatMainChannel(point.x, point.y)) {
                        oneIn = true;
                    } else {
                        allIn = false;
                    }
                    if (!boatCanReach(point.x, point.y)) {
                        canReach = false;
                    }
                }
                boatCanReach_[i][j][k] = canReach;
                boatHasOneInMainChannel_[i][j][k] = oneIn;
                boatIsAllInMainChannel_[i][j][k] = allIn;
            }
        }
    }

    for (auto &i: partOfBerthId) {
        for (int &j: i) {
            j = -1;
        }
    }

    for (auto &i: boatAroundBerthId) {
        for (int &j: i) {
            j = -1;
        }
    }
    for (auto &i: curWorkbenchId) {
        for (int &j: i) {
            j = -1;
        }
    }
    for (int i = -ROBOT_FOR_WORKBENCH_HEURISTIC_SIZE; i < 0; i++) {
        robotUseHeuristicCsWbIds.insert({i, i + ROBOT_FOR_WORKBENCH_HEURISTIC_SIZE});
        robotUseHeuristicCsWbIdList.push(i);//id列表，一旦超出，弹出最前面的
    }
    return true;
}

Point GameMap::posToDiscrete(Point point) {
    return posToDiscrete(point.x, point.y);
}

Point GameMap::posToDiscrete(int x, int y) {
    return {2 * x + 1, 2 * y + 1};
}

Point GameMap::discreteToPos(int x, int y) {
    assert(x % 2 == 1 && y % 2 == 1);//偶数点是额外添加的点，不是真实点
    return {x / 2, y / 2};
}

void GameMap::initRobotsDiscrete() {
    memset(robotDiscreteMainChannel, 1, sizeof robotDiscreteMainChannel);
    memset(robotDiscreteMapData, 1, sizeof robotDiscreteMapData);

    for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
        for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
            if (!isRobotMainChannel(i, j)) {
                //其他东西
                Point point = posToDiscrete(i, j);
                robotDiscreteMainChannel[point.x][point.y] = false;
                //周围八个点和这个点都是不可达点,其实只需要四个点，因为其他四个不可达
                for (const Point &dir: DIR) {
                    Point tmp = point + dir;
                    robotDiscreteMainChannel[tmp.x][tmp.y] = false;//不是主干道，需要检测
                }
            }
            if (!robotCanReach(i, j)) {
                //海洋或者障碍;
                Point point = posToDiscrete(i, j);
                robotDiscreteMapData[point.x][point.y] = false;
                //周围八个点和这个点都是不可达点
                for (const Point &dir: DIR) {
                    Point tmp = point + dir;
                    robotDiscreteMapData[tmp.x][tmp.y] = false;//不可达
                }
            }
        }
    }
}

vector<Point> GameMap::toDiscretePath(vector<Point> &tmp) {
    vector<Point> result;
    result.reserve(2 * result.size());
    for (int i = 0; i < int(tmp.size()) - 1; i++) {
        Point cur = posToDiscrete(tmp[i]);
        Point next = posToDiscrete(tmp[i + 1]);
        Point mid = (cur + next) / 2;
        result.push_back(cur);
        result.push_back(mid);
    }
    result.push_back(posToDiscrete(tmp.back()));
    return result;
}

vector<Point> GameMap::toRealPath(vector<Point> &tmp) {
    assert(tmp.size() % 2 == 1);//2倍的路径加个起始点
    vector<Point> result;
    for (int i = 0; i < tmp.size(); i += 2) {
        result.push_back(discreteToPos(tmp.at(i).x, tmp.at(i).y));
    }
    return result;
}

bool GameMap::isBerth(Point target) {
    assert (target.x >= 0 && target.y >= 0 && target.x < MAP_FILE_ROW_NUMS && target.y <= MAP_FILE_COL_NUMS);
    return mapData[target.x][target.y] == 'B';
}

Point GameMap::discreteToPos(Point point) {
    return discreteToPos(point.x, point.y);
}

bool GameMap::isLegalPoint(int x, int y) {
    return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS);
}

bool GameMap::boatCanReach(int x, int y) {
    return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
            (mapData[x][y] == '*'//海洋
             || mapData[x][y] == 'B'//泊位
             || mapData[x][y] == '~'//主航道
             || mapData[x][y] == 'S'//购买地块
             || mapData[x][y] == 'K'//靠泊区
             || mapData[x][y] == 'C'//交通地块
             || mapData[x][y] == 'c'//交通地块,加主航道
             || mapData[x][y] == 'T'//交货点
            )
    );
}

bool GameMap::isBoatMainChannel(int x, int y) {
    return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
            (mapData[x][y] == '~'//主航道
             || mapData[x][y] == 'S'//购买地块
             || mapData[x][y] == 'K'//靠泊区
             || mapData[x][y] == 'B'//泊位
             || mapData[x][y] == 'c'//交通地块
             || mapData[x][y] == 'T'));//交货点
}

bool GameMap::boatIsAllInMainChannel(Point corePoint, int direction) {
    return boatIsAllInMainChannel_[corePoint.x][corePoint.y][direction];
}

bool GameMap::boatHasOneInMainChannel(Point corePoint, int direction) {
    //核心点和方向，求出是否整艘船都在主航道
    //核心点一定在方向左下角
    return boatHasOneInMainChannel_[corePoint.x][corePoint.y][direction];
}

vector<Point> GameMap::getBoatPoints(Point corePoint, int direction) {
    vector<Point> points;
    points.reserve(BOAT_LENGTH * BOAT_WIDTH);
    for (int i = 0; i < BOAT_LENGTH; i++) {
        points.push_back(corePoint + (DIR[direction] * i));
    }
    Point nextPoint(corePoint);
    if (direction == 0) {
        nextPoint += {1, 0};
    } else if (direction == 1) {
        nextPoint += {-1, 0};
    } else if (direction == 2) {
        nextPoint += {0, 1};
    } else {
        nextPoint += {0, -1};
    }
    for (int i = 0; i < BOAT_LENGTH; i++) {
        points.push_back(nextPoint + (DIR[direction] * i));
    }
    return points;
}

bool GameMap::robotCanReach(int x, int y) {
    return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
            (mapData[x][y] == '.' || mapData[x][y] == '>' || mapData[x][y] == 'R'
             || mapData[x][y] == 'B'
             || mapData[x][y] == 'C'
             || mapData[x][y] == 'c'));//不是海洋或者障碍
}

bool GameMap::boatCanReach(int x, int y, int dir) {
    return isLegalPoint(x, y) && boatCanReach_[x][y][dir];
}

bool GameMap::boatCanReach(Point corePoint, int dir) {
    return boatCanReach(corePoint.x, corePoint.y, dir);
}

int GameMap::getRotationDir(int curDir, int nextDir) {
    static int data[] = {0, 3, 1, 2, 0};
    for (int i = 1; i < sizeof data / sizeof data[0]; i++) {
        if (data[i] == nextDir && data[i - 1] == curDir) {
            return 0;
        }
    }
    for (int i = 1; i < sizeof data / sizeof data[0]; i++) {
        if (data[i - 1] == nextDir && data[i] == curDir) {
            return 1;
        }
    }
    return -1;
}

bool GameMap::isBerthAround(int x, int y) {
    return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
            (mapData[x][y] == 'K'//靠泊区
             || mapData[x][y] == 'B'//泊位
            )
    );
}

bool GameMap::isBerth(int x, int y) {
    return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
            mapData[x][y] == 'B'//泊位
    );
}

int GameMap::boatGetFlashBerthId(int x, int y) {
    return boatAroundBerthId[x][y];
}

void GameMap::updateBerthAndAround(const vector<Point> &AroundPoints, const vector<Point> &corePoints, int berthId) {
    for (const Point &point: AroundPoints) {
        boatAroundBerthId[point.x][point.y] = berthId;
    }
    for (const Point &corePoint: corePoints) {
        partOfBerthId[corePoint.x][corePoint.y] = berthId;
    }
}

int GameMap::getDiscreteBelongToBerthId(int x, int y) {
    assert(x % 2 == 1 && y % 2 == 1);//偶数点是额外添加的点，不是真实点
    return partOfBerthId[x / 2][y / 2];
}

bool GameMap::isRobotMainChannel(int x, int y) {
    return x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
           (mapData[x][y] == '>'//陆地主干道
            || mapData[x][y] == 'R'//机器人购买处
            || mapData[x][y] == 'B'//泊位
            || mapData[x][y] == 'c');//海陆
}

bool GameMap::isRobotDiscreteMainChannel(int x, int y) {
    //最外层是墙壁
    return (x > 0 && x < MAP_DISCRETE_HEIGHT - 1 && y > 0 && y < MAP_DISCRETE_WIDTH - 1 &&
            (robotDiscreteMainChannel[x][y]));//海陆
}

int GameMap::getBelongToBerthId(Point point) {
    return partOfBerthId[point.x][point.y];
}
