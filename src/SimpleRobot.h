//
// Created by sg12q on 2024/3/9.
//

#pragma once


#include "Utils.h"
#include "queue"
#include "stack"
#include "Constants.h"

using std::queue;
using std::stack;

class SimpleRobot {
public:
    int type = 0;
    int maxNum;
    bool belongToMe = false;
    int id;
    Point lastP{};
    Point p{};
    int lastNum = 0;
    int num = 0;
    queue<Point> path;//保存50帧路径
    vector<int> goodList;//保存50帧路径

    int lastDir = -1;
    int noMoveTime = 0;

    void input() {
        lastNum = num;
        lastP = p;
        scanf("%d %d %d %d", &id, &num, &p.x, &p.y);
        printMost(to_string(id) + " " + to_string(num) + " " + to_string(p.x) + " " + to_string(p.y));
        if (lastP != p) {
            noMoveTime = 0;
            //移动了
            lastDir = getDir(p - lastP);
            if (lastDir == -1) {
                lastDir = 0;
            }
        } else {
            //没移动
            noMoveTime++;
        }
        path.push(p);
        if (path.size() > SIMPLE_ROBOT_PATH_LENGTH) {
            path.pop();
        }
    }


};
