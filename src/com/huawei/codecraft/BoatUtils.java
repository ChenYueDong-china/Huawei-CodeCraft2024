package com.huawei.codecraft;

import java.util.*;

import static com.huawei.codecraft.Utils.*;
import static com.huawei.codecraft.Constants.*;
import static java.lang.Math.abs;

public class BoatUtils {
    public static PointWithDirection getRotationPoint(PointWithDirection pointWithDirection, boolean clockwise) {
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

    public static Point getLastPoint(GameMap gameMap, Point curPoint, int curDir, int lastDir) {
        //判断是否顺
        boolean clockwise = gameMap.getRotationDir(lastDir, curDir) == 0;
        PointWithDirection pointWithDirection = getRotationPoint(new PointWithDirection(new Point(0, 0), lastDir), clockwise);
        assert pointWithDirection.direction == curDir;
        return curPoint.add(pointWithDirection.point.mul(-1));
    }

    public static PointWithDirection getLastPointWithDirection(GameMap gameMap, ArrayList<PointWithDirection> result, PointWithDirection t, int lastDir) {
        Point lastPoint;
        if (lastDir == t.direction) {
            //前进
            lastPoint = t.point.add(DIR[lastDir ^ 1]);
        } else {
            //旋转或者不旋转
            lastPoint = getLastPoint(gameMap, t.point, t.direction, lastDir);
        }
        t = new PointWithDirection(lastPoint, lastDir);
        result.add(t);
        return t;
    }

    public static PointWithDirection getNextPoint(PointWithDirection top, int k) {
        PointWithDirection next;
        if (k == 0) {
            Point dir = DIR[top.direction];
            int dx = top.point.x + dir.x;
            int dy = top.point.y + dir.y;//第一步
            next = new PointWithDirection(new Point(dx, dy), top.direction);
        } else if (k == 1) {
            next = getRotationPoint(top, true);
        } else {
            next = getRotationPoint(top, false);
        }
        return next;
    }

    public static ArrayList<PointWithDirection> moveTo(GameMap gameMap, PointWithDirection start, PointWithDirection end, int maxDeep) {
        //已经在了，直接返回，防止cs消耗
        if (start.point.equal(end.point) && (start.direction == end.direction || end.direction == -1)) {
            //输出路径
            ArrayList<PointWithDirection> result = new ArrayList<>();
            result.add(start);
            return result;
        }
        int[][][] cs = gameMap.commonCs;
        for (int[][] c1 : cs) {
            for (int[] c2 : c1) {
                Arrays.fill(c2, Integer.MAX_VALUE);
            }
        }
        //从目标映射的四个点开始搜
        int deep = 0;
        PointWithDirection s = new PointWithDirection(new Point(start.point), start.direction);
        Queue<PointWithDirection> queue = new ArrayDeque<>();
        queue.offer(s);
        cs[s.point.x][s.point.y][s.direction] = s.direction;
        ArrayList<PointWithDirection> twoDistancesPoints = new ArrayList<>();
        while (!queue.isEmpty() || !twoDistancesPoints.isEmpty()) {
            if (deep > maxDeep) {
                return null;
            }
            deep += 1;
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
                if (top.point.equal(end.point) && (top.direction == end.direction || end.direction == -1)) {
                    //输出路径
                    return backTrackPath(gameMap, top, cs);
                }
                nextEnterQueue(gameMap, cs, deep, queue, twoDistancesPoints, top);
            }
        }
        return null;
    }


