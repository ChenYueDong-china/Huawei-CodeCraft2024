package com.huawei.codecraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import static com.huawei.codecraft.BoatUtils.getNextPoint;
import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;

public class BoatDijkstra {
    private Point mTarget;//目标点
    private GameMap mGameMap;

    short[][][] minDistanceDirection = new short[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][DIR.length / 2];

    void init(Point target, GameMap gameMap) {
        this.mTarget = target;
        this.mGameMap = gameMap;
    }

    public Point map2RelativePoint(Point corePoint, int dir) {
        if (dir == 0) {
            return corePoint.add(new Point(BOAT_WIDTH - 1, BOAT_LENGTH - 1));
        } else if (dir == 1) {
            return corePoint.add(new Point(BOAT_WIDTH - 1, BOAT_LENGTH - 1).mul(-1));
        } else if (dir == 2) {
            return corePoint.add(new Point(-(BOAT_LENGTH - 1), BOAT_WIDTH - 1));
        } else {
            return corePoint.add(new Point(-(BOAT_LENGTH - 1), BOAT_WIDTH - 1).mul(-1));
        }
    }

    void update(int maxDeep) {
        for (short[][] shorts : minDistanceDirection) {
            for (short[] aShort : shorts) {
                Arrays.fill(aShort, Short.MAX_VALUE);
            }
        }
        //从目标映射的四个点开始搜
        int deep = 0;
        Queue<PointWithDirection> queue = new ArrayDeque<>();
        for (int i = 0; i < DIR.length / 2; i++) {
            //单向dfs搜就行
            // 求最短路径
            Point s = map2RelativePoint(mTarget, i);
            if (!mGameMap.isLegalPoint(s.x, s.y) || !mGameMap.boatCanReach(s, i ^ 1)) {
                continue;//起始点直接不可达，没得玩
            }
            minDistanceDirection[s.x][s.y][i ^ 1] = 0;
            queue.offer(new PointWithDirection(s, i ^ 1));
        }
        ArrayList<PointWithDirection> twoDistancesPoints = new ArrayList<>();
        while (!queue.isEmpty() || !twoDistancesPoints.isEmpty()) {
            deep += 1;
            if (deep > maxDeep) {
                break;
            }
            //2距离的下一个点,先保存起来，后面直接插进去
            int size = queue.size();
            //先加进去
            if (!twoDistancesPoints.isEmpty()) {
                queue.addAll(twoDistancesPoints);
                twoDistancesPoints.clear();
            }
            for (int j = 0; j < size; j++) {
                PointWithDirection top = queue.poll();
                assert top != null;
                for (int k = 0; k < 3; k++) {
                    PointWithDirection next = getNextPoint(top, k);
                    //合法性判断
                    if (!mGameMap.boatCanReach(next.point, next.direction)
                            ||
                            deep >= minDistanceDirection[next.point.x][next.point.y][next.direction]) {
                        continue;
                    }
                    //是否到达之后需要恢复,有一个点进入了主航道
                    if (mGameMap.boatHasOneInMainChannel(next.point, next.direction)) {
                        if (deep + 1 >= minDistanceDirection[next.point.x][next.point.y][next.direction]) {
                            continue;
                        }
                        minDistanceDirection[next.point.x][next.point.y][next.direction] = (short) (deep + 1);
                        twoDistancesPoints.add(next);
                    } else {
                        minDistanceDirection[next.point.x][next.point.y][next.direction] = (short) deep;
                        queue.offer(next);
                    }
                }
            }

        }
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                for (int k = 0; k < DIR.length / 2; k++) {
                    Point corePoint = new Point(i, j);
                    if (!mGameMap.boatCanReach(corePoint, k)) {
                        continue;
                    }
                    if (!mGameMap.boatHasOneInMainChannel(corePoint, k)
                            &&
                            minDistanceDirection[i][j][k] != Short.MAX_VALUE) {
                        minDistanceDirection[i][j][k] += 1;//开始船不在主航道上，距离需要加一，因为到达目标点之后需要等待一帧
                    }
                }
            }
        }
    }


    //非细化目标到这里的移动时间
    public short getMoveDistance(Point target, int dir) {
        Point start = map2RelativePoint(target, dir);
        if (!mGameMap.isLegalPoint(start.x, start.y)) {
            return Short.MAX_VALUE;
        }
        dir ^= 1;
        return minDistanceDirection[start.x][start.y][dir] != Short.MAX_VALUE ?
                minDistanceDirection[start.x][start.y][dir] :
                Short.MAX_VALUE;

    }

}
