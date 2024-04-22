package com.huawei.codecraft;

import java.util.*;

import static com.huawei.codecraft.Utils.*;
import static com.huawei.codecraft.Constants.*;
import static java.lang.Math.abs;

public class BoatUtils {


    public static ArrayList<PointWithDirection> boatMoveToBerthSellPoint(GameMap gameMap, PointWithDirection start,
                                                                         PointWithDirection end, int berthId
            , PointWithDirection berthCorePoint
            , int recoveryTime, int maxDeep, int curBoatId
            , ArrayList<ArrayList<PointWithDirection>> otherPaths, ArrayList<Integer> otherIds
            , short[][][] heuristicCs, boolean[][] conflictPoints) {
        short[][][] cs = gameMap.boatCommonCs;
        gameMap.curVisitId++;
        int curVisitId = gameMap.curVisitId;
        int[][][] visits = gameMap.boatVisits;
        //从目标映射的四个点开始搜
        int deep = 0;
        PointWithDirection s = new PointWithDirection(new Point(start.point), start.direction);
        Queue<PointWithDirection> queue = new ArrayDeque<>();
        queue.offer(s);
        cs[s.point.x][s.point.y][s.direction] = (short) s.direction;
        visits[s.point.x][s.point.y][s.direction] = curVisitId;
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
            PointWithDirection bestPoint = null;
            int bestDeep = Integer.MAX_VALUE;
            for (int j = 0; j < size; j++) {
                count++;
                PointWithDirection top = queue.poll();
                assert top != null;
                if (heuristicCs != null && deep - 1 + (heuristicCs[top.point.x][top.point.y][top.direction] >> 2) > maxDeep) {
                    continue;
                }
                if (berthId != -1) {
                    if (gameMap.boatGetFlashBerthId(top.point.x, top.point.y) == berthId) {
                        int nextDeep = 1 + 2 * (abs(top.point.x - berthCorePoint.point.x) + abs(top.point.y - berthCorePoint.point.y));
                        int curDeep = deep - 1 + nextDeep;
                        assert heuristicCs != null;
                        assert curDeep >= (heuristicCs[start.point.x][start.point.y][start.direction] >> 2);
                        if (deep + nextDeep <= maxDeep && curDeep < bestDeep) {
                            //否则继续往前走
                            bestPoint = top;
                            bestDeep = curDeep;
                        }
                    }
                } else {
                    if (top.equal(end) || (top.point.equal(end.point) && end.direction == -1)) {
                        //回溯路径
                        bestPoint = top;
                        break;
                    }
                }
                for (int k = 0; k < 3; k++) {
                    PointWithDirection next = getNextPoint(top, k);
                    //合法性判断
                    if (!gameMap.boatCanReach(next.point, next.direction) ||
                            (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                                    deep >= (cs[next.point.x][next.point.y][next.direction] >> 2))
                    ) {
                        continue;
                    }
                    //检测碰撞。
                    if (otherPaths != null) {
                        if (boatCheckCrashInDeep(gameMap, recoveryTime + deep, curBoatId, next, otherPaths, otherIds))
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
                        if (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                                deep + 1 >= (cs[next.point.x][next.point.y][next.direction] >> 2)) {
                            continue;//visit过且深度大于已有的,剪枝
                        }
                        //检测碰撞。
                        if (otherPaths != null) {
                            if (boatCheckCrashInDeep(gameMap, recoveryTime + deep + 1, curBoatId, next, otherPaths, otherIds))
                                continue;
                        }
                        cs[next.point.x][next.point.y][next.direction]
                                = (short) (((deep + 1) << 2) + top.direction);
                        twoDistancesPoints.add(next);
                    } else {
                        cs[next.point.x][next.point.y][next.direction]
                                = (short) ((deep << 2) + top.direction);
                        queue.offer(next);
                    }
                    visits[next.point.x][next.point.y][next.direction] = curVisitId;
                }

            }
            if (bestPoint != null) {
                //回溯路径
                ArrayList<PointWithDirection> path;
                if (berthId != -1) {
                    path = getBoatToBerthBackPath(gameMap, berthCorePoint, recoveryTime, bestPoint, cs);
                } else {
                    path = backTrackPath(gameMap, bestPoint, cs, recoveryTime);
                }
                if (path.size() == 1) {
                    path.add(path.get(0));
                }
            }
        }
        printError("error boat no find a path");
        return new ArrayList<>();
    }

    public static ArrayList<PointWithDirection> boatMoveToBerthSellPointHeuristic(GameMap gameMap, PointWithDirection start,
                                                                                  PointWithDirection end, int berthId
            , PointWithDirection berthCorePoint
            , int recoveryTime, int maxDeep, int curBoatId
            , ArrayList<ArrayList<PointWithDirection>> otherPaths, ArrayList<Integer> otherIds
            , short[][][] heuristicCs, boolean[][] conflictPoints) {
        short[][][] cs = gameMap.boatCommonCs;
        gameMap.curVisitId++;
        int curVisitId = gameMap.curVisitId;
        int[][][] visits = gameMap.boatVisits;
        //从目标映射的四个点开始搜
        class PointWithDeep {
            final PointWithDirection point;
            final int deep;

            public PointWithDeep(PointWithDirection point, int deep) {
                this.point = point;
                this.deep = deep;
            }
        }
        PointWithDeep s = new PointWithDeep(new PointWithDirection(new Point(start.point), start.direction), 0);
        Deque<PointWithDeep> queue = new ArrayDeque<>();
        queue.offer(s);
        cs[s.point.point.x][s.point.point.y][s.point.direction] = (short) s.point.direction;
        visits[s.point.point.x][s.point.point.y][s.point.direction] = curVisitId;
        int curDeep = (heuristicCs[s.point.point.x][s.point.point.y][s.point.direction] >> 2) + s.deep;
        TreeMap<Integer, Deque<PointWithDeep>> cacheMap = new TreeMap<>();
        cacheMap.put(curDeep, queue);
        PointWithDirection bestPoint = null;
        int count = 0;
        while (!cacheMap.isEmpty()) {
            Map.Entry<Integer, Deque<PointWithDeep>> dequeEntry = cacheMap.firstEntry();
            int totalDeep = dequeEntry.getKey();
            Deque<PointWithDeep> q = dequeEntry.getValue();
            PointWithDeep point = q.pollLast();
            assert point != null;
            int deep = point.deep;//最多16万个点,这个不是启发式，会一直搜
            if (totalDeep > maxDeep || count > MAP_FILE_ROW_NUMS * MAP_FILE_COL_NUMS / 4) {
                break;
            }
            PointWithDirection top = point.point;
            if (visits[top.point.x][top.point.y][top.direction] == curVisitId &&
                    point.deep > (cs[top.point.x][top.point.y][top.direction] >> 2)) {
                if (q.isEmpty()) {
                    cacheMap.remove(totalDeep);
                }
                continue;
            }
            //2距离的下一个点,先保存起来，后面直接插进去
            if (berthId != -1) {
                if (gameMap.boatGetFlashBerthId(top.point.x, top.point.y) == berthId) {
                    int nextDeep = 1 + 2 * (abs(top.point.x - berthCorePoint.point.x) + abs(top.point.y - berthCorePoint.point.y));
                    assert deep + nextDeep >= (heuristicCs[start.point.x][start.point.y][start.direction] >> 2);
                    if (deep + nextDeep <= maxDeep) {
                        //否则继续往前走
                        bestPoint = top;
                        break;
                    }
                }
            } else {
                if (top.equal(end) || (top.point.equal(end.point) && end.direction == -1)) {
                    //回溯路径
                    bestPoint = top;
                    break;
                }
            }
            deep += 1;
            for (int k = 2; k >= 0; k--) {
                count++;
                PointWithDirection next = getNextPoint(top, k);
                //合法性判断
                if (!gameMap.boatCanReach(next.point, next.direction) ||
                        (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                                deep >= (cs[next.point.x][next.point.y][next.direction] >> 2))) {
                    continue;
                }
                //检测碰撞。
                if (otherPaths != null) {
                    if (boatCheckCrashInDeep(gameMap, recoveryTime + deep, curBoatId, next, otherPaths, otherIds))
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
                int nextDeep = deep;
                if (gameMap.boatHasOneInMainChannel(next.point, next.direction)) {
                    if (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                            deep + 1 >= (cs[next.point.x][next.point.y][next.direction] >> 2)) {
                        continue;//visit过且深度大于已有的,剪枝
                    }
                    //检测碰撞。
                    if (otherPaths != null) {
                        if (boatCheckCrashInDeep(gameMap, recoveryTime + deep + 1, curBoatId, next, otherPaths, otherIds))
                            continue;
                    }
                    cs[next.point.x][next.point.y][next.direction]
                            = (short) (((deep + 1) << 2) + top.direction);
                    nextDeep += 1;
                } else {
                    cs[next.point.x][next.point.y][next.direction]
                            = (short) ((deep << 2) + top.direction);
                }
                visits[next.point.x][next.point.y][next.direction] = curVisitId;
                PointWithDeep pointWithDeep = new PointWithDeep(next, nextDeep);
                nextDeep += (heuristicCs[next.point.x][next.point.y][next.direction] >> 2);
                if (!cacheMap.containsKey(nextDeep)) {
                    cacheMap.put(nextDeep, new ArrayDeque<>());
                }
                cacheMap.get(nextDeep).addLast(pointWithDeep);
            }
            if (q.isEmpty()) {
                cacheMap.remove(totalDeep);
            }
        }
        if (bestPoint != null) {
            //回溯路径
            ArrayList<PointWithDirection> path;
            if (berthId != -1) {
                path = getBoatToBerthBackPath(gameMap, berthCorePoint, recoveryTime, bestPoint, cs);
            } else {
                path = backTrackPath(gameMap, bestPoint, cs, recoveryTime);
            }
            if (path.size() == 1) {
                path.add(path.get(0));
            }
        }
        printError("error boat no find a path");
        return new ArrayList<>();
    }

    private static ArrayList<PointWithDirection> getBoatToBerthBackPath(GameMap gameMap, PointWithDirection berthCorePoint, int recoveryTime
            , PointWithDirection minMidPoint, short[][][] cs) {
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
    public static ArrayList<PointWithDirection> boatGetSafePoints(GameMap gameMap, short[][][] cs, PointWithDirection start
            , boolean[][] conflictPoints
            , boolean[][] noResultPoints
            , int maxResultCount) {
        //从目标映射的四个点开始搜
        //主航道点解除冲突和非结果
        gameMap.curVisitId++;
        int curVisitId = gameMap.curVisitId;
        int[][][] visits = gameMap.boatVisits;
        PointWithDirection s = new PointWithDirection(new Point(start.point), start.direction);
        Queue<PointWithDirection> queue = new ArrayDeque<>();
        queue.offer(s);
        int deep = 0;
        cs[s.point.x][s.point.y][s.direction] = (short) s.direction;
        visits[s.point.x][s.point.y][s.direction] = curVisitId;
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
                for (int k = 0; k < 3; k++) {
                    PointWithDirection next = getNextPoint(top, k);
                    if (!gameMap.boatCanReach(next.point, next.direction) ||
                            (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                                    deep >= (cs[next.point.x][next.point.y][next.direction] >> 2))) {
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
                        if (visits[next.point.x][next.point.y][next.direction] == curVisitId &&
                                deep + 1 >= (cs[next.point.x][next.point.y][next.direction] >> 2)) {
                            continue;//visit过且深度大于已有的,剪枝
                        }
                        cs[next.point.x][next.point.y][next.direction]
                                = (short) (((deep + 1) << 2) + top.direction);
                        twoDistancesPoints.add(next);
                    } else {
                        cs[next.point.x][next.point.y][next.direction]
                                = (short) ((deep << 2) + top.direction);
                        queue.offer(next);
                    }
                    visits[next.point.x][next.point.y][next.direction] = curVisitId;
                }
            }
        }
        return result;
    }

    private static boolean checkIfExcludePoint(GameMap gameMap, boolean[][] conflictPoints, PointWithDirection top) {
        ArrayList<Point> points = gameMap.getBoatPoints(top.point, top.direction);
        for (Point point : points) {
            if (conflictPoints[point.x][point.y] && !gameMap.isBoatMainChannel(point.x, point.y)) {
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
            for (int deep = 1; deep < myPaths.size(); deep++) {
                //从我的移动后的点开始
                PointWithDirection pointWithDirection = myPaths.get(deep);
                if (deep < otherPath.size()) {
                    boolean crash = boatCheckCrash(gameMap, pointWithDirection, otherPath.get(deep));
                    if (crash) {
                        assert (boatCheckCrashInDeep(gameMap, deep, myId, pointWithDirection, otherPaths, otherIds));
                        return otherId;
                    }
                }
                if (otherId < myId) {
                    //我的这帧撞他的这帧，他的下一位置撞我的这位置
                    if (deep + 1 < otherPath.size()) {
                        boolean crash = boatCheckCrash(gameMap, pointWithDirection, otherPath.get(deep + 1));
                        if (crash) {
                            assert (boatCheckCrashInDeep(gameMap, deep, myId, pointWithDirection, otherPaths, otherIds));
                            return otherId;
                        }
                    }
                } else {
                    //我的这帧撞他的前一帧，他的这一帧撞我的这帧
                    if (deep - 1 < otherPath.size()) {
                        boolean crash = boatCheckCrash(gameMap, pointWithDirection, otherPath.get(deep - 1));
                        if (crash) {
                            assert (boatCheckCrashInDeep(gameMap, deep, myId, pointWithDirection, otherPaths, otherIds));
                            return otherId;
                        }
                    }
                }

                if (2 * deep > maxDetectDistance) {
                    //假设一人走一步，则需要乘以两倍
                    break;
                }
            }
        }
        return -1;
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


    public static ArrayList<PointWithDirection> backTrackPath(GameMap gameMap, PointWithDirection top, short[][][] cs, int recoveryTime) {
        ArrayList<PointWithDirection> result = new ArrayList<>();
        PointWithDirection t = top;
        result.add(t);
        while ((cs[t.point.x][t.point.y][t.direction] >> 2) != 0) {
            int lastDir = cs[t.point.x][t.point.y][t.direction] & 3;
            t = getLastPointWithDirection(gameMap, result, t, lastDir);
        }
        Collections.reverse(result);
        result = boatFixedPath(gameMap, recoveryTime, result);
        return result;
    }

    static ArrayList<PointWithDirection> boatFixedPath(GameMap gameMap, int recoveryTime, ArrayList<PointWithDirection> result) {
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
        return tmp;
    }

    private static boolean boatCheckCrashInDeep(GameMap gameMap, int deep, int boatId, PointWithDirection point
            , ArrayList<ArrayList<PointWithDirection>> otherPaths, ArrayList<Integer> otherIds) {
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

}
