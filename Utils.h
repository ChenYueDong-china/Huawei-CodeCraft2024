#pragma once

#include <cstdio>
#include <cassert>
#include <cstring>
#include <cmath>
#include "Constants.h"
#include <iostream>
#include <vector>
#include <chrono>
#include <string>
#include <unordered_map>

using std::to_string;
using std::vector;
using std::pair;
using std::string;
using std::unordered_map;
const auto start_time = std::chrono::steady_clock::now();

inline long long int runTime() {
    auto now = std::chrono::steady_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(now - start_time);
    return duration.count();
}

inline long long getMicroTimestamp() {
    auto duration_since_epoch = std::chrono::system_clock::now().time_since_epoch(); // 从1970-01-01 00:00:00到当前时间点的时长
    auto microseconds_since_epoch = std::chrono::duration_cast<std::chrono::microseconds>(
            duration_since_epoch).count(); // 将时长转换为微秒数
    return microseconds_since_epoch;
}


static bool debug = false;

inline void printDebug(const string &s) {
    if (debug) {
        fprintf(stderr, "%s\n", s.c_str());
        //         fflush(stderr)
    }
}

static bool error = true;

inline void printError(const string &s) {
    if (error) {
        fprintf(stderr, "%s\n", s.c_str());
        fflush(stderr);
    }
}


inline void trim(string &s) {

    if (!s.empty()) {
        s.erase(0, s.find_first_not_of('\n'));
        s.erase(s.find_last_not_of('\n') + 1);
    }

}

static bool MOST = false;

inline void printMost(string s) {
    if (MOST) {
        trim(s);
        fprintf(stderr, "%s\n", s.c_str());
        fflush(stderr);
    }
}


struct Point // 向量或坐标
{
    int x, y;

    Point() : x(0), y(0) {
    }

    constexpr Point(int _x, int _y) : x(_x), y(_y) {
    }


    Point operator+(Point v) const {
        return {x + v.x, y + v.y};
    }

    Point operator-(Point v) const {
        return {x - v.x, y - v.y};
    }

    Point &operator+=(const Point &v) {
        x += v.x;
        y += v.y;
        return *this;
    }

    Point operator/(int i) const {
        return {x / i, y / i};
    }

    bool operator==(const Point &v) const {
        return x == v.x && y == v.y;
    }

    bool operator!=(const Point &v) const {
        return !this->operator==(v);
    }

    int operator*(Point v) const {
        return x * v.x + y * v.y;
    }

    Point operator*(int v) const {
        return {x * v, y * v};
    }
};


struct PointWithDirection {
    Point point{};
    int direction{};

    PointWithDirection() = default;

    PointWithDirection(Point point, int direction) {
        this->point = point;
        this->direction = direction;
    }

    bool operator==(const PointWithDirection &rhs) const {
        return point == rhs.point &&
               direction == rhs.direction;
    }

    bool operator!=(const PointWithDirection &rhs) const {
        return !(rhs == *this);
    }
};

const Point DIR[8] = {
        Point(0, 1),//右移
        Point(0, -1),//左移
        Point(-1, 0),//上移
        Point(1, 0),//下移
        Point(-1, -1),
        Point(1, 1),
        Point(-1, 1),
        Point(1, -1),
};

const int BOAT_ROTATION[DIR_LENGTH][DIR_LENGTH] = {
        {3, 2, 0, 1},
        {2, 3, 1, 0}};
const Point BOAT_ROTATION_POINT[2][DIR_LENGTH][DIR_LENGTH] = {
        {
                {{}, {}, {},     {0, 2}}, //3
                  {{}, {}, {0, -2}, {}},//2
                  {{-2, 0}, {},      {}, {}},//0
                  {{},      {2, 0}, {}, {}}//1
        },
        {
                {{}, {}, {1, 1}, {}}//2
                , {{}, {}, {},      {-1, -1}}//3
                , {{},      {-1, 1}, {}, {}}//1
                , {{1, -1}, {},     {}, {}}//0
        }
};

inline static PointWithDirection getBoatRotationPoint(const PointWithDirection &pointWithDirection, bool clockwise) {
    Point corePint = pointWithDirection.point;
    int originDir = pointWithDirection.direction;
    int nextDir;
    Point nextPoint;
    if (clockwise) {
        nextDir = BOAT_ROTATION[0][originDir];
        nextPoint = corePint + BOAT_ROTATION_POINT[0][originDir][nextDir];
    } else {
        nextDir = BOAT_ROTATION[1][originDir];
        nextPoint = corePint + BOAT_ROTATION_POINT[1][originDir][nextDir];
    }
    return {nextPoint, nextDir};
}

static int getIntInput() {
    int intIn;
    scanf("%d", &intIn);
    printMost(to_string(intIn));
    return intIn;
}


static std::vector<Point> getRobotPathByCs(short cs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS], Point target) {
    vector<Point> result;
    Point t(target);
    result.emplace_back(t);
    while (cs[t.x][t.y] != 0) {
        t += DIR[(cs[t.x][t.y] & 3) ^ 1];
        result.emplace_back(t);
    }
    return result;
}

static std::vector<Point>
getRobotPathByCs(short cs[MAP_FILE_ROW_NUMS / 2 + 1][MAP_FILE_COL_NUMS / 2 + 1], Point target) {
    vector<Point> result;
    Point t(target);
    result.emplace_back(t);
    while (cs[t.x][t.y] != 0) {
        t += DIR[(cs[t.x][t.y] & 3) ^ 1];
        result.emplace_back(t);
    }
    return result;
}


static std::vector<Point> getPathByCs(short cs[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS], Point target) {
    std::vector<Point> result;
    Point t(target);
    result.emplace_back(t);
    while (cs[t.x][t.y] != 0) {
        t += DIR[(cs[t.x][t.y] & 3) ^ 1];
        result.emplace_back(t);
    }
    return result;
}

static int getDir(Point point) {
    for (int i = 0; i < sizeof DIR / sizeof DIR[0]; i++) {
        if (DIR[i] * point > 0) {
            return i;
        }
    }
    assert(false);
    return -1;
}

template<class T1, class T2>
static vector<pair<T1, T2>> showComplexMap(unordered_map<T1, T2> complexMap) {
    vector<pair<T1, T2>> result;
    result.reserve(complexMap.size());
    for (pair<T1, T2> item: complexMap) {
        result.push_back(item);
    }
    return result;
}