//
// Created by sg12q on 2024/3/9.
//

#pragma once


#include "Utils.h"
#include "queue"
#include "Constants.h"

using std::queue;

class SimpleBoat {
public:
    int id = -1;
    int type = 0;
    int capacity = 0;
    int localId = -1;
    int lastNum = 0;
    int num = 0;
    bool belongToMe = false;
    int noMoveTime = 0;

    Point corePoint{-1, -1};
    PointWithDirection pointWithDirection{};
    PointWithDirection lastPointWithDirection{};
    queue<PointWithDirection> path;//保存50帧路径
    int direction = -1;
    int status = 0;
    int lastStatus = 0;
    int value = 0;

    void input() {
        lastNum = num;
        lastStatus = status;
        scanf("%d %d %d %d %d %d", &this->id, &this->num, &this->corePoint.x, &this->corePoint.y, &this->direction,
              &this->status);
        if (num == 0) {
            value = 0;
        }

        lastPointWithDirection = pointWithDirection;
        printMost(to_string(id) + " " + to_string(num) + " " + to_string(corePoint.x) + " " + to_string(corePoint.y) +
                  " " +
                  to_string(direction) + " " + to_string(status));
        pointWithDirection = {corePoint, direction};
        if (pointWithDirection == lastPointWithDirection) {
            noMoveTime++;
        } else {
            noMoveTime = 0;
        }
        path.push({{corePoint}, direction});
        if (path.size() > SIMPLE_BOAT_PATH_LENGTH) {
            path.pop();
        }
    }
};
