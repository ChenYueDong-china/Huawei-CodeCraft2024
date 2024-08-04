//
// Created by sg12q on 2024/3/9.
//

#include "Berth.h"
#include "BoatUtils.h"


void Berth::init(GameMap &gameMap_) {
    this->gameMap = &gameMap_;
    Point start(corePoint);
    queue<Point> q;
    q.push(start);
    berthAroundPoints.push_back(start);
    bool visits[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
    memset(visits, 0, sizeof visits);
    visits[start.x][start.y] = true;
    while (!q.empty()) {
        Point top = q.front();
        q.pop();
        for (int k = 0; k < (sizeof DIR / sizeof DIR[0]) / 2; k++) {
            Point dir = DIR[k];
            int dx = top.x + dir.x;
            int dy = top.y + dir.y;//第一步
            if (!GameMap::isLegalPoint(dx, dy) || visits[dx][dy] || !gameMap_.isBerthAround(dx, dy)) {
                continue; // 不可达或者访问过了
            }
            visits[dx][dy] = true;
            berthAroundPoints.emplace_back(dx, dy);
            q.emplace(dx, dy);
        }
    }

    //判题器漏洞，只能有6个点是泊位,保证泊位一定是2*3的
    for (int i = 0; i < (sizeof DIR / sizeof DIR[0]) / 2; i++) {
        vector<Point> points = GameMap::getBoatPoints(corePoint, i);
        bool allBerth = true;
        for (Point &point: points) {
            if (!gameMap_.isBerth(point.x, point.y)) {
                allBerth = false;
                break;
            }
        }
        if (allBerth) {
            coreDirection = i;
            break;
        }
    }
    berthPoints = GameMap::getBoatPoints(corePoint, coreDirection);
    //机器人路径

    memset(robotMinDistance, 0x7f, sizeof robotMinDistance);

    static Dijkstra dijkstra;
    dijkstra.init(corePoint, gameMap_);
    dijkstra.update(berthPoints, robotMinDistance);

    static BoatDijkstra boatDijkstra;
    boatDijkstra.init(corePoint, gameMap_);
    memset(boatMinDistance, 0x7f, sizeof boatMinDistance);
    boatDijkstra.berthUpdate(berthAroundPoints, corePoint, BERTH_MAX_BOAT_SEARCH_DEEP);
    for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
        for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
            Point point{i, j};
            for (int k = 0; k < (sizeof DIR / sizeof DIR[0]) / 2; k++) {
                if (gameMap->boatCanReach(i, j, k)) {
                    Point point1 = BoatDijkstra::map2RelativePoint(point, k);
                    int dir = (k ^ 1);
                    if (boatDijkstra.minDistance[point1.x][point1.y][dir] == SHORT_INF) {
                        continue;
                    }
                    int deep = (boatDijkstra.minDistance[point1.x][point1.y][dir] >> 2);
                    int lastDir = (boatDijkstra.minDistance[point1.x][point1.y][dir] & 3);
                    lastDir ^= 1;//回溯用
                    boatMinDistance[i][j][k] = (short) ((deep << 2) + lastDir);
                }
            }
        }
    }
    gameMap_.updateBerthAndAround(berthAroundPoints, berthPoints, id);
}

bool Berth::robotCanReach(Point pos) {
    return robotMinDistance[pos.x][pos.y] != SHORT_INF;
}

short Berth::getRobotMinDistance(Point pos) {
    return robotMinDistance[pos.x][pos.y] == SHORT_INF ? SHORT_INF : short(robotMinDistance[pos.x][pos.y] >> 2);
}

bool Berth::inBerth(Point point) const {
    return gameMap->getBelongToBerthId(point) == id;
}

bool Berth::boatCanReach(Point pos, int dir) {
    return boatMinDistance[pos.x][pos.y][dir] != SHORT_INF;
}

short Berth::getBoatMinDistance(Point pos, int dir) {
    return boatMinDistance[pos.x][pos.y][dir] == SHORT_INF ? SHORT_INF : short(boatMinDistance[pos.x][pos.y][dir] >> 2);
}

vector<PointWithDirection> Berth::boatMoveFrom(Point pos, int dir, int recoveryTime, bool comeBreak) {
    vector<PointWithDirection> tmp;
    PointWithDirection t = {pos, dir};
    tmp.push_back(t);
    while ((boatMinDistance[t.point.x][t.point.y][t.direction] >> 2) != 0) {
        //特殊标记过
        if (gameMap->boatGetFlashBerthId(t.point.x, t.point.y) == id) {
            int waitTime = 1 + 2 * (abs(t.point.x - corePoint.x) + abs(t.point.y - corePoint.y));
            if ((boatMinDistance[t.point.x][t.point.y][t.direction] >> 2)
                == waitTime) {
                break;
            }
        }
        if (comeBreak) {
            if (gameMap->boatGetFlashBerthId(t.point.x, t.point.y) == id) {
                break;
            }
        }
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
    //结束点添加
    PointWithDirection end{corePoint, coreDirection};
    PointWithDirection lastTwoPoint = result[result.size() - 1];
    //一帧闪现到达，剩下的是闪现恢复时间
    int waitTime = 1 + 2 * (abs(lastTwoPoint.point.x - end.point.x) + abs(lastTwoPoint.point.y - end.point.y));
    for (int i = 0; i < waitTime; i++) {
        result.push_back(end);
    }
    if (result.size() == 1) {
        printError("error berth start equal end");
        result.push_back(result[0]);
    }
    return result;
}

vector<Point> Berth::robotMoveFrom(Point point) {
    Point source{point};
    vector<Point> result = getRobotPathByCs(robotMinDistance, source);
    if (result.size() == 1) {
        //此时大概率有问题
        printError("error start equal end");
        result.push_back(source);//多加一个
    }
    //细化，转成精细坐标
    return GameMap::toDiscretePath(result);
}
