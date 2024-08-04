//
// Created by sg12q on 2024/4/5.
//

#pragma once

#include <algorithm>
#include <queue>
#include <stack>
#include "GameMap.h"
#include "Utils.h"

using std::stack;
using std::min;

struct RobotUtils {

    static vector<Point>
    robotMoveToPointHeuristic(GameMap &gameMap, Point start, Point end, const vector<vector<Point>> &otherPaths,
                              const int heuristicCs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
        start = GameMap::posToDiscrete(start);
        end = GameMap::posToDiscrete(end);
        if (start == end) {
            //输出路径
            vector<Point> result;
            result.reserve(3);
            for (int i = 0; i < 3; i++) {
                result.push_back(start);//3个,一次两步
            }
            return result;
        }
        //发现一条更好的路径,启发式搜
        struct Pair {
            int deep;
            Point p;

            Pair(int deep, Point p) {
                this->deep = deep;
                this->p = p;
            }
        };
        auto discreteCs = gameMap.robotCommonDiscreteCs; //前面2位是距离，后面的位数是距离0xdistdir
        for (int i = 0; i < MAP_DISCRETE_HEIGHT; i++) {
            for (int j = 0; j < MAP_DISCRETE_WIDTH; j++) {
                discreteCs[i][j] = INT_INF;
            }
        }

        stack<Pair> st;
        st.emplace(0, start);
        discreteCs[start.x][start.y] = 0;
        while (!st.empty()) {
            Point top = st.top().p;
            if (top == end) {
                //回溯路径
                vector<Point> result = {};
                reverse(result.begin(), result.end());
                return result;
            }
            int deep = st.top().deep;
            st.pop();
            for (int i = 0; i < sizeof DIR / sizeof DIR[0] / 2; i++) {
                //四方向的
                Point point = getNextPoint(gameMap, otherPaths, discreteCs, top, i, deep, heuristicCs);
                if (point.x == INT_INF || point.y == INT_INF) continue;
                st.emplace(deep + 2, point);
            }
        }
        //搜不到，走原路
        return {};
    }

    static vector<Point>
    robotMoveToBerth(GameMap &gameMap, Point start, int berthId, int maxDeep,
                     const vector<vector<Point>> &otherPaths = {}) {
        assert(berthId != -1);
        return robotMoveToPoint(gameMap, start, Point(-1, -1), maxDeep, otherPaths, berthId);
    }

    static vector<Point> robotMoveToPoint(GameMap &gameMap, Point start, Point end, int maxDeep,
                                          const vector<vector<Point>> &otherPaths, int berthId = -1) {
        //已经在了，直接返回，防止cs消耗
        start = GameMap::posToDiscrete(start);
        end = GameMap::posToDiscrete(end);
        if ((berthId == -1 && start == end) ||
            (berthId != -1 && gameMap.getDiscreteBelongToBerthId(start.x, start.y) == berthId)) {
            //输出路径
            vector<Point> result;
            result.reserve(3);
            for (int i = 0; i < 3; i++) {
                result.push_back(start);//3个
            }
            return result;
        }
        auto cs = gameMap.robotCommonDiscreteCs;
        for (int i = 0; i < MAP_DISCRETE_HEIGHT; i++) {
            for (int j = 0; j < MAP_DISCRETE_WIDTH; j++) {
                cs[i][j] = INT_INF;
            }
        }
        //从目标映射的四个点开始搜
        Point s(start);
        queue<Point> q;
        q.emplace(s);
        cs[s.x][s.y] = 0;
        int deep = 0;
        int count = 0;
        while (!q.empty()) {
            if (deep > maxDeep) {
                break;
            }
            int size = int(q.size());
            for (int i = 0; i < size; i++) {
                count++;
                Point top = q.front();
                q.pop();
                if ((berthId == -1 && top == end) ||
                    (berthId != -1 && gameMap.getDiscreteBelongToBerthId(top.x, top.y) == berthId)) {
                    //回溯路径
                    vector<Point> result = {};
                    reverse(result.begin(), result.end());
                    return result;
                }
                for (int j = 0; j < sizeof DIR / sizeof DIR[0] / 2; j++) {
                    //四方向的
                    Point point = getNextPoint(gameMap, otherPaths, cs, top, j, deep, nullptr);
                    if (point.x == INT_INF || point.y == INT_INF) continue;
                    q.emplace(point);
                }
            }
            deep += 2;
        }
        return {};
    }


    static Point
    getNextPoint(GameMap &gameMap, const vector<vector<Point>> &otherPaths,
                 int discreteCs[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH], Point top, int index,
                 int deep, const int heuristicCs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
        int lastDirIdx = discreteCs[top.x][top.y] & 3;
        int dirIdx = index ^ lastDirIdx; // 优先遍历上一次过来的方向
        Point dir = DIR[dirIdx];
        int dx = top.x + dir.x;
        int dy = top.y + dir.y;//第一步
        if (discreteCs[dx][dy] != INT_INF || !gameMap.robotCanReachDiscrete(dx, dy)) {
            return {INT_INF, INT_INF};
        }
        if (discreteCs[dx + dir.x][dy + dir.y] != INT_INF
            || !gameMap.robotCanReachDiscrete(dx + dir.x, dy + dir.y)
                ) {
            return {INT_INF, INT_INF};
        }
        if (robotCheckCrashInDeep(gameMap, deep + 1, dx, dy, otherPaths)
            || robotCheckCrashInDeep(gameMap, deep + 2, dx + dir.x, dy + dir.y, otherPaths)) {
            return {INT_INF, INT_INF};
        }
        if (heuristicCs != nullptr) {
            Point cur = GameMap::discreteToPos(top.x, top.y);
            Point next = GameMap::discreteToPos(dx + dir.x, dy + dir.y);
            if ((heuristicCs[cur.x][cur.y] >> 2) != (heuristicCs[next.x][next.y] >> 2) + 1) {
                //启发式剪枝，不是距离更近则直接结束
                return {INT_INF, INT_INF};
            }
        }
        discreteCs[dx][dy] = ((deep + 1) << 2) + dirIdx;//第一步
        dx += dir.x;
        dy += dir.y;
        discreteCs[dx][dy] = ((deep + 2) << 2) + dirIdx;//第一步
        return {dx, dy};
    }


