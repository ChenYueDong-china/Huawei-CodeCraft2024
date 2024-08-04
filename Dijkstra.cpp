//
// Created by sg12q on 2024/3/9.
//
#include "Dijkstra.h"


void Dijkstra::update(const vector<Point> &berthPoints, short cs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
    //单向dfs搜就行
    int deep = 0;
    queue<Point> q;
    for (const Point &point: berthPoints) {
        q.push(point);
        cs[point.x][point.y] = 0;
    }
    int count = 0;
    while (!q.empty()) {
        int size = (int) q.size();
        deep += 1;
        //这一层出来的深度都一样
        for (int i = 0; i < size; i++) {
            ++count;
            Point top = q.front();
            q.pop();
            for (int j = 0; j < (sizeof(DIR) / sizeof(DIR[0])) / 2; j++) {
                //四方向的
                int lastDirIdx = cs[top.x][top.y] & 3;
                int dirIdx = j ^ lastDirIdx; // 优先遍历上一次过来的方向
                Point dir = DIR[dirIdx];
                int dx = top.x + dir.x;
                int dy = top.y + dir.y;//第一步
                if (!mGameMap->robotCanReach(dx, dy) || cs[dx][dy] != SHORT_INF) {
                    continue; // 不可达或者访问过了
                }
                cs[dx][dy] = short((deep << 2) + dirIdx);//第一步家的优先级大于移位
                q.emplace(dx, dy);
            }
        }
    }
}

void Dijkstra::update(int maxDeep, short cs[MAP_FILE_ROW_NUMS / 2 + 1][MAP_FILE_COL_NUMS / 2 + 1]) {
    int leftTopX = mTarget.x - MAP_FILE_ROW_NUMS / 4;
    int leftTopY = mTarget.y - MAP_FILE_COL_NUMS / 4;
    int fakeX = mTarget.x - leftTopX;
    int fakeY = mTarget.y - leftTopY;
    Point s = {fakeX, fakeY};

    //单向dfs搜就行
    short deep = 0;
    queue<Point> q;
    q.push(s);
    cs[s.x][s.y] = 0;
    int count = 0;
    while (!q.empty()) {
        if (deep > maxDeep) {
            break;
        }
        int size = (int) q.size();
        deep += 1;
        //这一层出来的深度都一样
        for (int i = 0; i < size; i++) {
            ++count;
            Point top = q.front();
            q.pop();
            for (int j = 0; j < (sizeof(DIR) / sizeof(DIR[0])) / 2; j++) {
                //四方向的
                int lastDirIdx = cs[top.x][top.y] & 3;
                int dirIdx = j ^ lastDirIdx; // 优先遍历上一次过来的方向
                Point dir = DIR[dirIdx];
                int dx = top.x + dir.x;
                int dy = top.y + dir.y;//第一步
                if (cs[dx][dy] != SHORT_INF ||
                    (!(dx >= 0 && dx <= MAP_FILE_ROW_NUMS / 2 && dy >= 0 && dy <= MAP_FILE_COL_NUMS / 2)) ||
                    !mGameMap->robotCanReach(dx + leftTopX, dy + leftTopY)) {
                    continue; // 不可达或者访问过了
                }
                cs[dx][dy] = short((deep << 2) + dirIdx);//第一步家的优先级大于移位
                q.emplace(dx, dy);
            }
        }
    }
}
