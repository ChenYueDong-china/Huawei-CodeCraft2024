package com.huawei.codecraft;

import sun.security.util.Length;

import java.util.*;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;
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

    public PointWithDirection getRotationPoint(PointWithDirection pointWithDirection, boolean clockwise) {
        Point corePint = pointWithDirection.point;
        int originDir = pointWithDirection.direction;
        int[] data;
        if (clockwise) {
            data = new int[]{0, 3, 1, 2, 0};
        } else {
            data = new int[]{0, 2, 1, 3, 0};
        }
        int nextDir = -1;
        for (int i = 0; i < DIR.length / 2; i++) {
            if (originDir == data[i]) {
                nextDir = data[i + 1];
                break;
            }
        }
        //位置
        Point[] data2;
        if (clockwise) {
            data2 = new Point[]{new Point(0, 0), new Point(2, 2), new Point(2, 0), new Point(0, 2)};
        } else {
            data2 = new Point[]{new Point(0, 0), new Point(0, 2), new Point(1, 1), new Point(-1, 1)};
        }
        Point nextPoint = corePint.add(data2[nextDir]).sub(data2[originDir]);
        return new PointWithDirection(nextPoint, nextDir);

    }


    void update() {

        class SearchPoint {
            final PointWithDirection pointWithDirection;
            final int comingDir;

            public SearchPoint(PointWithDirection pointWithDirection, int comingDir) {
                this.pointWithDirection = pointWithDirection;
                this.comingDir = comingDir;
            }
        }
        for (int[][][] c1 : cs) {
            for (int[][] c2 : c1) {
                for (int[] c3 : c2) {
                    Arrays.fill(c3, Integer.MAX_VALUE);
                }
            }
        }
        //从目标映射的四个点开始搜
        for (int i = 0; i < DIR.length / 2; i++) {
            //单向dfs搜就行
            // 求最短路径
            Point s = map2RelativePoint(mTarget, i);
            if (!mGameMap.boatCanReach(s, i ^ 1)) {
                continue;//起始点直接不可达，没得玩
            }

            int deep = 0;
            Queue<SearchPoint> queue = new LinkedList<>();
            queue.offer(new SearchPoint(new PointWithDirection(s, i ^ 1), i ^ 1));
            cs[i][s.x][s.y][i ^ 1] = i ^ 1;
            ArrayList<SearchPoint> twoDistancesPoints = new ArrayList<>();
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
                    SearchPoint top = queue.poll();
                    assert top != null;
                    for (int k = 0; k < 3; k++) {
                        PointWithDirection next;
                        if (k == 0) {
                            Point dir = DIR[top.pointWithDirection.direction];
                            int dx = top.pointWithDirection.point.x + dir.x;
                            int dy = top.pointWithDirection.point.y + dir.y;//第一步
                            next = new PointWithDirection(new Point(dx, dy), top.pointWithDirection.direction);
                        } else if (k == 1) {
                            next = getRotationPoint(top.pointWithDirection, true);
                        } else {
                            next = getRotationPoint(top.pointWithDirection, false);
                        }
                        //合法性判断
                        if ((cs[i][next.point.x][next.point.y][next.direction] >> 2) < deep + 2
                                || !mGameMap.boatCanReach(next.point, next.direction)) {
                            continue;
                        }
                        //是否到达之后需要恢复,有一个点进入了主航道
                        if (mGameMap.hasOneInMainChannel(next.point, next.direction)) {
                            cs[i][next.point.x][next.point.y][next.direction]
                                    = ((deep + 1) << 2) + top.pointWithDirection.direction;
                            twoDistancesPoints.add(new SearchPoint(next, top.pointWithDirection.direction));
                        } else {
                            cs[i][next.point.x][next.point.y][next.direction]
                                    = (deep << 2) + top.pointWithDirection.direction;
                            queue.offer(new SearchPoint(next, top.pointWithDirection.direction));
                        }
                    }
                }

            }
        }

        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                for (int k = 0; k < DIR.length / 2; k++) {
                    int minDir = -1;
                    int minDis = Integer.MAX_VALUE;
                    for (int l = 0; l < DIR.length / 2; l++) {
                        if (cs[l][i][j][k] != Integer.MAX_VALUE) {
                            int deep = cs[l][i][j][k] >> 2;
                            if (!mGameMap.hasOneInMainChannel(new Point(i, j), k) && mGameMap.hasOneInMainChannel(mTarget, l)) {
                                deep += 1;//开始船不在主航道上，距离需要加一，因为到达目标点之后需要等待一帧
                            }
                            if (mGameMap.hasOneInMainChannel(new Point(i, j), k) && !mGameMap.hasOneInMainChannel(mTarget, l)) {
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
            Point lastPoint;
            if (lastDir == t.direction) {
                //前进
                lastPoint = t.point.add(DIR[lastDir ^ 1]);
            } else {
                //旋转或者不旋转
                lastPoint = getLastPoint(t.point, t.direction, lastDir);
            }
            t = new PointWithDirection(lastPoint, lastDir);
            result.add(t);
        }

        //转为正常路径
        for (PointWithDirection pointWithDirection : result) {
            pointWithDirection.point = map2RelativePoint(pointWithDirection.point, pointWithDirection.direction);
            pointWithDirection.direction ^= 1;
        }
        int deep = minDistanceDirection[start.x][start.y][dir] >> 2;
        //验证深度对不对
        int curDeep = 0;
        for (int i = 1; i < result.size(); i++) {
            if (mGameMap.hasOneInMainChannel(result.get(i).point, result.get(i).direction)) {
                curDeep += 2;
            } else {
                curDeep += 1;
            }
        }
        assert curDeep == deep;
        return result;
    }

    private Point getLastPoint(Point curPoint, int curDir, int lastDir) {
        //判断是否顺
        boolean clockwise = mGameMap.getRotationDir(lastDir, curDir) == 0;
        PointWithDirection pointWithDirection = getRotationPoint(new PointWithDirection(new Point(0, 0), lastDir), clockwise);
        assert pointWithDirection.direction == curDir;
        return curPoint.add(pointWithDirection.point.mul(-1));
    }


    //非细化目标到这里的移动时间
    public int getMoveDistance(Point target, int dir) {
        Point start = map2RelativePoint(target, dir);
        dir ^= 1;
        return minDistanceDirection[start.x][start.y][dir] != Integer.MAX_VALUE ?
                minDistanceDirection[start.x][start.y][dir] >> 2 :
                Integer.MAX_VALUE;

    }

}
