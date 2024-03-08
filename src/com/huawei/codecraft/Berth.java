package com.huawei.codecraft;

import java.util.ArrayList;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.Point;

public class Berth {
    Point leftTopPos = new Point();
    int transportTime;
    int loadingSpeed;

    public Dijkstra[][] dijkstras = new Dijkstra[BERTH_HEIGHT][BERTH_WIDTH];//berth有16个点,需要16次dij

    public int[][] minDistance = new int[MAP_FILE_COL_NUMS][MAP_FILE_COL_NUMS];
    public ArrayList<Point>[][] minDistancePos = new ArrayList[MAP_FILE_COL_NUMS][MAP_FILE_COL_NUMS];

    public void init(GameMap gameMap) {
        for (int i = 0; i < BERTH_HEIGHT; i++) {
            for (int j = 0; j < BERTH_WIDTH; j++) {
                dijkstras[i][j] = new Dijkstra();
                Point point = new Point(leftTopPos.x + i, leftTopPos.y + j);
                dijkstras[i][j].init(point, gameMap);
                dijkstras[i][j].update();
            }
        }
        //保存一下最小的到每个其它点的距离,便于选择驳口
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                Point point = new Point(i, j);
                minDistancePos[i][j] = new ArrayList<>();
                minDistance[i][j] = Integer.MAX_VALUE - 1;
                for (int k = 0; k < BERTH_HEIGHT; k++) {
                    for (int l = 0; l < BERTH_WIDTH; l++) {
                        int moveDistance = dijkstras[k][l].getMoveDistance(point);
                        if (moveDistance <= minDistance[i][j]) {
                            if (moveDistance < minDistance[i][j]) {
                                minDistancePos[i][j].clear();
                            }
                            minDistance[i][j] = moveDistance;
                            minDistancePos[i][j].add(new Point(k, l));//相对位置，便于找dij
                        }
                    }
                }
                if (minDistance[i][j] == Integer.MAX_VALUE - 1) {
                    minDistance[i][j] = Integer.MAX_VALUE;
                }
            }
        }
        System.out.println("111");

    }


}
