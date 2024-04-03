package com.huawei.codecraft;

import java.util.*;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;
import static com.huawei.codecraft.BoatUtils.*;
import static java.lang.Math.min;

public class BoatDijkstra {
    private Point mTarget;//目标点
    private GameMap mGameMap;

    int[][][][] cs = new int[DIR.length / 2][MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][DIR.length / 2];//位置，来源方向，0只走，1顺转，2逆转
    int[][][] minDistanceDirection = new int[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][DIR.length / 2];

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


    void update() {

        for (int[][][] c1 : cs) {
            for (int[][] c2 : c1) {
                for (int[] c3 : c2) {
                    Arrays.fill(c3, Integer.MAX_VALUE);
                }
            }
        }
        //从目标映射的四个点开始搜
        int count = 0;
        long l1 = System.currentTimeMillis();
        for (int i = 0; i < DIR.length / 2; i++) {
            //单向dfs搜就行
            // 求最短路径
            Point s = map2RelativePoint(mTarget, i);
            if (!mGameMap.boatCanReach(s, i ^ 1)) {
                continue;//起始点直接不可达，没得玩
            }
            int deep = 0;
            Queue<PointWithDirection> queue = new ArrayDeque<>();
            queue.offer(new PointWithDirection(s, i ^ 1));
            cs[i][s.x][s.y][i ^ 1] = i ^ 1;
            ArrayList<PointWithDirection> twoDistancesPoints = new ArrayList<>();
            while (!queue.isEmpty() || !twoDistancesPoints.isEmpty()) {
                deep += 1;
                //2距离的下一个点,先保存起来，后面直接插进去
                int size = queue.size();
                //先加进去
                if (!twoDistancesPoints.isEmpty()) {
                    queue.addAll(twoDistancesPoints);
                    twoDistancesPoints.clear();
                }
                for (int j = 0; j < size; j++) {
                    count++;
                    PointWithDirection top = queue.poll();
                    assert top != null;
                    for (int k = 0; k < 3; k++) {
                        PointWithDirection next = getNextPoint(top, k);
                        //合法性判断
                        if (deep >= (cs[i][next.point.x][next.point.y][next.direction] >> 2)
                                || !mGameMap.boatCanReach(next.point, next.direction)) {
                            continue;
                        }
                        //是否到达之后需要恢复,有一个点进入了主航道
                        if (mGameMap.boatHasOneInMainChannel(next.point, next.direction)) {
                            if (deep + 1 >= (cs[i][next.point.x][next.point.y][next.direction] >> 2)) {
                                continue;
                            }
                            cs[i][next.point.x][next.point.y][next.direction]
                                    = ((deep + 1) << 2) + top.direction;
                            twoDistancesPoints.add(next);
                        } else {
                            cs[i][next.point.x][next.point.y][next.direction]
                                    = (deep << 2) + top.direction;
                            queue.offer(next);
                        }
                    }
                }
            }
        }
        long r1 = System.currentTimeMillis();
        long time1 = r1 - l1;
        l1 = System.currentTimeMillis();
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                for (int k = 0; k < DIR.length / 2; k++) {
                    int minDir = -1;
                    int minDis = Integer.MAX_VALUE;
                    for (int l = 0; l < DIR.length / 2; l++) {
                        if (cs[l][i][j][k] != Integer.MAX_VALUE) {
                            int deep = cs[l][i][j][k] >> 2;
                            if (!mGameMap.boatHasOneInMainChannel(new Point(i, j), k)
                                    && mGameMap.boatHasOneInMainChannel(mTarget, l)) {
                                deep += 1;//开始船不在主航道上，距离需要加一，因为到达目标点之后需要等待一帧
                            }
                            if (mGameMap.boatHasOneInMainChannel(new Point(i, j), k)
                                    && !mGameMap.boatHasOneInMainChannel(mTarget, l)) {
                                deep -= 1;
                            }
                            if (deep < minDis) {
                                minDir = l;
                                minDis = deep;
                            }
                        }
                    }
                    if (minDir == -1) {
                        minDistanceDirection[i][j][k] = Integer.MAX_VALUE;
                    } else {
                        minDistanceDirection[i][j][k] = (minDis << 2) + minDir;
                    }
                }
            }
        }
        r1 = System.currentTimeMillis();
        long time2 = r1 - l1;
        //System.out.println("time1:" + time1 + ",time2:" + time2);
    }

    //move to没有，因为没保存
    public ArrayList<PointWithDirection> moveFrom(Point source, int dir) {
        Point start = map2RelativePoint(source, dir);

        dir ^= 1;
        ArrayList<PointWithDirection> result = new ArrayList<>();
        int index = minDistanceDirection[start.x][start.y][dir];
        assert index != Integer.MAX_VALUE;
        index = index & 3;
        PointWithDirection t = new PointWithDirection(start, dir);
        result.add(t);
        while ((cs[index][t.point.x][t.point.y][t.direction] >> 2) != 0) {
            int lastDir = cs[index][t.point.x][t.point.y][t.direction] & 3;
            t = getLastPointWithDirection(mGameMap, result, t, lastDir);
        }

        //转为正常路径
        for (PointWithDirection pointWithDirection : result) {
            pointWithDirection.point = map2RelativePoint(pointWithDirection.point, pointWithDirection.direction);
            pointWithDirection.direction ^= 1;
        }
        int deep = minDistanceDirection[start.x][start.y][dir] >> 2;
        ArrayList<PointWithDirection> tmp = new ArrayList<>();
        tmp.add(result.get(0));
        //验证深度对不对
        int curDeep = 0;
        for (int i = 1; i < result.size(); i++) {
            if (mGameMap.boatHasOneInMainChannel(result.get(i).point, result.get(i).direction)) {
                tmp.add(result.get(i));
            }
            tmp.add(result.get(i));
        }
        result.clear();
        result.addAll(tmp);
        assert tmp.size() - 1 == deep;
        return result;
    }


    //非细化目标到这里的移动时间
    public int getMoveDistance(Point target, int dir) {
        Point start = map2RelativePoint(target, dir);
        if (!mGameMap.isLegalPoint(start.x, start.y)) {
            return Integer.MAX_VALUE;
        }
        dir ^= 1;
        return minDistanceDirection[start.x][start.y][dir] != Integer.MAX_VALUE ?
                minDistanceDirection[start.x][start.y][dir] >> 2 :
                Integer.MAX_VALUE;

    }

}
