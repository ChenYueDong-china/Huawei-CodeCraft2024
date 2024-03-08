package com.huawei.codecraft;


import java.util.Arrays;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.Point;

public class Map {


    private final char[][] mapData = new char[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
    private final boolean[][] discreteMapData = new boolean[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH];//0不可达 1可达

    public static final Point[] DIR = {
            new Point(1, 0),//右移
            new Point(-1, 0),//左移
            new Point(0, 1),//上移
            new Point(0, -1),//下移
            new Point(-1, -1),
            new Point(1, 1),
            new Point(-1, 1),
            new Point(1, -1),

    };


    static class Pair<T, R> {
        // 一般是把这两个变量设置为私有的 然后再定义set, get方法
        // 但是做算法题就没有这些讲究了
        public T first;
        public R second;

        Pair(T first, R second) {
            this.first = first;
            this.second = second;
        }

    }


    public boolean canReachDiscrete(int row, int col) {
        return (row > 0 && row < MAP_DISCRETE_HEIGHT && col > 0 && col < MAP_DISCRETE_WIDTH &&
                (discreteMapData[row][col]));
    }


    @SuppressWarnings("all")
    boolean SetMap(char[][] mapData) {
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            System.arraycopy(mapData[i], 0, mapData[i], 0, MAP_FILE_COL_NUMS);
        }
        InitDiscrete();
        return true;
    }

    public Point posToDiscrete(int x, int y) {
        return new Point(2 * x + 1, 2 * y + 1);
    }

    public Point discreteToPos(int x, int y) {
        assert x % 2 == 1 && y % 2 == 1;//偶数点是额外添加的点，不是真实点
        return new Point(x / 2, y / 2);
    }


    private void InitDiscrete() {
        for (boolean[] data : discreteMapData) {
            Arrays.fill(data, false);
        }
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                if (mapData[i][j] == '*' || mapData[i][j] == '#') {
                    //海洋或者障碍;
                    Point point = posToDiscrete(i, j);
                    discreteMapData[point.x][point.y] = false;
                    //周围八个点和这个点都是不可达点
                    for (int k = 0; k < DIR.length; k++) {
                        Point tmp = point.add(DIR[i]);
                        if (canReachDiscrete(tmp.x, tmp.y)) {
                            discreteMapData[point.x][point.y] = false;//不可达
                        }
                    }
                }

            }
        }
        //todo update一次走两步
    }

    // 判断地图上某个坐标能否容纳半径为radius的圆
    boolean CanReach(Vec2 pos, double radius) {
        // 对圆心 + 圆上8个点进行检测
        Vec2[] checkPoints = new Vec2[9];
        for (int i = 0; i < CIRCLE.length; i++) {
            checkPoints[i] = new Vec2(CIRCLE[i]);
        }

        for (int i = 0; i < 8; i++) {
            checkPoints[i].mulEquals(radius);
            checkPoints[i].addEquals(pos);
        }
        checkPoints[8] = pos;

        // 9个点必须全部合法
        for (Vec2 it : checkPoints) {
            if (it.x < 0 || it.y < 0 || it.x > MAP_WIDTH || it.y > MAP_HEIGHT)
                return false;
            RowCol mapIt = PosToMap(it);
            if (mapIt.r < 0 || mapIt.c < 0 || mapIt.r >= MAP_FILE_ROW_NUMS || mapIt.c >= MAP_FILE_COL_NUMS ||
                    m_mapData[mapIt.r][mapIt.c] == '#') {
                return false;
            }
        }
        return true;
    }

    // 判断地图上某个坐标能否容纳半径为radius的圆
    boolean CanReachExactly(Vec2 pos, double radius) {

        RowCol mapIt = PosToMap(pos);
        //todo与四个墙的间距小于radius，则直接返回false

        double dis1 = PtoSegDistance(pos, new Line(new Vec2(0, 0), new Vec2(0, MAP_HEIGHT)));
        double dis2 = PtoSegDistance(pos, new Line(new Vec2(0, MAP_HEIGHT), new Vec2(MAP_WIDTH, MAP_HEIGHT)));
        double dis3 = PtoSegDistance(pos, new Line(new Vec2(MAP_WIDTH, 0), new Vec2(MAP_WIDTH, MAP_HEIGHT)));
        double dis4 = PtoSegDistance(pos, new Line(new Vec2(0, 0), new Vec2(MAP_WIDTH, 0)));
        if (dis1 < radius || dis2 < radius || dis3 < radius || dis4 < radius) {
            return false;
        }
        int WIDTH = (int) Math.ceil(radius / CELL_SIZE);
        for (int i = -WIDTH; i <= WIDTH; i++)
            for (int j = -WIDTH; j <= WIDTH; j++) {
                int row = mapIt.r + i;
                int col = mapIt.c + j;
                if (row > 0 && row < MAP_DISCRETE_HEIGHT && col > 0 && col < MAP_DISCRETE_WIDTH &&
                        m_mapData[row][col] == '#') {
                    //周围四条线段距离小于r,则false
                    Vec2 tmp = MapToPos(row, col);
                    Line line1 = new Line(tmp.add(new Vec2(DISCRETE_SIZE, -DISCRETE_SIZE)), tmp.add(new Vec2(DISCRETE_SIZE, DISCRETE_SIZE)));
                    Line line2 = new Line(tmp.add(new Vec2(-DISCRETE_SIZE, -DISCRETE_SIZE)), tmp.add(new Vec2(-DISCRETE_SIZE, DISCRETE_SIZE)));
                    Line line3 = new Line(tmp.add(new Vec2(-DISCRETE_SIZE, DISCRETE_SIZE)), tmp.add(new Vec2(DISCRETE_SIZE, DISCRETE_SIZE)));
                    Line line4 = new Line(tmp.add(new Vec2(-DISCRETE_SIZE, -DISCRETE_SIZE)), tmp.add(new Vec2(DISCRETE_SIZE, -DISCRETE_SIZE)));
                    dis1 = PtoSegDistance(pos, line1);
                    dis2 = PtoSegDistance(pos, line2);
                    dis3 = PtoSegDistance(pos, line3);
                    dis4 = PtoSegDistance(pos, line4);
                    if (dis1 < radius || dis2 < radius || dis3 < radius || dis4 < radius) {
                        return false;
                    }
                }
            }
        return true;
    }


}
