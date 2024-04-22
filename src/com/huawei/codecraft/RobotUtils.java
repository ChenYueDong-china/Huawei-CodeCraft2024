package com.huawei.codecraft;

import java.util.*;

import static com.huawei.codecraft.Constants.MAP_FILE_COL_NUMS;
import static com.huawei.codecraft.Constants.MAP_FILE_ROW_NUMS;
import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.abs;
import static java.lang.Math.min;

public class RobotUtils {
//
//    public static ArrayList<Point> robotMoveToPointHeuristic(GameMap gameMap, Point start, Point end
//            , ArrayList<ArrayList<Point>> otherPaths, int[][] heuristicCs) {
//        start = gameMap.posToDiscrete(start);
//        end = gameMap.posToDiscrete(end);
//        if (start.equal(end)) {
//            //输出路径
//            ArrayList<Point> result = new ArrayList<>();
//            for (int i = 0; i < 3; i++) {
//                result.add(start);//3个,一次两步
//            }
//            return result;
//        }
//        //发现一条更好的路径,启发式搜
//        class Pair {
//            final int deep;
//            final Point p;
//
//            public Pair(int deep, Point p) {
//                this.deep = deep;
//                this.p = p;
//            }
//        }
//        int[][] discreteCs = gameMap.robotCommonDiscreteCs; //前面2位是距离，后面的位数是距离0xdistdir
//        for (int[] discreteC : discreteCs) {
//            Arrays.fill(discreteC, Integer.MAX_VALUE);
//        }
//
//        Deque<Pair> stack = new ArrayDeque<>();
//        stack.push(new Pair(0, start));
//        discreteCs[start.x][start.y] = 0;
//        while (!stack.isEmpty()) {
//            Point top = stack.peekLast().p;
//            if (top.equal(end)) {
//                //回溯路径
//                ArrayList<Point> result = getRobotPathByCs(discreteCs, top);
//                Collections.reverse(result);
//                return result;
//            }
//            assert stack.peekLast() != null;
//            int deep = stack.peekLast().deep;
//            stack.pop();
//            for (int i = 0; i < DIR.length / 2; i++) {
//                //四方向的
//                Point point = getNextPoint(gameMap, otherPaths, discreteCs, top, i, deep, heuristicCs);
//                if (point == null) continue;
//                stack.push(new Pair(deep + 2, point));
//            }
//        }
//        //搜不到，走原路
//        return new ArrayList<>();
//    }
//
//    public static ArrayList<Point> robotMoveToPoint(GameMap gameMap, Point start, Point end
//            , int maxDeep, ArrayList<ArrayList<Point>> otherPaths) {
//        return robotMoveToPoint(gameMap, start, end, -1, maxDeep, otherPaths);
//    }
//
//    public static ArrayList<Point> robotMoveToBerth(GameMap gameMap, Point start, int berthId
//            , int maxDeep, ArrayList<ArrayList<Point>> otherPaths) {
//        assert berthId != -1;
//        return robotMoveToPoint(gameMap, start, new Point(-1, -1), berthId, maxDeep, otherPaths);
//    }
//

