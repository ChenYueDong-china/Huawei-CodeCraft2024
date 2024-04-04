package com.huawei.codecraft;

import java.util.*;

import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.abs;

public class RobotUtils {

    public static ArrayList<Point> robotMoveToPointHeuristic(GameMap gameMap, Point start, Point end
            , int maxDeep, ArrayList<ArrayList<Point>> otherPaths, int[][] heuristicCs) {
        start = gameMap.posToDiscrete(start);
        end = gameMap.posToDiscrete(end);
        if (start.equal(end)) {
            //输出路径
            ArrayList<Point> result = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                result.add(start);//3个,一次两步
            }
            return result;
        }
        //发现一条更好的路径,启发式搜
        class Pair {
            final int deep;
            final Point p;

            public Pair(int deep, Point p) {
                this.deep = deep;
                this.p = p;
            }
        }
        int[][] discreteCs = gameMap.robotCommonDiscreteCs; //前面2位是距离，后面的位数是距离0xdistdir
        for (int[] discreteC : discreteCs) {
            Arrays.fill(discreteC, Integer.MAX_VALUE);
        }

        Stack<Pair> stack = new Stack<>();
        stack.push(new Pair(0, start));
        discreteCs[start.x][start.y] = 0;
        while (!stack.empty()) {
            Point top = stack.peek().p;
            if (top.equal(end)) {
                //回溯路径
                ArrayList<Point> result = getRobotPathByCs(discreteCs, top);
                Collections.reverse(result);
                return result;
            }
            int deep = stack.peek().deep;
            stack.pop();
            for (int i = 0; i < DIR.length / 2; i++) {
                //四方向的
                Point point = getNextPoint(gameMap, otherPaths, heuristicCs, top, i, deep, discreteCs);
                if (point == null) continue;
                stack.push(new Pair(deep + 2, point));
            }
        }
        //搜不到，走原路
        return new ArrayList<>();
    }

    public static ArrayList<Point> robotMoveToPoint(GameMap gameMap, Point start, Point end
            , int maxDeep, ArrayList<ArrayList<Point>> otherPaths) {
        return robotMoveToPoint(gameMap, start, end, -1, maxDeep, otherPaths);
    }

    public static ArrayList<Point> robotMoveToBerth(GameMap gameMap, Point start, int berthId
            , int maxDeep, ArrayList<ArrayList<Point>> otherPaths) {
        assert berthId != -1;
        return robotMoveToPoint(gameMap, start, new Point(-1, -1), berthId, maxDeep, otherPaths);
    }

    private static ArrayList<Point> robotMoveToPoint(GameMap gameMap, Point start, Point end, int berthId
            , int maxDeep, ArrayList<ArrayList<Point>> otherPaths) {
        //已经在了，直接返回，防止cs消耗
        start = gameMap.posToDiscrete(start);
        end = gameMap.posToDiscrete(end);
        if ((berthId == -1 && start.equal(end)) || (berthId != -1 && gameMap.getDiscreteBelongToBerthId(start.x, start.y) == berthId)) {
            //输出路径
            ArrayList<Point> result = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                result.add(start);//3个
            }
            return result;
        }
        int[][] cs = gameMap.robotCommonDiscreteCs;
        for (int[] c : cs) {
            Arrays.fill(c, Integer.MAX_VALUE);
        }
        //从目标映射的四个点开始搜
        Point s = new Point(start);
        Queue<Point> queue = new ArrayDeque<>();
        queue.offer(s);
        cs[s.x][s.y] = 0;
        int deep = 0;
        int count = 0;
        while (!queue.isEmpty()) {
            if (deep > maxDeep) {
                break;
            }
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                count++;
                Point top = queue.poll();
                assert top != null;
                if ((berthId == -1 && top.equal(end)) || (berthId != -1 && gameMap.getDiscreteBelongToBerthId(top.x, top.y) == berthId)) {
                    //回溯路径
                    ArrayList<Point> result = getRobotPathByCs(cs, top);
                    Collections.reverse(result);
                    return result;
                }
                for (int j = 0; j < DIR.length / 2; j++) {
                    //四方向的
                    Point point = getNextPoint(gameMap, otherPaths, cs, top, j, deep, null);
                    if (point == null) continue;
                    queue.offer(point);
                }
            }
            deep += 2;
        }
        return new ArrayList<>();
    }


    private static Point getNextPoint(GameMap gameMap, ArrayList<ArrayList<Point>> otherPaths, int[][] discreteCs
            , Point top, int index
            , int deep, int[][] heuristicCs) {
        int lastDirIdx = discreteCs[top.x][top.y] & 3;
        int dirIdx = index ^ lastDirIdx; // 优先遍历上一次过来的方向
        Point dir = DIR[dirIdx];
        int dx = top.x + dir.x;
        int dy = top.y + dir.y;//第一步
        if (discreteCs[dx][dy] != Integer.MAX_VALUE || !gameMap.robotCanReachDiscrete(dx, dy)) {
            return null;
        }
        if (discreteCs[dx + dir.x][dy + dir.y] != Integer.MAX_VALUE
                || !gameMap.robotCanReachDiscrete(dx + dir.x, dy + dir.y)
        ) {
            return null;
        }
        if (robotCheckCrashInDeep(gameMap, deep + 1, dx, dy, otherPaths)
                || robotCheckCrashInDeep(gameMap, deep + 2, dx + dir.x, dy + dir.y, otherPaths)) {
            return null;
        }
        if (heuristicCs != null) {
            Point cur = gameMap.discreteToPos(top.x, top.y);
            Point next = gameMap.discreteToPos(dx + dir.x, dy + dir.y);
            if ((heuristicCs[cur.x][cur.y] >> 2) != (heuristicCs[next.x][next.y] >> 2) + 1) {
                //启发式剪枝，不是距离更近则直接结束
                return null;
            }
        }
        discreteCs[dx][dy] = ((deep + 1) << 2) + dirIdx;//第一步
        dx += dir.x;
        dy += dir.y;
        discreteCs[dx][dy] = ((deep + 2) << 2) + dirIdx;//第一步
        return new Point(dx, dy);
    }


    private static boolean robotCheckCrashInDeep(GameMap gameMap, int deep, int x, int y, ArrayList<ArrayList<Point>> otherPaths) {
        if (otherPaths != null) {
            for (int i = 0; i < otherPaths.size(); i++) {
                ArrayList<Point> otherPath = otherPaths.get(i);
                if (deep < otherPath.size() && otherPath.get(i).equal(x, y)
                        && !gameMap.isDiscreteRobotMainChannel(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }


}
