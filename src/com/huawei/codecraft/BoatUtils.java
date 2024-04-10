package com.huawei.codecraft;

import java.util.*;

import static com.huawei.codecraft.Utils.*;
import static com.huawei.codecraft.Constants.*;
import static java.lang.Math.abs;

public class BoatUtils {

    @SuppressWarnings("all")
    public static ArrayList<PointWithDirection> boatMoveToBerthHeuristic(GameMap gameMap, PointWithDirection start, int berthId
            , PointWithDirection berthCorePoint
            , int recoveryTime, int[][][] heuristicDistance) {
        if (start.point.equal(berthCorePoint.point)) {
            //直接返回
            return getBoatToBerthEqualPath(start, berthCorePoint, recoveryTime);
        }
        int[][][] cs = gameMap.boatCommonCs;
        resetCs(cs);
        //从目标映射的四个点开始搜
        int deep = 0;
        PointWithDirection s = new PointWithDirection(new Point(start.point), start.direction);
        Queue<PointWithDirection> queue = new ArrayDeque<>();
        queue.offer(s);
        cs[s.point.x][s.point.y][s.direction] = s.direction;
        ArrayList<PointWithDirection> twoDistancesPoints = new ArrayList<>();
        int count = 0;
        while (!queue.isEmpty() || !twoDistancesPoints.isEmpty()) {
            deep += 1;
            //2距离的下一个点,先保存起来，后面直接插进去
            int size = queue.size();
            if (!twoDistancesPoints.isEmpty()) {
                queue.addAll(twoDistancesPoints);
                twoDistancesPoints.clear();
            }
            for (int j = 0; j < size; j++) {
                count++;
                PointWithDirection top = queue.poll();
                assert top != null;
                if (gameMap.boatGetFlashBerthId(top.point.x, top.point.y) == berthId) {
                    //第二次访问距离肯定更长
                    int nextDeep = 1 + 2 * (abs(top.point.x - berthCorePoint.point.x) + abs(top.point.y - berthCorePoint.point.y));
                    int minDeep = heuristicDistance[top.point.x][top.point.y][top.direction];
                    int curDeep = deep - 1;
                    if (normal_init_success) {
                        //没正常初始化成功说明这个可能还小于
                        assert curDeep + nextDeep >= heuristicDistance[start.point.x][start.point.y][start.direction];
                    }
                    //可能启发距离不是最好的，此时小于都行，因为可能初始化超时
                    if (minDeep == nextDeep && curDeep + nextDeep
                            <= heuristicDistance[start.point.x][start.point.y][start.direction]) {
                        return getBoatToBerthBackPath(gameMap, berthCorePoint, recoveryTime, top, cs);
                    }
                }
                nextEnterQueue(gameMap, cs, deep, queue, twoDistancesPoints, top, -1
                        , 0, null, null, null, heuristicDistance);
            }
        }
        printError("error boat no find a min path");
        return new ArrayList<>();
    }

    private static void resetCs(int[][][] boatCommonCs) {
        for (int[][] c1 : boatCommonCs) {
            for (int[] c2 : c1) {
                Arrays.fill(c2, Integer.MAX_VALUE);
            }
        }
    }

    private static ArrayList<PointWithDirection> getBoatToBerthBackPath(GameMap gameMap, PointWithDirection berthCorePoint, int recoveryTime, PointWithDirection minMidPoint, int[][][] cs) {
        //回溯路径
        ArrayList<PointWithDirection> path = backTrackPath(gameMap, minMidPoint, cs, recoveryTime);
        //结束点添加
        PointWithDirection end = new PointWithDirection(berthCorePoint.point, berthCorePoint.direction);
        PointWithDirection lastTwoPoint = path.get(path.size() - 1);
        //一帧闪现到达，剩下的是闪现恢复时间
        int waitTime = 1 + 2 * (abs(lastTwoPoint.point.x - end.point.x) + abs(lastTwoPoint.point.y - end.point.y));
        for (int i = 0; i < waitTime; i++) {
            path.add(end);
        }
        return path;
    }

