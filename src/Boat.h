//
// Created by sg12q on 2024/3/9.
//

#pragma once

#include "Strategy.h"
#include "Utils.h"
#include "SimpleBoat.h"

class Strategy;

class Boat {
public:
    Strategy *strategy;
    int beConflicted = 0;
    int avoidOtherTime = 0;
    int noMoveTime = 0;//发了移动指令但没移动的帧数
    bool lastFrameMove = true; //上一帧是否移动
    bool carry = false;
    int sellCount = 0;
    int globalId = 0;
    vector<PointWithDirection> path;
    Point corePoint{-1, -1};
    int id{};
    int direction{};
    int targetBerthId = -1;//买的Id
    int targetSellId = -1;//卖的id
    int status{};//状态
    int originStatus{};//状态
    int remainRecoveryTime = 0;//恢复时间

    int lastNum{};//目前货物数量
    int num{};//目前货物数量
    int value{};//货物金额
    int capacity{};//容量
    bool assigned{};
    bool buyAssign{};
    bool avoid{};
    int forcePri{};

    bool lastFlashBerth = false;
    bool lastFlashDept = false;

    Boat() {
        num = 0;
        this->strategy = nullptr;
    };

    explicit Boat(Strategy *strategy, int capacity) {
        num = 0;
        this->capacity = capacity;
        this->strategy = strategy;
    }

    void input(SimpleBoat &simpleBoat);

    void ship();

    void rotation(int rotaDir);

    void flashBerth();

    void flashDept();

    void finish();
};
