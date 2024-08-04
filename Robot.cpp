//
// Created by sg12q on 2024/3/9.
//

#include "Strategy.h"
#include "Robot.h"

void Robot::input(SimpleRobot &simpleRobot) {
    carry = simpleRobot.num == maxNum;
    this->num = simpleRobot.num;
    pos.x = simpleRobot.p.x;
    pos.y = simpleRobot.p.y;
    simpleRobot.type = type;
    assigned = false;
    carryValue = 0;
    if (lastFrameMove && simpleRobot.lastP == simpleRobot.p) {
        noMoveTime++;
    } else {
        noMoveTime = 0;
    }
    if (num != 0) {
        for (const auto &i: simpleRobot.goodList) {
            carryValue += i;
        }
    }
    redundancy = true;//到目标点有冗余时间
    avoid = false;
    path.clear();
    buyAssign = false;
}

void Robot::buy() {
    printf("get %d\n", id);
    int value = strategy->workbenches[targetWorkBenchId].value;
    if (value < PRECIOUS_WORKBENCH_BOUNDARY) {
        //珍贵物品，啥都不干，会进入问答状态
        num += 1;
        carry = num == maxNum;
        lastBuyPos = strategy->workbenches[targetWorkBenchId].pos;
        strategy->workbenchesLock.insert(targetWorkBenchId);//锁住不能再次决策
        //不一定能买的到，不能删除，也不能把自己得目标改了
    }
}

void Robot::finish() {
    //按照path来看做什么操作
    if (path.size() <= 2) {
        printError("error");
        lastFrameMove = false;
        return;
    }
    Point target = GameMap::discreteToPos(path[2]);
    assert(strategy->gameMap.robotCanReach(target.x, target.y));
    if (target == pos) {
        if (isPending && ans != -1) {
            //回答问题
            printf("ans %d %d\n", id, ans);
        }
        lastFrameMove = false;
        return;
    }//todo assert 以下不能走两格
    assert((target - pos).x == 0 || (target - pos).y == 0);
    assert(abs((target - pos).x) == 1 || abs((target - pos).y) == 1);
    int dir = getDir(target - pos);
    printf("move %d %d\n", this->id, dir);
    lastFrameMove = true;
    if (!assigned) {
        //没任务，但是避让了
        return;
    }
    //此时是移动后，可以提前做一些事情
    if (carry) {
        //要去卖
        assert(targetBerthId != -1);
        //在他这个berth范围内
        if (strategy->berths[targetBerthId].inBerth(target)) {
            //提前卖，移动完毕卖,货物这种时候可以增加
            pull();
        }
    } else {
        //要去买
        assert(targetWorkBenchId != -1);
        if (strategy->workbenches[targetWorkBenchId].pos == target) {
            //提前买，移动完毕买,机器人可以移动后立即取货
            buy();
        }
    }
}

void Robot::pull() {
    targetBerthId = -1;
    targetWorkBenchId = -1;
    carry = false;
    num = 0;
    printf("pull %d\n", id);
}
