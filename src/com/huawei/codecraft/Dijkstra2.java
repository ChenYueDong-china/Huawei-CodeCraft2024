package com.huawei.codecraft;

import java.util.*;

import static com.huawei.codecraft.Constants.MAP_FILE_COL_NUMS;
import static com.huawei.codecraft.Constants.MAP_FILE_ROW_NUMS;
import static com.huawei.codecraft.Utils.*;


public class Dijkstra2 {

    private Point mTarget;//目标点
    private GameMap mGameMap;

    short[][] cs = new short[MAP_FILE_ROW_NUMS / 2 + 1][MAP_FILE_COL_NUMS / 2 + 1]; //前面2位是距离，后面的位数是距离0xdistdir

    void init(Point target, GameMap gameMap) {
        this.mTarget = target;
        this.mGameMap = gameMap;
    }

    void update(int maxDeep, int maxCount) {
        // 求最短路径
        int leftTopX = mTarget.x - MAP_FILE_ROW_NUMS / 4;
        int leftTopY = mTarget.y - MAP_FILE_COL_NUMS / 4;
        int fakeX = mTarget.x - leftTopX;
        int fakeY = mTarget.y - leftTopY;
        Point s = new Point(fakeX, fakeY);
        for (short[] c : cs) {
            Arrays.fill(c, Short.MAX_VALUE);
        }
        //单向dfs搜就行
        int deep = 0;
        Queue<Point> queue = new ArrayDeque<>();
        queue.offer(s);
        cs[s.x][s.y] = 0;
        int count = 0;
        while (!queue.isEmpty()) {
            if (deep > maxDeep || count > maxCount) {
                break;
            }
            int size = queue.size();
            deep += 1;
            //这一层出来的深度都一样
            for (int i = 0; i < size; i++) {
                Point top = queue.poll();
                count++;
                for (int j = 0; j < DIR.length / 2; j++) {
                    //四方向的
                    int lastDirIdx = cs[top.x][top.y] & 3;
                    int dirIdx = j ^ lastDirIdx; // 优先遍历上一次过来的方向
                    Point dir = DIR[dirIdx];
                    int dx = top.x + dir.x;
                    int dy = top.y + dir.y;//第一步
                    if ((!(dx >= 0 && dx <= MAP_FILE_ROW_NUMS / 2 && dy >= 0 && dy <= MAP_FILE_COL_NUMS / 2))
                            || !mGameMap.robotCanReach(dx + leftTopX, dy + leftTopY)
                            || cs[dx][dy] != Short.MAX_VALUE) {
                        continue; // 不可达或者访问过了
                    }
                    cs[dx][dy] = (short) ((deep << 2) + dirIdx);//第一步家的优先级大于移位
                    queue.offer(new Point(dx, dy));
                }
            }
        }
    }

    void update(int maxDeep, int maxCount, int[][] fastQueue) {
        // 求最短路径
        int leftTopX = mTarget.x - MAP_FILE_ROW_NUMS / 4;
        int leftTopY = mTarget.y - MAP_FILE_COL_NUMS / 4;
        int fakeX = mTarget.x - leftTopX;
        int fakeY = mTarget.y - leftTopY;
        Point s = new Point(fakeX, fakeY);
        for (short[] c : cs) {
            Arrays.fill(c, Short.MAX_VALUE);
        }
        //单向dfs搜就行
        int deep = 0;
        int head = 0;
        int tail = 0;
        fastQueue[tail][0] = s.x;
        fastQueue[tail][1] = s.y;
        tail++;
        cs[s.x][s.y] = 0;
        int count = 0;
        while ((tail - head) != 0) {
            if (deep > maxDeep || count > maxCount) {
                break;
            }
            int size = tail - head;
            deep += 1;
            //这一层出来的深度都一样
            for (int i = 0; i < size; i++) {
                int x = fastQueue[head][0];
                int y = fastQueue[head][1];
                head++;
                count++;
                for (int j = 0; j < DIR.length / 2; j++) {
                    //四方向的
                    int lastDirIdx = cs[x][y] & 3;
                    int dirIdx = j ^ lastDirIdx; // 优先遍历上一次过来的方向
                    Point dir = DIR[dirIdx];
                    int dx = x + dir.x;
                    int dy = y + dir.y;//第一步
                    if (!mGameMap.robotCanReach(dx + leftTopX, dy + leftTopY)
                            || (!(dx >= 0 && dx <= MAP_FILE_ROW_NUMS / 2 && dy >= 0 && dy <= MAP_FILE_COL_NUMS / 2))
                            || cs[dx][dy] != Short.MAX_VALUE) {
                        continue; // 不可达或者访问过了
                    }
                    cs[dx][dy] = (short) ((deep << 2) + dirIdx);//第一步家的优先级大于移位
                    fastQueue[tail][0] = dx;
                    fastQueue[tail][1] = dy;
                    tail++;
                }
            }
        }
    }

    public ArrayList<Point> moveFrom(Point source) {
        assert cs[source.x][source.y] != Short.MAX_VALUE;
        if (cs[source.x][source.y] == Short.MAX_VALUE) {
            return new ArrayList<>();
        }
        ArrayList<Point> result = getRobotPathByCs(cs, source);
        if (result.size() == 1) {
            //此时大概率有问题
            printError("error start equal end");
            result.add(source);//多加一个
        }
        //细化，转成精细坐标
        return mGameMap.toDiscretePath(result);
    }

    @SuppressWarnings("all")
    public ArrayList<Point> moveTo(Point target) {
        assert cs[target.x][target.y] != Integer.MAX_VALUE;
        if (cs[target.x][target.y] == Integer.MAX_VALUE) {
            return new ArrayList<>();
        }
        ArrayList<Point> result = getRobotPathByCs(cs, target);
        Collections.reverse(result);
        //细化，转成精细坐标
        return mGameMap.toDiscretePath(result);
    }


    //非细化目标到这里的移动时间
    public int getMoveDistance(Point target) {
        return getMoveDistance(target.x, target.y);
    }

    public short getMoveDistance(int x, int y) {
        if (cs[x][y] == Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        return (short) (cs[x][y] >> 2);
    }

}
