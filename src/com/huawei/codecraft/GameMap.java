package com.huawei.codecraft;


import java.util.Arrays;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.Point;
import static com.huawei.codecraft.Utils.DIR;

public class GameMap {


    private final char[][] mapData = new char[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
    private final boolean[][] discreteMapData = new boolean[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH];//0不可达 1可达


    public boolean canReachDiscrete(int x, int y) {
        return (x > 0 && x < MAP_DISCRETE_HEIGHT - 1 && y > 0 && y < MAP_DISCRETE_WIDTH - 1 &&
                (discreteMapData[x][y]));//0和最后一行或者列是墙，不判断
    }


    @SuppressWarnings("all")
    boolean setMap(char[][] mapData) {
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            System.arraycopy(mapData[i], 0, this.mapData[i], 0, MAP_FILE_COL_NUMS);
        }
        initDiscrete();
        return true;
    }

    public Point posToDiscrete(Point point) {
        return posToDiscrete(point.x, point.y);
    }

    public Point posToDiscrete(int x, int y) {
        return new Point(2 * x + 1, 2 * y + 1);
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

    // 判断地图上某个坐标能否容纳半径为radius的圆
    boolean canReach(int x, int y) {
        // 对圆心 + 圆上8个点进行检测
        return canReachDiscrete(posToDiscrete(x, y).x, posToDiscrete(x, y).y);
    }

}
