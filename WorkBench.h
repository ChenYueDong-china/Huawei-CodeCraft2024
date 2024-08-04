//
// Created by chenyuedong on 2023/9/24.
//
#pragma once

#include "Utils.h"
#include "Dijkstra.h"


struct Workbench {

    int id{-1};
    Point pos;
    int value{};
    int minSellDistance{};
    int minSellBerthId{-1};
    short cs[MAP_FILE_ROW_NUMS / 2 + 1][MAP_FILE_COL_NUMS / 2 + 1]{};
    int remainTime{};
    bool lastFrameSelect{};
    double curMaxProfit{};
    int maxProfitId = -1;
public:
    Workbench() = default;

    explicit Workbench(int id) {
        this->id = id;
    }

    void updateDij(GameMap &map) {
        //初始化dij
        long long int l1 = runTime();
        static Dijkstra dijkstra;
        dijkstra.init(pos, map);
        memset(cs, SHORT_INF, sizeof cs);
        if (value > PRECIOUS_WORKBENCH_BOUNDARY) {
            //300米之内
            dijkstra.update(PRECIOUS_WORKBENCH_MAX_SEARCH_DEEP, cs);
        } else {
            //100米之内
            dijkstra.update(COMMON_WORKBENCH_MAX_SEARCH_DEEP, cs);
        }
        long long int l2 = runTime();
        printDebug("wb:" + to_string(l2 - l1));
    }

    vector<Point> moveFrom(Point point) {
        int leftTopX = pos.x - MAP_FILE_ROW_NUMS / 4;
        int leftTopY = pos.y - MAP_FILE_COL_NUMS / 4;
        int fakeX = point.x - leftTopX;
        int fakeY = point.y - leftTopY;
        Point source{fakeX, fakeY};
        if (cs[fakeX][fakeY] == SHORT_INF) {
            return {};
        }
        vector<Point> result = getRobotPathByCs(cs, source);
        if (result.size() == 1) {
            //此时大概率有问题
            printError("error start equal end");
            result.push_back(source);//多加一个
        }
        for (Point &re: result) {
            re.x += leftTopX;
            re.y += leftTopY;
        }
        //细化，转成精细坐标
        return GameMap::toDiscretePath(result);
    }

    //获得这个workbench到任意一个位置的最小距离
    short getMinDistance(Point point) {
        int leftTopX = pos.x - MAP_FILE_ROW_NUMS / 4;
        int leftTopY = pos.y - MAP_FILE_COL_NUMS / 4;
        int fakeX = point.x - leftTopX;
        int fakeY = point.y - leftTopY;
        if (fakeX >= 0 && fakeX <= (MAP_FILE_ROW_NUMS / 2) &&
            fakeY >= 0 && fakeY <= (MAP_FILE_COL_NUMS / 2)) {
            return cs[fakeX][fakeY] == SHORT_INF ? SHORT_INF : (short) (cs[fakeX][fakeY] >> 2);
        }
        return SHORT_INF;
    }

    bool canReach(Point pos_) {
        return getMinDistance(pos_) != SHORT_INF;
    }


    void setHeuristicCs(short heuristicCs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]) {
        int leftTopX = pos.x - MAP_FILE_ROW_NUMS / 4;
        int leftTopY = pos.y - MAP_FILE_COL_NUMS / 4;
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                int fakeX = i - leftTopX;
                int fakeY = j - leftTopY;
                if (fakeX >= 0 && fakeX <= (MAP_FILE_ROW_NUMS / 2) &&
                    fakeY >= 0 && fakeY <= (MAP_FILE_COL_NUMS / 2)) {
                    heuristicCs[i][j] = cs[fakeX][fakeY];
                } else {
                    heuristicCs[i][j] = SHORT_INF;
                }
            }
        }
    }
};
