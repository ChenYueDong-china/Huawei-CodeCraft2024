//
// Created by sg12q on 2024/3/9.
//

#include "Strategy.h"
#include "Boat.h"

void Boat::input(SimpleBoat &simpleBoat) {
    num = simpleBoat.num;
    corePoint.x = simpleBoat.corePoint.x;
    corePoint.y = simpleBoat.corePoint.y;
    direction = simpleBoat.direction;
    status = simpleBoat.status;
    for (BoatSellPoint &boatSellPoint: strategy->boatSellPoints) {
        if (num == 0 && corePoint == boatSellPoint.point) {
            targetSellId = -1;//卖完
            carry = false;
            value = 0;
        }
    }
    printMost(to_string(id) + " " + to_string(num) + " " + to_string(corePoint.x) + " " + to_string(corePoint.y) + " " +
              to_string(direction) + " " + to_string(status));
    if (lastFrameMove && corePoint == simpleBoat.lastPointWithDirection.point
        &&
        direction == simpleBoat.lastPointWithDirection.direction) {
        noMoveTime++;
    } else {
        noMoveTime = 0;
    }
    if (lastFlashBerth && status == 0 && corePoint == simpleBoat.lastPointWithDirection.point
        && direction == simpleBoat.lastPointWithDirection.direction) {//闪现，且这一帧没等待或者出问题
        //没闪现成功，可能他恰好走了,也可能他也在strategy->berths[targetBerthId].curBoatGlobalId != -1
        printError(to_string(frameId) + ",boatId:" + to_string(id) + ",last frame flash berth default");
    }
    if (lastFlashBerth && status != 0) {
        //一定闪现成功
        assert(targetBerthId != -1);
        assert(corePoint == strategy->berths[targetBerthId].corePoint &&
               direction == strategy->berths[targetBerthId].coreDirection);
        //一定进入泊位
        strategy->berths[targetBerthId].curBoatGlobalId = globalId;
    }
    lastFlashBerth = false;
    if (lastFlashDept && !strategy->gameMap.boatIsAllInMainChannel(corePoint, direction)) {
        //没闪现成功
        printError(to_string(frameId) + "error last frame flash dept default");
    }
    lastFlashBerth = false;
    if (status == 1) {
        //恢复状态
        remainRecoveryTime--;
        if (remainRecoveryTime <= 0) {
            printError("error");
        }
        remainRecoveryTime = std::max(1, remainRecoveryTime);
    } else {
        //可能掉帧，一般情况下可能为1或者0
        remainRecoveryTime = 0;
    }
    assigned = false;
    path.clear();
    buyAssign = false;
}

void Boat::finish() {
    //按照path来看做什么操作
    if (status != 1
        && !assigned && !strategy->gameMap.boatIsAllInMainChannel(corePoint, direction) &&
        frameId > GAME_FRAME - 1000) {
        flashDept();
        lastFrameMove = false;
        return;
    }
    if (path.empty() || path.size() < 2) {//啥都不动，可能有问题，就算避让也会多走一帧
        printError("error");
        lastFrameMove = false;
        return;
    }
    if (status == 1 || (status == 2
                        && targetBerthId != -1
                        && strategy->berths[targetBerthId].curBoatGlobalId == globalId)) {
        //恢复状态，或者已经在目标泊位装货了
        lastFrameMove = false;
        return;
    }
    PointWithDirection next = path[1];

    if (!carry
        && targetBerthId != -1
        && next.point == strategy->berths[targetBerthId].corePoint) {
        if (strategy->berths[targetBerthId].curBoatGlobalId == -1 || corePoint == next.point) {
            //没有其他人了或者达到核心点了
            assert(strategy->gameMap.boatGetFlashBerthId(corePoint.x, corePoint.y) == targetBerthId);
            flashBerth();//去泊位
            lastFrameMove = false;//闪现泊位不能算动，因为可以跟别人抢位置
            return;
        }
    }
    if (next.point == corePoint && next.direction == direction) {
        lastFrameMove = false;
        return;
    }

    if (next.direction == direction && corePoint + DIR[direction] == next.point) {
        ship();//前进
        lastFrameMove = true;
    } else {
        int rotaDir = GameMap::getRotationDir(direction, next.direction);
        PointWithDirection rotationPoint = getBoatRotationPoint({corePoint, direction}, rotaDir == 0);
        if (rotationPoint.point == next.point) {
            rotation(rotaDir);//旋转
            lastFrameMove = true;
        } else {
            //闪现去主航道
            assert(strategy->gameMap.boatIsAllInMainChannel(next.point, next.direction));
            flashDept();
            lastFrameMove = false;
        }
    }
}

void Boat::ship() {
    assert(status != 1);
    printf("ship %d\n", id);
    //撞到主航道
    Point dir = DIR[direction];
    corePoint.x += dir.x;
    corePoint.y += dir.y;
    //检查移动后的位置
    if (strategy->gameMap.boatHasOneInMainChannel(corePoint, direction)) {
        remainRecoveryTime = 2;
    }
}

void Boat::rotation(int rotaDir) {
    assert(status != 1);
    printf("rot %d %d\n", id, rotaDir);
    //撞到主航道
    bool clockwise = rotaDir == 0;
    PointWithDirection next = getBoatRotationPoint(PointWithDirection(corePoint, direction), clockwise);
    corePoint.x = next.point.x;
    corePoint.y = next.point.y;
    direction = next.direction;
    //检查移动后的位置
    if (strategy->gameMap.boatHasOneInMainChannel(corePoint, direction)) {
        status = 1;
        remainRecoveryTime = 2;
    }
}

void Boat::flashBerth() {
    assert(status != 1);
    printf("berth %d\n", id);
    //计算恢复时间
    PointWithDirection next = PointWithDirection(strategy->berths[targetBerthId].corePoint, 0);
    remainRecoveryTime = 1 + 2 * (abs(next.point.x - corePoint.x) + abs(next.point.y - corePoint.y));
    corePoint.x = next.point.x;
    corePoint.y = next.point.y;
    direction = next.direction;
    status = 1;//不一定成功
    lastFlashBerth = true;
}

void Boat::flashDept() {
    assert(status != 1);
    printf("dept %d\n", id);
    PointWithDirection next = strategy->getBoatFlashDeptPoint(corePoint);
    remainRecoveryTime = 1 + 2 * (abs(next.point.x - corePoint.x) + abs(next.point.y - corePoint.y));
    corePoint.x = next.point.x;
    corePoint.y = next.point.y;
    direction = next.direction;
    status = 1;//闪现成功
    lastFlashDept = true;
    //计算恢复时间
}