    public static ArrayList<Point> robotMoveToPointBerthHeuristicCs(GameMap gameMap, Point start, int berthId
            , Point end, int maxDeep
            , ArrayList<ArrayList<Point>> otherPaths
            , short[][] heuristicCs, boolean[][] conflictPoints) {
        //已经在了，直接返回，防止cs消耗
        Point s = new Point(start);
        gameMap.curVisitId++;
        int curVisitId = gameMap.curVisitId;
        int[][] visits = gameMap.robotVisits;
        short[][] cs = gameMap.robotCommonCs;
        class PointWithDeep {
            final Point point;
            final int deep;

            public PointWithDeep(Point point, int deep) {
                this.point = point;
                this.deep = deep;
            }
        }
        Deque<PointWithDeep> queue = new ArrayDeque<>();
        queue.offer(new PointWithDeep(s, 0));
        cs[s.x][s.y] = 0;
        visits[s.x][s.y] = curVisitId;
        int curDeep = (heuristicCs[s.x][s.y] >> 2);
        TreeMap<Integer, Deque<PointWithDeep>> cacheMap = new TreeMap<>();
        cacheMap.put(curDeep, queue);

        int count = 0;
        //从目标映射的四个点开始搜
        while (!cacheMap.isEmpty()) {
            Map.Entry<Integer, Deque<PointWithDeep>> dequeEntry = cacheMap.firstEntry();
            Deque<PointWithDeep> q = dequeEntry.getValue();
            PointWithDeep point = q.pollLast();
            assert point != null;
            int deep = point.deep;//最多8万个点,这个不是启发式，会一直搜
            if (dequeEntry.getKey() > maxDeep || count > MAP_FILE_ROW_NUMS * MAP_FILE_COL_NUMS / 8) {
                break;
            }
            Point top = point.point;
            boolean arrive = false;
            if (berthId != -1) {
                if (gameMap.getBelongToBerthId(top) == berthId) {
                    //回溯路径
                    arrive = true;
                }
            } else {
                if (top.equal(end)) {
                    //回溯路径
                    arrive = true;
                }
            }
            if (arrive) {
                ArrayList<Point> result = getRobotPathByCs(cs, top);
                Collections.reverse(result);
                if (result.size() == 1) {
                    result.add(result.get(0));
                }
                return gameMap.toDiscretePath(result);
            }
            Point pre = gameMap.posToDiscrete(top);
            for (int j = DIR.length / 2 - 1; j >= 0; j--) {
                //四方向的
                count++;
                Point dir = DIR[j];
                int dx = top.x + dir.x;
                int dy = top.y + dir.y;//第一步
                if (!gameMap.robotCanReach(dx, dy) || visits[dx][dy] == curVisitId) {
                    //不会发生后面有更近点得可能
                    continue;
                }
                if (conflictPoints != null) {
                    if (conflictPoints[dx][dy] && !gameMap.isRobotMainChannel(dx, dy)) {
                        continue;//这个点被锁死了
                    }
                }
                //转成离散去避让
                Point next = gameMap.posToDiscrete(dx, dy);
                Point mid = pre.add(dir);
                int midDeep = 2 * deep - 1;
                int nextDeep = 2 * deep;
                if (otherPaths != null) {
                    if (robotCheckCrashInDeep(gameMap, midDeep, mid.x, mid.y, otherPaths)
                            || robotCheckCrashInDeep(gameMap, nextDeep, next.x, next.y, otherPaths)) {
                        continue;
                    }
                }
                cs[dx][dy] = (short) ((deep << 2) + j);
                visits[dx][dy] = curVisitId;
                nextDeep = deep + 1;
                PointWithDeep pointWithDeep = new PointWithDeep(next, nextDeep);
                nextDeep += (heuristicCs[next.x][next.y] >> 2);
                if (!cacheMap.containsKey(nextDeep)) {
                    cacheMap.put(nextDeep, new ArrayDeque<>());
                }
                cacheMap.get(nextDeep).addLast(pointWithDeep);
            }
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("all")
    public static ArrayList<Point> robotMoveToPointBerth(GameMap gameMap, Point start, int berthId
            , Point end, int maxDeep
            , ArrayList<ArrayList<Point>> otherPaths
            , short[][] heuristicCs, boolean[][] conflictPoints) {
        //已经在了，直接返回，防止cs消耗
        Point s = new Point(start);
        Queue<Point> queue = new ArrayDeque<>();
        gameMap.curVisitId++;
        int curVisitId = gameMap.curVisitId;
        int[][] visits = gameMap.robotVisits;
        short[][] cs = gameMap.robotCommonCs;
        queue.offer(s);
        cs[s.x][s.y] = 0;
        visits[s.x][s.y] = curVisitId;
        int deep = 0;
        int count = 0;
        //从目标映射的四个点开始搜
        while (!queue.isEmpty()) {
            if (deep > maxDeep) {
                break;
            }
            deep += 1;
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                count++;
                Point top = queue.poll();
                assert top != null;
                if (heuristicCs != null && deep - 1 + (heuristicCs[top.x][top.y] >> 2) > maxDeep) {
                    continue;
                }
                boolean arrive = false;
                if (berthId != -1) {
                    if (gameMap.getBelongToBerthId(top) == berthId) {
                        //回溯路径
                        arrive = true;
                    }
                } else {
                    if (top.equal(end)) {
                        //回溯路径
                        arrive = true;
                    }
                }
                if (arrive) {
                    ArrayList<Point> result = getRobotPathByCs(cs, top);
                    Collections.reverse(result);
                    if (result.size() == 1) {
                        result.add(result.get(0));
                    }
                    return gameMap.toDiscretePath(result);
                }
                Point pre = gameMap.posToDiscrete(top);
                for (int j = 0; j < DIR.length / 2; j++) {
                    //四方向的
                    int lastDirIdx = cs[top.x][top.y] & 3;
                    int dirIdx = j ^ lastDirIdx; // 优先遍历上一次过来的方向
                    Point dir = DIR[dirIdx];
                    int dx = top.x + dir.x;
                    int dy = top.y + dir.y;//第一步
                    if (!gameMap.robotCanReach(dx, dy) || visits[dx][dy] == curVisitId) {
                        continue;
                    }
                    if (conflictPoints != null) {
                        if (conflictPoints[dx][dy] && !gameMap.isRobotMainChannel(dx, dy)) {
                            continue;//这个点被锁死了
                        }
                    }
                    //转成离散去避让
                    Point next = gameMap.posToDiscrete(dx, dy);
                    Point mid = pre.add(dir);
                    int midDeep = 2 * deep - 1;
                    int nextDeep = 2 * deep;
                    if (otherPaths != null) {
                        if (robotCheckCrashInDeep(gameMap, midDeep, mid.x, mid.y, otherPaths)
                                || robotCheckCrashInDeep(gameMap, nextDeep, next.x, next.y, otherPaths)) {
                            continue;
                        }
                    }
                    cs[dx][dy] = (short) ((deep << 2) + dirIdx);
                    visits[dx][dy] = curVisitId;
                    queue.offer(new Point(dx, dy));
                }
            }
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("all")
    public static ArrayList<Point> robotMoveToBerth(GameMap gameMap, Point start, int berthId, short[][] heuristicCs) {
        //已经在了，直接返回，防止cs消耗
        //从目标映射的四个点开始搜
        Point s = new Point(start);
        Queue<Point> queue = new ArrayDeque<>();
        gameMap.curVisitId++;
        int curVisitId = gameMap.curVisitId;
        int[][] visits = gameMap.robotVisits;
        short[][] cs = gameMap.robotCommonCs;
        queue.offer(s);
        cs[s.x][s.y] = 0;
        visits[s.x][s.y] = curVisitId;
        int deep = 0;
        int count = 0;
        while (!queue.isEmpty()) {
            deep++;
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                count++;
                Point top = queue.poll();
                assert top != null;
                if (gameMap.getBelongToBerthId(top) == berthId) {
                    //回溯路径
                    ArrayList<Point> result = getRobotPathByCs(cs, top);
                    Collections.reverse(result);
                    if (result.size() == 1) {
                        result.add(result.get(0));
                    }
                    return gameMap.toDiscretePath(result);
                }
                for (int j = 0; j < DIR.length / 2; j++) {
                    //四方向的
                    int lastDirIdx = cs[top.x][top.y] & 3;
                    int dirIdx = j ^ lastDirIdx; // 优先遍历上一次过来的方向
                    Point dir = DIR[dirIdx];
                    int dx = top.x + dir.x;
                    int dy = top.y + dir.y;//第一步
                    if (!gameMap.robotCanReach(dx, dy) || visits[dx][dy] == curVisitId) {
                        continue;
                    }
                    if ((heuristicCs[dx][dy] >> 2) >= (heuristicCs[top.x][top.y] >> 2)) {
                        //启发式剪枝，不是距离更近则直接结束
                        continue;
                    }
                    cs[dx][dy] = (short) ((deep << 2) + dirIdx);
                    visits[dx][dy] = curVisitId;
                    queue.offer(new Point(dx, dy));
                }
            }
        }
        return new ArrayList<>();
    }


    private static boolean robotCheckCrashInDeep(GameMap gameMap, int deep, int x, int y, ArrayList<ArrayList<Point>> otherPaths) {
        if (otherPaths != null) {
            for (ArrayList<Point> otherPath : otherPaths) {
                if (deep < otherPath.size() && otherPath.get(deep).equal(x, y)
                        && !gameMap.isRobotDiscreteMainChannel(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean robotCheckCrash(GameMap gameMap, ArrayList<Point> myPath, ArrayList<ArrayList<Point>> otherPaths) {
        //默认检测整条路径
        for (ArrayList<Point> otherPath : otherPaths) {
            int detectDistance = min(otherPath.size(), myPath.size());
            for (int j = 0; j < detectDistance; j++) {
                Point point = otherPath.get(j);
                if (point.equal(myPath.get(j)) && !gameMap.isRobotDiscreteMainChannel(point.x, point.y)) {
                    return true;
                }
            }
        }
        return false;
    }


}
