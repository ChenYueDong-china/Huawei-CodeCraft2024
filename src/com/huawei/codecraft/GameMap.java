package com.huawei.codecraft;


import java.util.ArrayList;
import java.util.Arrays;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.Point;
import static com.huawei.codecraft.Utils.DIR;

public class GameMap {


    private final char[][] mapData = new char[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];//列到行
    private final boolean[][] discreteMapData = new boolean[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH];//0不可达 1可达


    public boolean canReachDiscrete(int x, int y) {
        return (x > 0 && x < MAP_DISCRETE_HEIGHT - 1 && y > 0 && y < MAP_DISCRETE_WIDTH - 1 &&
                (discreteMapData[x][y]));//0和最后一行或者列是墙，不判断
    }


    @SuppressWarnings("all")
    boolean setMap(char[][] mapData) {
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                this.mapData[i][j] = mapData[i][j];//颠倒一下
            }
        }
//        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
//            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
//                this.mapData[j][i] = mapData[i][j];//颠倒一下
//            }
//        }
        initDiscrete();
        return true;
    }

    public Point posToDiscrete(Point point) {
        return posToDiscrete(point.x, point.y);
    }

    public Point posToDiscrete(int x, int y) {
        return new Point(2 * x + 1, 2 * y + 1);
    }

    public Point discreteToPos(Point point) {
        return discreteToPos(point.x, point.y);
    }

    public Point discreteToPos(int x, int y) {
        assert x % 2 == 1 && y % 2 == 1;//偶数点是额外添加的点，不是真实点
        return new Point(x / 2, y / 2);
    }


    private void initDiscrete() {
        for (boolean[] data : discreteMapData) {
            Arrays.fill(data, true);
        }
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                if (mapData[i][j] == '*' || mapData[i][j] == '#') {
                    //海洋或者障碍;
                    Point point = posToDiscrete(i, j);
                    discreteMapData[point.x][point.y] = false;
                    //周围八个点和这个点都是不可达点
                    for (Point dir : DIR) {
                        Point tmp = point.add(dir);
                        if (canReachDiscrete(tmp.x, tmp.y)) {
                            discreteMapData[tmp.x][tmp.y] = false;//不可达
                        }
                    }
                }
            }
        }
        //四条墙壁,不能走
        Arrays.fill(discreteMapData[0], false);
        Arrays.fill(discreteMapData[MAP_DISCRETE_WIDTH - 1], false);
        for (int i = 0; i < MAP_DISCRETE_HEIGHT; i++) {
            discreteMapData[i][0] = false;
        }
        for (int i = 0; i < MAP_DISCRETE_HEIGHT; i++) {
            discreteMapData[i][MAP_DISCRETE_WIDTH - 1] = false;
        }
    }

    public boolean canReach(int x, int y) {
        return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
                (mapData[x][y] != '*' && mapData[x][y] != '#'));//不是海洋或者障碍
    }

    //普通路径转细化路径
    public ArrayList<Point> toDiscretePath(ArrayList<Point> tmp) {
        ArrayList<Point> result = new ArrayList<>();
        for (int i = 0; i < tmp.size() - 1; i++) {
            Point cur = posToDiscrete(tmp.get(i));
            Point next = posToDiscrete(tmp.get(i + 1));
            Point mid = cur.add(next).div(2);
            result.add(cur);
            result.add(mid);
        }
        result.add(posToDiscrete(tmp.get(tmp.size() - 1)));
        return result;
    }

    @SuppressWarnings("all")
    //细化路径转会普通路径
    public ArrayList<Point> toRealPath(ArrayList<Point> tmp) {
        assert tmp.size() % 2 == 1;//2倍的路径加个起始点
        ArrayList<Point> result = new ArrayList<>();
        for (int i = 0; i < tmp.size(); i += 2) {
            result.add(discreteToPos(tmp.get(i).x, tmp.get(i).y));
        }
        return result;
    }

}
