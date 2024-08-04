//
// Created by sg12q on 2024/4/4.
//
#include "BoatDijkstra.h"
#include "BoatUtils.h"


Point BoatDijkstra::map2RelativePoint(Point corePoint, int dir) {
    if (dir == 0) {
        return corePoint + Point(BOAT_WIDTH - 1, BOAT_LENGTH - 1);
    } else if (dir == 1) {
        return corePoint + Point(BOAT_WIDTH - 1, BOAT_LENGTH - 1) * -1;
    } else if (dir == 2) {
        return corePoint + Point(-(BOAT_LENGTH - 1), BOAT_WIDTH - 1);
    } else {
        return corePoint + Point(-(BOAT_LENGTH - 1), BOAT_WIDTH - 1) * -1;
    }
}


void BoatDijkstra::update(int maxDeep) {
    memset(minDistance, 0x7f, sizeof minDistance);
    //从目标映射的四个点开始搜
    int deep = 0;
    queue<PointWithDirection> q;
    for (int i = 0; i < (sizeof DIR / sizeof DIR[0]) / 2; i++) {
        //单向dfs搜就行
        // 求最短路径
        Point s = map2RelativePoint(mTarget, i);
        if (!GameMap::isLegalPoint(s.x, s.y) || !mGameMap->boatCanReach(s, i ^ 1)) {
            continue;//起始点直接不可达，没得玩
        }
        minDistance[s.x][s.y][i ^ 1] = 0;
        q.emplace(s, i ^ 1);
    }
    vector<PointWithDirection> twoDistancesPoints;
    while (!q.empty() || !twoDistancesPoints.empty()) {
        deep += 1;
        if (deep > maxDeep) {
            break;
        }
        //2距离的下一个点,先保存起来，后面直接插进去
        int size = int(q.size());
        //先加进去
        if (!twoDistancesPoints.empty()) {
            for (const PointWithDirection &searchPoint: twoDistancesPoints) {
                q.push(searchPoint);
            }
            twoDistancesPoints.clear();
        }
        for (int j = 0; j < size; j++) {
            PointWithDirection top = q.front();
            q.pop();
            for (int k = 0; k < 3; k++) {
                PointWithDirection next = BoatUtils::getNextPoint(top, k);
                //合法性判断
                if (!mGameMap->boatCanReach(next.point, next.direction)
                    || deep >= (minDistance[next.point.x][next.point.y][next.direction] >> 2)) {
                    continue;
                }
                //是否到达之后需要恢复,有一个点进入了主航道
                if (mGameMap->boatHasOneInMainChannel(next.point, next.direction)) {
                    if (deep + 1 >= (minDistance[next.point.x][next.point.y][next.direction] >> 2)) {
                        continue;
                    }
                    minDistance[next.point.x][next.point.y][next.direction] = (short) (((deep + 1) << 2) +
                                                                                       top.direction);
                    twoDistancesPoints.push_back(next);
                } else {
                    minDistance[next.point.x][next.point.y][next.direction] = ((short) ((deep << 2) + top.direction));
                    q.push(next);
                }
            }
        }
    }
    adjustDistance();
}

void BoatDijkstra::adjustDistance() {
    for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
        for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
            for (int k = 0; k < (sizeof DIR / sizeof DIR[0]) / 2; k++) {
                Point corePoint{i, j};
                if (!mGameMap->boatCanReach(corePoint, k)) {
                    continue;
                }
                if (!mGameMap->boatHasOneInMainChannel(corePoint, k)
                    &&
                    minDistance[i][j][k] != SHORT_INF) {
                    int deep = minDistance[i][j][k] >> 2;//开始船不在主航道上，距离需要加一，因为到达目标点之后需要等待一帧
                    int dir = minDistance[i][j][k] & 3;
                    deep += 1;
                    minDistance[i][j][k] = (short) ((deep << 2) + dir);
                }
            }
        }
    }
}

void BoatDijkstra::berthUpdate(const vector<Point> &berthAroundPoints, Point berthCorePoint, const int maxDeep) {
    memset(minDistance, 0x7f, sizeof minDistance);
    //从目标映射的四个点开始搜
    int deep = 0;
    queue<PointWithDirection> q;
    queue<PointWithDirection> candidateQueue;
    queue<short> candidateDeep;
    for (const auto &aroundPoint: berthAroundPoints) {
        auto flashDistance = (short) (1 +
                                      2 * (abs(berthCorePoint.x - aroundPoint.x) +
                                           abs(berthCorePoint.y - aroundPoint.y)));
        for (int i = 0; i < (sizeof DIR / sizeof DIR[0]) / 2; i++) {
            //单向dfs搜就行
            // 求最短路径
            Point s = map2RelativePoint(aroundPoint, i);
            if (!GameMap::isLegalPoint(s.x, s.y) || !mGameMap->boatCanReach(s, i ^ 1)) {
                continue;//起始点直接不可达，没得玩
            }
            minDistance[s.x][s.y][i ^ 1] = (short) (flashDistance << 2);
            PointWithDirection pointWithDirection{s, i ^ 1};
            candidateQueue.push(pointWithDirection);
            candidateDeep.push(flashDistance);
        }
    }

    vector<PointWithDirection> twoDistancesPoints;
    while (!candidateQueue.empty() || !q.empty() || !twoDistancesPoints.empty()) {
        deep += 1;
        if (deep > maxDeep) {
            break;
        }
        //2距离的下一个点,先保存起来，后面直接插进去
        int size = int(q.size());
        //先加进去
        if (!twoDistancesPoints.empty()) {
            for (const PointWithDirection &searchPoint: twoDistancesPoints) {
                q.push(searchPoint);
            }
            twoDistancesPoints.clear();
        }
        while (!candidateQueue.empty()) {
            assert (!candidateDeep.empty());
            short startDeep = candidateDeep.front();
            if (startDeep == deep) {
                q.push(candidateQueue.front());
                candidateQueue.pop();
                candidateDeep.pop();
            } else {
                break;
            }
        }
        for (int j = 0; j < size; j++) {
            PointWithDirection top = q.front();
            q.pop();
            for (int k = 0; k < 3; k++) {
                PointWithDirection next = BoatUtils::getNextPoint(top, k);
                //合法性判断
                if (!mGameMap->boatCanReach(next.point, next.direction)
                    || deep >= (minDistance[next.point.x][next.point.y][next.direction] >> 2)) {
                    continue;
                }
                //是否到达之后需要恢复,有一个点进入了主航道
                if (mGameMap->boatHasOneInMainChannel(next.point, next.direction)) {
                    if (deep + 1 >= (minDistance[next.point.x][next.point.y][next.direction] >> 2)) {
                        continue;
                    }
                    minDistance[next.point.x][next.point.y][next.direction] = (short) (((deep + 1) << 2) +
                                                                                       top.direction);
                    twoDistancesPoints.push_back(next);
                } else {
                    minDistance[next.point.x][next.point.y][next.direction] = ((short) ((deep << 2) + top.direction));
                    q.push(next);
                }
            }
        }
    }
    adjustDistance();
}



