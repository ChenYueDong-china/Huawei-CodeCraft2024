//
// Created by sg12q on 2024/3/9.
//

#pragma once

#include "Strategy.h"
#include "SimpleRobot.h"

class Strategy;

struct Robot {
    int globalId = -1;
    int id = -1;
    int type = 0;
    int maxNum = 1;
    int num = 1;
    int buyFrame = 0;
    int avoidOtherTime = 0;
    bool lastFrameMove{};
    int noMoveTime{};//主动移动才算
    bool isPending{};//主动移动才算
    int questionId = -1;//主动移动才算
    string question{};//主动移动才算
    int ans = -1;//主动移动才算
    bool carry{};
    bool avoid{};
    int carryValue{};
    Point pos{-1, -1};
    Point lastBuyPos{-1, -1};
    bool redundancy{};

    bool assigned{};
    int targetBerthId = -1;
    int targetWorkBenchId = -1;
    vector<Point> path{};

    Strategy *strategy{};
    int forcePri{};
    int beConflicted = 0;
    bool buyAssign = false;
    int priority = 0;


    Robot() = default;

    explicit Robot(Strategy *strategy, int type) {
        this->strategy = strategy;
        this->type = type;
        maxNum = (type == 0 ? 1 : 2);
    }

    void input(SimpleRobot &simpleRobot);

    void buy();

    void finish();

    void pull();


};

