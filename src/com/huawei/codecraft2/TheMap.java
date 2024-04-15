package com.huawei.codecraft2;

import com.huawei.codecraft2.Berth;

import java.util.ArrayList;
import java.util.List;

/**
 * @ Description
 * @ Date 2024/4/9 20:54
 */
public class TheMap {
    public static final int MAP_SIZE = 800;

    public static char[][] GRID = new char[MAP_SIZE][MAP_SIZE];

    public static List<int[]> robotPurchasePoints = new ArrayList<int[]>();

    public static List<int[]> boatPurchasePoints = new ArrayList<int[]>();

    public static List<int[]> deliveryPoints = new ArrayList<int[]>();

    public static int BERTH_NUM;

    public static List<Berth> berths = new ArrayList<Berth>();

    public static void processMap() {
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                if (GRID[i][j] == 'R') {
                    robotPurchasePoints.add(new int[]{i, j});
                } else if (GRID[i][j] == 'S') {
                    boatPurchasePoints.add(new int[]{i, j});
                } else if (GRID[i][j] == 'T') {
                    deliveryPoints.add(new int[]{i, j});
                }
            }
        }
    }
}
