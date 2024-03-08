package com.huawei.codecraft;

import java.util.*;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;


public class Dijkstra {

    private Point mTarget;//目标点
    private GameMap mGameMap;

    CellStat[][] cs = new CellStat[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_HEIGHT];

    {
        for (int i = 0; i < cs.length; i++) {
            for (int j = 0; j < cs[0].length; j++) {
                cs[i][j] = new CellStat();
            }
        }
    }

    void init(Point target, GameMap gameMap) {
        this.mTarget = target;
        this.mGameMap = gameMap;
    }

    static class CellStat {
        int dist; // 距离，走一步一个距离

        int dir;

        public CellStat() {
        }


        public void set(int dist, int dir) {
            this.dist = dist;
            this.dir = dir;
        }
    }


    void update() {
        // 求最短路径
        Point s = mGameMap.posToDiscrete(mTarget);
        for (CellStat[] c : cs) {
            for (CellStat cellStat : c) {
                cellStat.dist = Integer.MAX_VALUE;//最大值就是没访问过
            }
        }
        //单向dfs搜就行
        int deep = 0;
        Queue<Point> queue = new LinkedList<>();
        queue.offer(s);
        cs[s.x][s.y].set(0, 0);
        while (!queue.isEmpty()) {
            int size = queue.size();
            //这一层出来的深度都一样
            for (int i = 0; i < size; i++) {
                Point top = queue.poll();
                assert top != null;
                for (int j = 0; j < DIR.length / 2; j++) {
                    //四方向的
                    int lastDirIdx = cs[top.x][top.y].dir;
                    int dirIdx = j ^ lastDirIdx; // 优先遍历上一次过来的方向
                    Point dir = DIR[dirIdx];
                    int dx = top.x + dir.x;
                    int dy = top.y + dir.y;//第一步
                    if (!mGameMap.canReachDiscrete(dx, dy) || cs[dx][dy].dist != Integer.MAX_VALUE) {
                        continue; // 不可达或者访问过了
                    }
                    cs[dx][dy].dist = deep + 1;
                    cs[dx][dy].dir = dirIdx;
                    dx += dir.x;
                    dy += dir.y;
                    assert (mGameMap.canReachDiscrete(dx, dy));//必定可达
                    cs[dx][dy].dist = deep + 2;//第二步
                    cs[dx][dy].dir = dirIdx;
                    queue.offer(new Point(dx, dy));
                }
            }
            deep += 2;
        }
    }

    public ArrayList<Point> moveFrom(Point source) {
        Point t = mGameMap.posToDiscrete(source);
        ArrayList<Point> result = new ArrayList<>();
        if (t.x < 0 || t.y < 0 || cs[t.x][t.y].dist == Integer.MAX_VALUE) {
            return result; // 不可达
        }
        result.add(t);
        while (cs[t.x][t.y].dist != 0) {
            t.addEquals(DIR[cs[t.x][t.y].dir ^ 1]);
            result.add(new Point(t.x, t.y));
        }
        return result;
    }

    public ArrayList<Point> moveTo(Point target) {
        assert (cs[target.x][target.y].dist != Integer.MAX_VALUE);
        Point t = mGameMap.posToDiscrete(target);
        ArrayList<Point> result = new ArrayList<>();
        result.add(t);
        while (cs[t.x][t.y].dist != 0) {
            t.addEquals(DIR[cs[t.x][t.y].dir ^ 1]);
            result.add(new Point(t.x, t.y));
        }
        Collections.reverse(result);
        return result;
    }


    //非细化目标到这里的移动时间
    public int getMoveDistance(Point target) {
        Point t = mGameMap.posToDiscrete(target);
        if (cs[t.x][t.y].dist == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        assert (cs[t.x][t.y].dist % 2 == 0);
        return cs[t.x][t.y].dist / 2;
    }

    public int getMoveDistance(int x, int y) {
        return getMoveDistance(new Point(x, y));
    }

}