    static vector<Point>
    robotMoveToPointBerthHeuristicCs(GameMap &gameMap, Point start, int berthId, Point end, int maxDeep,
                                     const vector<vector<Point>> &otherPaths,
                                     short heuristicCs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS],
                                     bool conflictPoints[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
        //已经在了，直接返回，防止cs消耗
        Point s = start;
        gameMap.curVisitId++;
        int curVisitId = gameMap.curVisitId;
        int (*visits)[MAP_FILE_COL_NUMS] = gameMap.robotVisits;
        short (*cs)[MAP_FILE_COL_NUMS] = gameMap.robotCommonCs;
        struct PointWithDeep {
            Point point;
            int deep;
        };
        stack<PointWithDeep> tmp;
        tmp.push({s, 0});
        cs[s.x][s.y] = 0;
        visits[s.x][s.y] = curVisitId;
        int curDeep = (heuristicCs[s.x][s.y] >> 2);
        map<int, stack<PointWithDeep>> cacheMap;
        cacheMap[curDeep] = tmp;

        int count = 0;
        //从目标映射的四个点开始搜
        while (!cacheMap.empty()) {
            int totalDeep = cacheMap.begin()->first;
            if (totalDeep > maxDeep || count > MAP_FILE_ROW_NUMS * MAP_FILE_COL_NUMS / 8) {
                break;
            }
            stack<PointWithDeep> &st = cacheMap.begin()->second;
            PointWithDeep point = st.top();
            st.pop();
            int deep = point.deep;//最多8万个点,这个不是启发式，会一直搜

            Point top = point.point;
            bool arrive = false;
            if (berthId != -1) {
                if (gameMap.getBelongToBerthId(top) == berthId) {
                    //回溯路径
                    arrive = true;
                }
            } else {
                if (top == end) {
                    //回溯路径
                    arrive = true;
                }
            }
            if (arrive) {
                vector<Point> result = getRobotPathByCs(cs, top);
                std::reverse(result.begin(), result.end());
                if (result.size() == 1) {
                    result.push_back(result[0]);
                }
                return GameMap::toDiscretePath(result);
            }
            Point pre = GameMap::posToDiscrete(top);
            for (int j = 4 - 1; j >= 0; j--) {
                //四方向的
                count++;
                Point dir = DIR[j];
                int dx = top.x + dir.x;
                int dy = top.y + dir.y;//第一步
                if (!gameMap.robotCanReach(dx, dy) || visits[dx][dy] == curVisitId) {
                    //不会发生后面有更近点得可能
                    continue;
                }
                if (conflictPoints != nullptr) {
                    if (conflictPoints[dx][dy] && !gameMap.isRobotMainChannel(dx, dy)) {
                        continue;//这个点被锁死了
                    }
                }
                //转成离散去避让

                if (!otherPaths.empty()) {
                    Point next = GameMap::posToDiscrete(dx, dy);
                    Point mid = pre + dir;
                    int midDeep = 2 * (deep + 1) - 1;
                    int nextDeep = 2 * (deep + 1);
                    if (robotCheckCrashInDeep(gameMap, midDeep, mid.x, mid.y, otherPaths)
                        || robotCheckCrashInDeep(gameMap, nextDeep, next.x, next.y, otherPaths)) {
                        continue;
                    }
                }
                cs[dx][dy] = (short) (((deep + 1) << 2) + j);
                visits[dx][dy] = curVisitId;
                int nextDeep = deep + 1;
                Point next{dx, dy};
                PointWithDeep pointWithDeep{next, nextDeep};
                nextDeep += (heuristicCs[next.x][next.y] >> 2);
                if (!cacheMap.count(nextDeep)) {
                    cacheMap[nextDeep] = {};
                }
                cacheMap[nextDeep].push(pointWithDeep);
            }
            if (st.empty()) {
                cacheMap.erase(totalDeep);
            }
        }
        return {};
    }

    static bool
    robotCheckCrashInDeep(GameMap &gameMap, int deep, int x, int y, const vector<vector<Point>> &otherPaths) {
        if (!otherPaths.empty()) {
            for (const vector<Point> &otherPath: otherPaths) {
                if (deep < otherPath.size() && otherPath[deep] == Point(x, y)
                    && !gameMap.isRobotDiscreteMainChannel(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    static bool
    robotCheckCrash(GameMap &gameMap, const vector<Point> &myPath, const vector<vector<Point>> &otherPaths) {
        //默认检测整条路径
        for (const vector<Point> &otherPath: otherPaths) {
            int detectDistance = (int) min(otherPath.size(), myPath.size());
            for (int j = 0; j < detectDistance; j++) {
                Point point = otherPath[j];
                if (point == myPath[j] && !gameMap.isRobotDiscreteMainChannel(point.x, point.y)) {
                    return true;
                }
            }
        }
        return false;
    }
};