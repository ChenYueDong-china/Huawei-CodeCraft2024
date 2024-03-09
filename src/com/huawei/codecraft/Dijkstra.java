package com.huawei.codecraft;

import java.util.*;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;


public class Dijkstra {

    private Point mTarget;//目标点
    private GameMap mGameMap;

    int[][] cs = new int[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS]; //前面2位是距离，后面的位数是距离0xdistdir

    void init(Point target, GameMap gameMap) {
        this.mTarget = target;
        this.mGameMap = gameMap;
    }

    void update() {
        // 求最短路径
        Point s = mTarget;
        for (int[] c : cs) {
            Arrays.fill(c, Integer.MAX_VALUE);
        }
        //单向dfs搜就行
        int deep = 0;
        Queue<Point> queue = new LinkedList<>();
        queue.offer(s);
        cs[s.x][s.y] = 0;
        while (!queue.isEmpty()) {
            int size = queue.size();
            deep += 1;
            //这一层出来的深度都一样
            for (int i = 0; i < size; i++) {
                Point top = queue.poll();
                assert top != null;
                for (int j = 0; j < DIR.length / 2; j++) {
                    //四方向的
                    int lastDirIdx = cs[top.x][top.y] & 3;
                    int dirIdx = j ^ lastDirIdx; // 优先遍历上一次过来的方向
                    Point dir = DIR[dirIdx];
                    int dx = top.x + dir.x;
                    int dy = top.y + dir.y;//第一步
                    if (!mGameMap.canReach(dx, dy) || cs[dx][dy] != Integer.MAX_VALUE) {
                        continue; // 不可达或者访问过了
                    }
                    cs[dx][dy] = (deep << 2) + dirIdx;//第一步家的优先级大于移位
                    queue.offer(new Point(dx, dy));
                }
            }
        }
    }

    public ArrayList<Point> moveFrom(Point source) {
        assert cs[source.x][source.y] != Integer.MAX_VALUE;
        if (cs[source.x][source.y] == Integer.MAX_VALUE) {
            return new ArrayList<>();
        }
        ArrayList<Point> result = getBasicPath(source);
        //细化，转成精细坐标
        return mGameMap.toDiscretePath(result);
    }

    public ArrayList<Point> moveTo(Point target) {
        assert cs[target.x][target.y] != Integer.MAX_VALUE;
        if (cs[target.x][target.y] == Integer.MAX_VALUE) {
            return new ArrayList<>();
        }
        ArrayList<Point> result = getBasicPath(target);
        Collections.reverse(result);
        //细化，转成精细坐标
        return mGameMap.toDiscretePath(result);
    }

    private ArrayList<Point> getBasicPath(Point target) {
        ArrayList<Point> result = new ArrayList<>();
        Point point = new Point(target);
        result.add(new Point(point));
        while (cs[point.x][point.y] != 0) {
            point.addEquals(DIR[(cs[point.x][point.y] & 3) ^ 1]);
            result.add(new Point(point));
        }
        return result;
    }


    //非细化目标到这里的移动时间
    public int getMoveDistance(Point target) {
        return getMoveDistance(target.x, target.y);
    }

    public int getMoveDistance(int x, int y) {
        if (cs[x][y] == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (cs[x][y] >> 2);
    }

}