    public static ArrayList<PointWithDirection> moveToBerth(GameMap gameMap, PointWithDirection start, int berthId, Point berthCorePoint, int maxDeep, int aroundPointsCount) {
        //todo 船搜一条到泊位得路径,
        if(start.point.equals(berthCorePoint)){
            //直接返回
            ArrayList<PointWithDirection> result = new ArrayList<>();
            result.add(start);
            return result;
        }
        int[][][] cs = gameMap.commonCs;
        for (int[][] c1 : cs) {
            for (int[] c2 : c1) {
                Arrays.fill(c2, Integer.MAX_VALUE);
            }
        }
        //从目标映射的四个点开始搜
        PointWithDirection s = new PointWithDirection(new Point(start.point), start.direction);
        Queue<PointWithDirection> queue = new ArrayDeque<>();
        ArrayList<PointWithDirection> twoDistancesPoints = new ArrayList<>();
        queue.offer(s);
        cs[s.point.x][s.point.y][s.direction] = s.direction;
        int minDistance = Integer.MAX_VALUE;
        PointWithDirection minMidPoint = null;
        int visitAroundBerthCount = 0;
        boolean[][] visits = new boolean[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
        for (boolean[] visit : visits) {
            Arrays.fill(visit, false);
        }
        int deep = 0;
        while (!queue.isEmpty() || !twoDistancesPoints.isEmpty()) {
            if (deep > maxDeep || visitAroundBerthCount == aroundPointsCount) {
                //泊位周围得点全部遍历了一遍，或者超过最大深度
                if (minMidPoint == null) {
                    return null;
                }
                //回溯路径
                return backTrackPath(gameMap, minMidPoint, cs);
            }
            deep += 1;
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
                if (!visits[top.point.x][top.point.y] && gameMap.boatGetFlashBerthId(top.point.x, top.point.y) == berthId) {
                    //第二次访问距离肯定更长
                    int comeDeep = deep - 1;
                    int nextDeep = 1 + 2 * (abs(top.point.x - berthCorePoint.x) + abs(top.point.y - berthCorePoint.y));
                    visits[top.point.x][top.point.y] = true;
                    visitAroundBerthCount++;
                    if (comeDeep + nextDeep < minDistance) {
                        minDistance = comeDeep + nextDeep;
                        minMidPoint = top;
                    }
                }
                nextEnterQueue(gameMap, cs, deep, queue, twoDistancesPoints, top);
            }
        }
        if (minMidPoint == null) {
            return null;
        }
        //回溯路径
        return backTrackPath(gameMap, minMidPoint, cs);
    }

    private static ArrayList<PointWithDirection> backTrackPath(GameMap gameMap, PointWithDirection top, int[][][] cs) {
        ArrayList<PointWithDirection> result = new ArrayList<>();
        PointWithDirection t = top;
        result.add(t);
        while ((cs[t.point.x][t.point.y][t.direction] >> 2) != 0) {
            int lastDir = cs[t.point.x][t.point.y][t.direction] & 3;
            t = getLastPointWithDirection(gameMap, result, t, lastDir);
        }
        Collections.reverse(result);
        ArrayList<PointWithDirection> tmp = new ArrayList<>();
        tmp.add(result.get(0));
        for (int i = 1; i < result.size(); i++) {
            if (gameMap.boatHasOneInMainChannel(result.get(i).point, result.get(i).direction)) {
                tmp.add(result.get(i));
            }
            tmp.add(result.get(i));
        }
        result = tmp;
        return result;
    }

    private static void nextEnterQueue(GameMap gameMap, int[][][] cs, int deep, Queue<PointWithDirection> queue, ArrayList<PointWithDirection> twoDistancesPoints, PointWithDirection top) {
        for (int k = 0; k < 3; k++) {
            PointWithDirection next = getNextPoint(top, k);
            //合法性判断
            if (deep >= (cs[next.point.x][next.point.y][next.direction] >> 2)
                    || !gameMap.boatCanReach(next.point, next.direction)) {
                continue;
            }
            //是否到达之后需要恢复,有一个点进入了主航道
            if (gameMap.boatHasOneInMainChannel(next.point, next.direction)) {
                if (deep + 1 >= (cs[next.point.x][next.point.y][next.direction] >> 2)) {
                    continue;
                }
                cs[next.point.x][next.point.y][next.direction]
                        = ((deep + 1) << 2) + top.direction;
                twoDistancesPoints.add(next);
            } else {
                cs[next.point.x][next.point.y][next.direction]
                        = (deep << 2) + top.direction;
                queue.offer(next);
            }
        }
    }

    //todo 船搜一条不撞对面的路径

}