    @SuppressWarnings("all")
    public static ArrayList<PointWithDirection> boatGetSafePoints(GameMap gameMap, int[][][] cs, PointWithDirection start, boolean[][] conflictPoints, boolean[][] noResultPoints
            , int maxResultCount) {
        //从目标映射的四个点开始搜
        //主航道点解除冲突和非结果
        for (int[][] c1 : cs) {
            for (int[] c2 : c1) {
                Arrays.fill(c2, Integer.MAX_VALUE);
            }
        }
        PointWithDirection s = new PointWithDirection(new Point(start.point), start.direction);
        Queue<PointWithDirection> queue = new ArrayDeque<>();
        queue.offer(s);
        int deep = 0;
        cs[s.point.x][s.point.y][s.direction] = s.direction;
        ArrayList<PointWithDirection> twoDistancesPoints = new ArrayList<>();
        ArrayList<PointWithDirection> result = new ArrayList<>();
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
                if (result.size() > maxResultCount) {
                    return result;
                }
                PointWithDirection top = queue.poll();
                assert top != null;
                if (noResultPoints != null) {
                    //输出路径
                    boolean crash = checkIfExcludePoint(gameMap, noResultPoints, top);
                    if (!crash) {
                        result.add(top);
                    }
                }
                nextEnterQueue(gameMap, cs, deep, queue, twoDistancesPoints, top, conflictPoints);
            }
        }
        return result;
    }

    private static boolean checkIfExcludePoint(GameMap gameMap, boolean[][] conflictPoints, PointWithDirection top) {
        ArrayList<Point> points = gameMap.getBoatPoints(top.point, top.direction);
        for (Point point : points) {
            if (conflictPoints[point.x][point.y]) {
                return true;
            }
        }
        return false;
    }


    public static int boatCheckCrash(GameMap gameMap, int myId, ArrayList<PointWithDirection> myPaths
            , ArrayList<ArrayList<PointWithDirection>> otherPaths, ArrayList<Integer> otherIds
            , int maxDetectDistance) {
        for (int i = 0; i < otherPaths.size(); i++) {
            ArrayList<PointWithDirection> otherPath = otherPaths.get(i);
            int otherId = otherIds.get(i);
            assert myId != otherId;
            int curDistance = 0;
            for (int deep = 1; deep < myPaths.size(); deep++) {
                //从我的移动后的点开始
                PointWithDirection pointWithDirection = myPaths.get(deep);
                if (deep < otherPath.size()) {
                    boolean crash = boatCheckCrash(gameMap, pointWithDirection, otherPath.get(deep));
                    if (crash) {
                        return otherId;
                    }
                }
                if (otherId < myId) {
                    //我的这帧撞他的这帧，他的下一位置撞我的这位置
                    if (deep + 1 < otherPath.size()) {
                        boolean crash = boatCheckCrash(gameMap, pointWithDirection, otherPath.get(deep + 1));
                        if (crash) {
                            return otherId;
                        }
                    }
                } else {
                    //我的这帧撞他的前一帧，他的这一帧撞我的这帧
                    if (deep - 1 < otherPath.size()) {
                        boolean crash = boatCheckCrash(gameMap, pointWithDirection, otherPath.get(deep - 1));
                        if (crash) {
                            return otherId;
                        }
                    }
                }

                PointWithDirection last = myPaths.get(deep - 1);
                curDistance += abs(pointWithDirection.point.x - last.point.x) + abs(pointWithDirection.point.y - last.point.y);
                if (2 * curDistance > maxDetectDistance) {
                    //假设一人走一步，则需要乘以两倍
                    return -1;
                }
            }
        }
        return -1;
    }

    public static ArrayList<PointWithDirection> boatMoveToPoint(GameMap gameMap, PointWithDirection start, PointWithDirection end
            , int maxDeep, int recoveryTime) {
        return boatMoveToPoint(gameMap, start, end, maxDeep, -1, null, null, recoveryTime);
    }

    public static ArrayList<PointWithDirection> boatMoveToPoint(GameMap gameMap, PointWithDirection start, PointWithDirection end
            , int maxDeep, int boatId, ArrayList<ArrayList<PointWithDirection>> otherPath, ArrayList<Integer> otherIds, int recoveryTime) {
        //已经在了，直接返回，防止cs消耗
        if (start.point.equal(end.point) && (start.direction == end.direction || end.direction == -1)) {
            //输出路径
            ArrayList<PointWithDirection> result = new ArrayList<>();
            result.add(start);//加两个，说明不动
            for (int i = 0; i < recoveryTime; i++) {
                result.add(result.get(0));
            }
            if (result.size() == 1) {
                result.add(start);//说明不动
            }
            return result;
        }
        int[][][] cs = gameMap.boatCommonCs;
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
                break;
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
                    return backTrackPath(gameMap, top, cs, recoveryTime);
                }
                nextEnterQueue(gameMap, cs, deep, queue, twoDistancesPoints, top, boatId, recoveryTime, otherPath, otherIds);
            }
        }
        return new ArrayList<>();
    }

    public static ArrayList<PointWithDirection> boatMoveToBerth(GameMap gameMap, PointWithDirection start, int berthId, PointWithDirection berthCorePoint
            , int maxDeep, int aroundPointsCount, int recoveryTime) {
        return boatMoveToBerth(gameMap, start, berthId, berthCorePoint, maxDeep, aroundPointsCount, -1, null, null, recoveryTime);
    }

    public static ArrayList<PointWithDirection> boatMoveToBerth(GameMap gameMap, PointWithDirection start, int berthId, PointWithDirection berthCorePoint
            , int maxDeep, int aroundPointsCount, int boatId, ArrayList<ArrayList<PointWithDirection>> otherPaths, ArrayList<Integer> otherIds, int recoveryTime) {
        if (start.point.equal(berthCorePoint.point)) {
            //直接返回
            return getBoatToBerthEqualPath(start, berthCorePoint, recoveryTime);
        }
        int[][][] cs = gameMap.boatCommonCs;
        resetCs(cs);
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
                break;
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
                    int nextDeep = 1 + 2 * (abs(top.point.x - berthCorePoint.point.x) + abs(top.point.y - berthCorePoint.point.y));
                    visits[top.point.x][top.point.y] = true;
                    visitAroundBerthCount++;
                    if (comeDeep + nextDeep < minDistance) {
                        minDistance = comeDeep + nextDeep;
                        minMidPoint = top;
                    }
                }
                nextEnterQueue(gameMap, cs, deep, queue, twoDistancesPoints, top, boatId, recoveryTime, otherPaths, otherIds);
            }
        }
        if (minMidPoint == null || minDistance > maxDeep) {
            return new ArrayList<>();
        }
        return getBoatToBerthBackPath(gameMap, berthCorePoint, recoveryTime, minMidPoint, cs);
    }

    private static ArrayList<PointWithDirection> getBoatToBerthEqualPath(PointWithDirection start, PointWithDirection berthCorePoint, int recoveryTime) {
        ArrayList<PointWithDirection> result = new ArrayList<>();
        result.add(start);
        for (int i = 0; i < recoveryTime; i++) {
            result.add(start);
        }
        //闪现一帧,如果本来就在也不会执行，多一帧也没啥
        PointWithDirection end = new PointWithDirection(berthCorePoint.point, berthCorePoint.direction);
        result.add(end);
        return result;
    }


    public static Point getLastPoint(GameMap gameMap, Point curPoint, int curDir, int lastDir) {
        //判断是否顺
        boolean clockwise = gameMap.getRotationDir(lastDir, curDir) == 0;
        PointWithDirection pointWithDirection = getBoatRotationPoint(new PointWithDirection(new Point(0, 0), lastDir), clockwise);
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
            next = getBoatRotationPoint(top, true);
        } else {
            next = getBoatRotationPoint(top, false);
        }
        return next;
    }


    public static ArrayList<PointWithDirection> backTrackPath(GameMap gameMap, PointWithDirection top, int[][][] cs, int recoveryTime) {
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
        for (int i = 0; i < recoveryTime; i++) {
            tmp.add(result.get(0));
        }
        for (int i = 1; i < result.size(); i++) {
            if (gameMap.boatHasOneInMainChannel(result.get(i).point, result.get(i).direction)) {
                tmp.add(result.get(i));
            }
            tmp.add(result.get(i));
        }
        result = tmp;
        return result;
    }

    private static void nextEnterQueue(GameMap gameMap, int[][][] cs, int deep, Queue<PointWithDirection> queue
            , ArrayList<PointWithDirection> twoDistancesPoints, PointWithDirection top, boolean[][] conflictPoints) {
        nextEnterQueue(gameMap, cs, deep, queue, twoDistancesPoints, top, -1, 0, null, null, conflictPoints);
    }

    private static void nextEnterQueue(GameMap gameMap, int[][][] cs, int deep, Queue<PointWithDirection> queue
            , ArrayList<PointWithDirection> twoDistancesPoints, PointWithDirection top, int boatId, int boatRecoveryTime
            , ArrayList<ArrayList<PointWithDirection>> otherPaths, ArrayList<Integer> otherIds) {
        nextEnterQueue(gameMap, cs, deep, queue, twoDistancesPoints, top, boatId, boatRecoveryTime, otherPaths, otherIds, null);
    }

    private static void nextEnterQueue(GameMap gameMap, int[][][] cs, int deep, Queue<PointWithDirection> queue
            , ArrayList<PointWithDirection> twoDistancesPoints, PointWithDirection top, int boatId, int boatRecoveryTime
            , ArrayList<ArrayList<PointWithDirection>> otherPaths, ArrayList<Integer> otherIds, boolean[][] conflictPoints) {
        nextEnterQueue(gameMap, cs, deep, queue, twoDistancesPoints, top, boatId, boatRecoveryTime, otherPaths, otherIds, conflictPoints
                , null);
    }

    private static void nextEnterQueue(GameMap gameMap, int[][][] cs, int deep, Queue<PointWithDirection> queue
            , ArrayList<PointWithDirection> twoDistancesPoints, PointWithDirection top, int boatId, int boatRecoveryTime
            , ArrayList<ArrayList<PointWithDirection>> otherPaths, ArrayList<Integer> otherIds, boolean[][] conflictPoints
            , int[][][] heuristicDistance) {
        for (int k = 0; k < 3; k++) {
            PointWithDirection next = getNextPoint(top, k);
            //合法性判断
            if (deep >= (cs[next.point.x][next.point.y][next.direction] >> 2)
                    || !gameMap.boatCanReach(next.point, next.direction)) {
                continue;
            }
            //启发
            if (heuristicDistance != null && heuristicDistance[next.point.x][next.point.y][next.direction]
                    >= heuristicDistance[top.point.x][top.point.y][top.direction]) {
                continue;
            }
            //检测碰撞。
            if (boatId != -1 && otherPaths != null) {
                if (boatCheckCrashInDeep(gameMap, boatRecoveryTime + deep, boatId, next, otherPaths, otherIds))
                    continue;
            }

            //判断冲突点
            if (conflictPoints != null) {
                boolean crash = checkIfExcludePoint(gameMap, conflictPoints, next);
                if (crash) {
                    continue;
                }
            }

            //是否到达之后需要恢复,有一个点进入了主航道
            if (gameMap.boatHasOneInMainChannel(next.point, next.direction)) {
                if (deep + 1 >= (cs[next.point.x][next.point.y][next.direction] >> 2)) {
                    continue;
                }
                //检测碰撞。
                if (boatId != -1 && otherPaths != null) {
                    if (boatCheckCrashInDeep(gameMap, boatRecoveryTime + deep + 1, boatId, next, otherPaths, otherIds))
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

    private static boolean boatCheckCrashInDeep(GameMap gameMap, int deep, int boatId, PointWithDirection point
            , ArrayList<ArrayList<PointWithDirection>> otherPaths, ArrayList<Integer> otherIds
    ) {
        boolean crash = false;
        for (int i = 0; i < otherPaths.size(); i++) {
            int otherId = otherIds.get(i);
            ArrayList<PointWithDirection> otherPath = otherPaths.get(i);
            if (deep < otherPath.size()) {
                crash = boatCheckCrash(gameMap, point, otherPath.get(deep));
                if (crash) {
                    break;
                }
            }
            if (otherId < boatId) {
                //我的这帧撞他的这帧，他的下一位置撞我的这位置
                if (deep + 1 < otherPath.size()) {
                    crash = boatCheckCrash(gameMap, point, otherPath.get(deep + 1));
                    if (crash) {
                        break;
                    }
                }
            } else {
                //我的这帧撞他的前一帧，他的这一帧撞我的这帧
                if (deep - 1 < otherPath.size()) {
                    crash = boatCheckCrash(gameMap, point, otherPath.get(deep - 1));
                    if (crash) {
                        break;
                    }
                }
            }
        }
        return crash;
    }

    public static boolean boatCheckCrash(GameMap gameMap, PointWithDirection myPoint, PointWithDirection otherPoint) {
        //是否有点重合
        ArrayList<Point> myPoints = gameMap.getBoatPoints(myPoint.point, myPoint.direction);
        ArrayList<Point> othersPoints = gameMap.getBoatPoints(otherPoint.point, otherPoint.direction);
        for (Point point : myPoints) {
            for (Point othersPoint : othersPoints) {
                if (point.equal(othersPoint) && !gameMap.isBoatMainChannel(point.x, point.y)) {
                    return true;
                }
            }
        }
        return false;
    }

    //todo 船搜一条不撞对面的路径

}
