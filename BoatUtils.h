//
// Created by sg12q on 2024/4/4.
//
#pragma once

#include <algorithm>
#include <queue>
#include <stack>
#include <map>
#include "GameMap.h"
#include "Utils.h"

using std::queue;
using std::stack;
using std::map;

struct BoatUtils {

    static vector<PointWithDirection>
    boatFixedPath(GameMap &gameMap, int recoveryTime, const vector<PointWithDirection> &result) {
        vector<PointWithDirection> tmp;
        tmp.push_back(result[0]);
        for (int i = 0; i < recoveryTime; i++) {
            tmp.push_back(result[0]);
        }
        for (int i = 1; i < result.size(); i++) {
            if (gameMap.boatHasOneInMainChannel(result[i].point, result[i].direction)) {
                tmp.push_back(result[i]);
            }
            tmp.push_back(result[i]);
        }
        return tmp;
    }

    static vector<PointWithDirection>
    boatGetSafePoints(GameMap &gameMap,
                      short cs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][(sizeof DIR / sizeof DIR[0]) / 2],
                      PointWithDirection start,
                      const bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_ROW_NUMS],
                      const bool noResultPoints[MAP_FILE_ROW_NUMS][MAP_FILE_ROW_NUMS], int maxResultCount) {
        //从目标映射的四个点开始搜
        //主航道点解除冲突和非结果
        gameMap.curVisitId++;
        int curVisitId = gameMap.curVisitId;
        int (*visits)[MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2] = gameMap.boatVisits;
        PointWithDirection s{Point(start.point), start.direction};
        queue<PointWithDirection> q;
        q.emplace(s);
        int deep = 0;
        cs[s.point.x][s.point.y][s.direction] = (short) s.direction;
        visits[s.point.x][s.point.y][s.direction] = curVisitId;
        vector<PointWithDirection> twoDistancesPoints;
        vector<PointWithDirection> result;
        while (!q.empty() || !twoDistancesPoints.empty()) {
            deep += 1;
            //2距离的下一个点,先保存起来，后面直接插进去
            int size = (int) q.size();
            //先加进去
            if (!twoDistancesPoints.empty()) {
                for (const PointWithDirection twoDistancesPoint: twoDistancesPoints) {
                    q.push(twoDistancesPoint);
                }
                twoDistancesPoints.clear();
            }
            for (int j = 0; j < size; j++) {
                if (result.size() > maxResultCount) {
                    return result;
                }
                PointWithDirection top = q.front();
                q.pop();
                if (noResultPoints != nullptr) {
                    //输出路径
                    bool crash = checkIfExcludePoint(gameMap, noResultPoints, top);
                    if (!crash) {
                        result.push_back(top);
                    }
                }
                for (int k = 0; k < 3; k++) {
                    PointWithDirection next = getNextPoint(top, k);
                    if (!gameMap.boatCanReach(next.point, next.direction) ||
                        (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                         deep >= (cs[next.point.x][next.point.y][next.direction] >> 2))) {
                        continue;
                    }

                    //判断冲突点
                    if (conflictPoints != nullptr) {
                        bool crash = checkIfExcludePoint(gameMap, conflictPoints, next);
                        if (crash) {
                            continue;
                        }
                    }
                    //是否到达之后需要恢复,有一个点进入了主航道
                    if (gameMap.boatHasOneInMainChannel(next.point, next.direction)) {
                        if (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                            deep + 1 >= (cs[next.point.x][next.point.y][next.direction] >> 2)) {
                            continue;//visit过且深度大于已有的,剪枝
                        }
                        cs[next.point.x][next.point.y][next.direction]
                                = (short) (((deep + 1) << 2) + top.direction);
                        twoDistancesPoints.push_back(next);
                    } else {
                        cs[next.point.x][next.point.y][next.direction]
                                = (short) ((deep << 2) + top.direction);
                        q.push(next);
                    }
                    visits[next.point.x][next.point.y][next.direction] = curVisitId;
                }
            }
        }
        return result;
    }

    static bool checkIfExcludePoint(GameMap &gameMap, const bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_ROW_NUMS],
                                    const PointWithDirection &top) {
        vector<Point> points = GameMap::getBoatPoints(top.point, top.direction);
        return any_of(points.begin(), points.end(), [&conflictPoints, &gameMap]
                (const Point &point) -> bool {
            return conflictPoints[point.x][point.y] && !gameMap.isRobotMainChannel(point.x, point.y);
        });


    }

    static int
    boatCheckCrash(GameMap &gameMap, int myId, vector<PointWithDirection> &myPaths,
                   vector<vector<PointWithDirection>> &otherPaths, vector<int> &otherIds, int maxDetectDistance) {
        for (int i = 0; i < otherPaths.size(); i++) {
            vector<PointWithDirection> &otherPath = otherPaths[i];
            int otherId = otherIds[i];
            assert (myId != otherId);
            for (int deep = 1; deep < myPaths.size(); deep++) {
                //从我的移动后的点开始
                PointWithDirection &pointWithDirection = myPaths[deep];
                if (deep < otherPath.size()) {
                    bool crash = boatCheckCrash(gameMap, pointWithDirection, otherPath[deep]);
                    if (crash) {
                        assert(boatCheckCrashInDeep(gameMap, deep, myId, pointWithDirection, otherPaths, otherIds));
                        return otherId;
                    }
                }
                if (otherId < myId) {
                    //我的这帧撞他的这帧，他的下一位置撞我的这位置
                    if (deep + 1 < otherPath.size()) {
                        bool crash = boatCheckCrash(gameMap, pointWithDirection, otherPath[deep + 1]);
                        if (crash) {
                            assert(boatCheckCrashInDeep(gameMap, deep, myId, pointWithDirection, otherPaths, otherIds));
                            return otherId;
                        }
                    }
                } else {
                    //我的这帧撞他的前一帧，他的这一帧撞我的这帧
                    if (deep - 1 < otherPath.size()) {
                        bool crash = boatCheckCrash(gameMap, pointWithDirection, otherPath[deep - 1]);
                        if (crash) {
                            assert(boatCheckCrashInDeep(gameMap, deep, myId, pointWithDirection, otherPaths, otherIds));
                            return otherId;
                        }
                    }
                }

                if (2 * deep > maxDetectDistance) {
                    //假设一人走一步，则需要乘以两倍
                    break;
                }
            }
        }
        return -1;
    }


    static Point getLastPoint(Point curPoint, int curDir, int lastDir) {
        //判断是否顺
        bool clockwise = GameMap::getRotationDir(lastDir, curDir) == 0;
        PointWithDirection pointWithDirection = getBoatRotationPoint(PointWithDirection(Point(0, 0), lastDir),
                                                                     clockwise);
        assert(pointWithDirection.direction == curDir);
        return curPoint + (pointWithDirection.point * -1);
    }

    static PointWithDirection
    getLastPointWithDirection(vector<PointWithDirection> &result, PointWithDirection t, int lastDir) {
        Point lastPoint;
        if (lastDir == t.direction) {
            //前进
            lastPoint = t.point + (DIR[lastDir ^ 1]);
        } else {
            //旋转或者不旋转
            lastPoint = getLastPoint(t.point, t.direction, lastDir);
        }
        t = PointWithDirection(lastPoint, lastDir);
        result.push_back(t);
        return t;
    }

    inline static PointWithDirection getNextPoint(const PointWithDirection &top, int k) {
        if (k == 0) {
            return {top.point + DIR[top.direction], top.direction};
        } else if (k == 1) {
            return getBoatRotationPoint(top, true);
        } else {
            return getBoatRotationPoint(top, false);
        }
    }

    static vector<PointWithDirection>
    boatMoveToBerthSellPoint(GameMap &gameMap, PointWithDirection start, PointWithDirection end, int berthId,
                             PointWithDirection berthCorePoint, int recoveryTime, int maxDeep, int curBoatId,
                             const vector<vector<PointWithDirection>> &otherPaths, const vector<int> &otherIds,
                             short heuristicCs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2],
                             bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
        short (*cs)[MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2] = gameMap.boatCommonCs;
        gameMap.curVisitId++;
        int curVisitId = gameMap.curVisitId;
        int (*visits)[MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2] = gameMap.boatVisits;
        //从目标映射的四个点开始搜
        int deep = 0;
        PointWithDirection s{Point(start.point), start.direction};
        queue<PointWithDirection> q;
        q.emplace(s);
        cs[s.point.x][s.point.y][s.direction] = short(s.direction);
        visits[s.point.x][s.point.y][s.direction] = curVisitId;
        vector<PointWithDirection> twoDistancesPoints;
        PointWithDirection bestPoint{{}, -1};
        int bestDeep = INT_INF;
        while (!q.empty() || !twoDistancesPoints.empty()) {
            if (deep > maxDeep) {
                break;
            }
            deep += 1;
            //2距离的下一个点,先保存起来，后面直接插进去
            int size = int(q.size());
            //先加进去
            if (!twoDistancesPoints.empty()) {
                for (const PointWithDirection &twoDistancesPoint: twoDistancesPoints) {
                    q.push(twoDistancesPoint);
                }
                twoDistancesPoints.clear();
            }
            for (int j = 0; j < size; j++) {
                PointWithDirection top = q.front();
                q.pop();
                if (heuristicCs != nullptr &&
                    deep - 1 + (heuristicCs[top.point.x][top.point.y][top.direction] >> 2) > maxDeep) {
                    continue;//启发式剪枝
                }
                if (berthId != -1) {
                    if (gameMap.boatGetFlashBerthId(top.point.x, top.point.y) == berthId) {
                        int nextDeep = 1 + 2 * (abs(top.point.x - berthCorePoint.point.x) +
                                                abs(top.point.y - berthCorePoint.point.y));
                        int curDeep = deep - 1 + nextDeep;
                        assert (heuristicCs != nullptr);
                        assert(curDeep >= (heuristicCs[start.point.x][start.point.y][start.direction] >> 2));
                        if (deep + nextDeep <= maxDeep && curDeep < bestDeep) {
                            //否则继续往前走
                            bestPoint = top;
                            bestDeep = curDeep;
                        }
                    }
                } else {
                    if (top == end || (top.point == end.point && end.direction == -1)) {
                        //回溯路径
                        bestPoint = top;
                        break;
                    }
                }
                for (int k = 0; k < 3; k++) {
                    PointWithDirection next = getNextPoint(top, k);
                    //合法性判断
                    if (!gameMap.boatCanReach(next.point, next.direction) ||
                        (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                         deep >= (cs[next.point.x][next.point.y][next.direction] >> 2))
                            ) {
                        continue;
                    }
                    //检测碰撞。
                    if (!otherPaths.empty()) {
                        if (boatCheckCrashInDeep(gameMap, recoveryTime + deep, curBoatId, next, otherPaths, otherIds))
                            continue;
                    }

                    //判断冲突点
                    if (conflictPoints != nullptr) {
                        bool crash = checkIfExcludePoint(gameMap, conflictPoints, next);
                        if (crash) {
                            continue;
                        }
                    }

                    //是否到达之后需要恢复,有一个点进入了主航道
                    if (gameMap.boatHasOneInMainChannel(next.point, next.direction)) {
                        if (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                            deep + 1 >= (cs[next.point.x][next.point.y][next.direction] >> 2)) {
                            continue;//visit过且深度大于已有的,剪枝
                        }
                        //检测碰撞。
                        if (!otherPaths.empty()) {
                            if (boatCheckCrashInDeep(gameMap, recoveryTime + deep + 1, curBoatId, next, otherPaths,
                                                     otherIds))
                                continue;
                        }
                        cs[next.point.x][next.point.y][next.direction]
                                = (short) (((deep + 1) << 2) + top.direction);
                        twoDistancesPoints.push_back(next);
                    } else {
                        cs[next.point.x][next.point.y][next.direction]
                                = (short) ((deep << 2) + top.direction);
                        q.push(next);
                    }
                    visits[next.point.x][next.point.y][next.direction] = curVisitId;
                }
            }
            if (bestPoint.direction != -1) {
                //回溯路径
                vector<PointWithDirection> path;
                if (berthId != -1) {
                    path = getBoatToBerthBackPath(gameMap, berthCorePoint, recoveryTime, bestPoint, cs);
                } else {
                    path = backTrackPath(gameMap, bestPoint, cs, recoveryTime);
                }
                if (path.size() == 1) {
                    path.push_back(path[0]);
                }
                return path;
            }
        }

        return {};
    }


    static vector<PointWithDirection>
    boatMoveToBerthSellPointHeuristic(GameMap &gameMap, PointWithDirection start, PointWithDirection end, int berthId,
                                      PointWithDirection berthCorePoint, int recoveryTime, int maxDeep, int curBoatId,
                                      const vector<vector<PointWithDirection>> &otherPaths, const vector<int> &otherIds,
                                      short heuristicCs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][sizeof DIR /
                                                                                              sizeof DIR[0] / 2],
                                      bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
        short (*cs)[MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2] = gameMap.boatCommonCs;
        gameMap.curVisitId++;
        int curVisitId = gameMap.curVisitId;
        int (*visits)[MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2] = gameMap.boatVisits;
        //从目标映射的四个点开始搜
        struct PointWithDeep {
            PointWithDirection point;
            int deep;
        };
        PointWithDeep s{{Point(start.point), start.direction}, 0};
        stack<PointWithDeep> tmp;
        tmp.push(s);
        cs[s.point.point.x][s.point.point.y][s.point.direction] = (short) s.point.direction;
        visits[s.point.point.x][s.point.point.y][s.point.direction] = curVisitId;
        int curDeep = (heuristicCs[s.point.point.x][s.point.point.y][s.point.direction] >> 2) + s.deep;
        map<int, stack<PointWithDeep>> cacheMap;
        cacheMap[curDeep] = tmp;
        PointWithDirection bestPoint{{}, -1};
        int count = 0;
        while (!cacheMap.empty()) {
            int totalDeep = cacheMap.begin()->first;
            stack<PointWithDeep> &st = cacheMap.begin()->second;
            PointWithDeep point = st.top();
            st.pop();
            int deep = point.deep;//最多2万个点,这个不是启发式，会一直搜
            if (totalDeep > maxDeep || count > MAP_FILE_ROW_NUMS * 5) {
                //最多搜4k个点
                break;
            }
            PointWithDirection top = point.point;
            if (visits[top.point.x][top.point.y][top.direction] == curVisitId &&
                point.deep > (cs[top.point.x][top.point.y][top.direction] >> 2)) {
                if (st.empty()) {
                    cacheMap.erase(totalDeep);
                }
                continue;
            }
            //2距离的下一个点,先保存起来，后面直接插进去
            if (berthId != -1) {
                if (gameMap.boatGetFlashBerthId(top.point.x, top.point.y) == berthId) {
                    int nextDeep = 1 + 2 * (abs(top.point.x - berthCorePoint.point.x) +
                                            abs(top.point.y - berthCorePoint.point.y));
                    assert (deep + nextDeep >= (heuristicCs[start.point.x][start.point.y][start.direction] >> 2));
                    if (deep + nextDeep <= maxDeep) {
                        //否则继续往前走
                        bestPoint = top;
                        break;
                    }
                }
            } else {
                if (top == end || (top.point == end.point && end.direction == -1)) {
                    //回溯路径
                    bestPoint = top;
                    break;
                }
            }
            deep += 1;
            for (int k = 2; k >= 0; k--) {
                count++;
                PointWithDirection next = getNextPoint(top, k);
                //合法性判断
                if (!gameMap.boatCanReach(next.point, next.direction) ||
                    (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                     deep >= (cs[next.point.x][next.point.y][next.direction] >> 2))) {
                    continue;
                }
                //检测碰撞。
                if (!otherPaths.empty()) {
                    if (boatCheckCrashInDeep(gameMap, recoveryTime + deep, curBoatId, next, otherPaths, otherIds))
                        continue;
                }

                //判断冲突点
                if (conflictPoints != nullptr) {
                    bool crash = checkIfExcludePoint(gameMap, conflictPoints, next);
                    if (crash) {
                        continue;
                    }
                }
                //是否到达之后需要恢复,有一个点进入了主航道
                int nextDeep = deep;
                if (gameMap.boatHasOneInMainChannel(next.point, next.direction)) {
                    if (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                        deep + 1 >= (cs[next.point.x][next.point.y][next.direction] >> 2)) {
                        continue;//visit过且深度大于已有的,剪枝
                    }
                    //检测碰撞。
                    if (!otherPaths.empty()) {
                        if (boatCheckCrashInDeep(gameMap, recoveryTime + deep + 1, curBoatId, next, otherPaths,
                                                 otherIds))
                            continue;
                    }
                    cs[next.point.x][next.point.y][next.direction]
                            = (short) (((deep + 1) << 2) + top.direction);
                    nextDeep += 1;
                } else {
                    cs[next.point.x][next.point.y][next.direction]
                            = (short) ((deep << 2) + top.direction);
                }
                visits[next.point.x][next.point.y][next.direction] = curVisitId;
                PointWithDeep pointWithDeep{next, nextDeep};
                nextDeep += (heuristicCs[next.point.x][next.point.y][next.direction] >> 2);
                if (nextDeep <= maxDeep) {
                    if (!cacheMap.count(nextDeep)) {
                        cacheMap[nextDeep] = {};
                    }
                    cacheMap[nextDeep].push(pointWithDeep);
                }
            }
            if (st.empty()) {
                cacheMap.erase(totalDeep);
            }
        }
        if (bestPoint.direction != -1) {
            //回溯路径
            vector<PointWithDirection> path;
            if (berthId != -1) {
                path = getBoatToBerthBackPath(gameMap, berthCorePoint, recoveryTime, bestPoint, cs);
            } else {
                path = backTrackPath(gameMap, bestPoint, cs, recoveryTime);
            }
            if (path.size() == 1) {
                path.push_back(path[0]);
            }
            return path;
        }
        return {};
    }


    static vector<PointWithDirection> backTrackPath(GameMap &gameMap, PointWithDirection &top,
                                                    short cs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][
                                                    (sizeof DIR / sizeof DIR[0]) / 2], int recoveryTime) {
        vector<PointWithDirection> result;
        PointWithDirection t = top;
        result.push_back(t);
        while ((cs[t.point.x][t.point.y][t.direction] >> 2) != 0) {
            int lastDir = cs[t.point.x][t.point.y][t.direction] & 3;
            t = getLastPointWithDirection(result, t, lastDir);
        }
        reverse(result.begin(), result.end());
        vector<PointWithDirection> tmp;
        tmp.reserve(result.size() + recoveryTime);
        tmp.push_back(result[0]);
        for (int i = 0; i < recoveryTime; i++) {
            tmp.push_back(result[0]);
        }
        for (int i = 1; i < result.size(); i++) {
            if (gameMap.boatHasOneInMainChannel(result[i].point, result[i].direction)) {
                tmp.push_back(result[i]);
            }
            tmp.push_back(result[i]);
        }
        result = std::move(tmp);
        return result;
    }


    static bool
    boatCheckCrash(GameMap &gameMap, const PointWithDirection &myPoint, const PointWithDirection &otherPoint) {
        //是否有点重合
        vector<Point> myPoints = GameMap::getBoatPoints(myPoint.point, myPoint.direction);
        vector<Point> othersPoints = GameMap::getBoatPoints(otherPoint.point, otherPoint.direction);
        for (Point &point: myPoints) {
            for (Point &othersPoint: othersPoints) {
                if (point == othersPoint && !gameMap.isBoatMainChannel(point.x, point.y)) {
                    return true;
                }
            }
        }
        return false;
    }

    static vector<PointWithDirection>
    getBoatToBerthBackPath(GameMap &gameMap, PointWithDirection berthCorePoint, int recoveryTime,
                           PointWithDirection minMidPoint,
                           short cs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][sizeof DIR / sizeof DIR[0] / 2]) {
        //回溯路径
        vector<PointWithDirection> path = backTrackPath(gameMap, minMidPoint, cs, recoveryTime);
        //结束点添加
        PointWithDirection end = PointWithDirection(berthCorePoint.point, berthCorePoint.direction);
        PointWithDirection lastTwoPoint = path.back();
        //一帧闪现到达，剩下的是闪现恢复时间
        int waitTime = 1 + 2 * (abs(lastTwoPoint.point.x - end.point.x) + abs(lastTwoPoint.point.y - end.point.y));
        for (int i = 0; i < waitTime; i++) {
            path.push_back(end);
        }
        return path;
    }

    static vector<PointWithDirection>
    getBoatToBerthEqualPath(PointWithDirection start, PointWithDirection berthCorePoint, int recoveryTime) {
        vector<PointWithDirection> result;
        result.push_back(start);
        for (int i = 0; i < recoveryTime; i++) {
            result.push_back(start);
        }
        //闪现一帧,如果本来就在也不会执行，多一帧也没啥
        PointWithDirection end = PointWithDirection(berthCorePoint.point, berthCorePoint.direction);
        result.push_back(end);
        return result;
    }

    static bool boatCheckCrashInDeep(GameMap &gameMap, int deep, int boatId, PointWithDirection point,
                                     const vector<vector<PointWithDirection>> &otherPaths, const vector<int> &otherIds
    ) {
        bool crash = false;
        for (int i = 0; i < otherPaths.size(); i++) {
            int otherId = otherIds[i];
            const vector<PointWithDirection> &otherPath = otherPaths[i];
            if (deep < otherPath.size()) {
                crash = boatCheckCrash(gameMap, point, otherPath[deep]);
                if (crash) {
                    break;
                }
            }
            if (otherId < boatId) {
                //我的这帧撞他的这帧，他的下一位置撞我的这位置
                if (deep + 1 < otherPath.size()) {
                    crash = boatCheckCrash(gameMap, point, otherPath[deep + 1]);
                    if (crash) {
                        break;
                    }
                }
            } else {
                //我的这帧撞他的前一帧，他的这一帧撞我的这帧
                if (deep - 1 < otherPath.size()) {
                    crash = boatCheckCrash(gameMap, point, otherPath[deep - 1]);
                    if (crash) {
                        break;
                    }
                }
            }
        }
        return crash;
    }

    //todo 船搜一条不撞对面的路径

};