package com.huawei.codecraft;

import java.util.*;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.abs;

public class Berth {

    int id;
    Point corePoint = new Point();
    int coreDirection = 0;
    int transportTime;
    int loadingSpeed;
    int goodsNums;//泊位目前没装的货物数量

    Queue<Integer> goods = new ArrayDeque<>();

    int totalValue = 0;//货物总价值
    int totalGoodsNums;//来的总货，判断泊位好坏


    public int[][] robotMinDistance = new int[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
    public int[][][] boatMinDistance = new int[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][DIR.length / 2];//船上的

    @SuppressWarnings("all")
    public ArrayList<Integer>[][] robotMinDistanceIndexes = new ArrayList[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];//保存一个就行

    public ArrayList<Dijkstra> robotDijkstra = new ArrayList<>();

    public ArrayList<Point> berthPoints = new ArrayList<>();
    public ArrayList<Point> berthAroundPoints = new ArrayList<>();//也包含berths
    private GameMap gameMap;


    public void init(GameMap gameMap, boolean littleTime) {
        this.gameMap = gameMap;
        Point start = new Point(corePoint);
        Queue<Point> queue = new ArrayDeque<>();
        queue.offer(start);
        berthAroundPoints.add(start);
        boolean[][] visits = new boolean[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
        for (boolean[] visit : visits) {
            Arrays.fill(visit, false);
        }
        visits[start.x][start.y] = true;
        while (!queue.isEmpty()) {
            Point top = queue.poll();
            for (int k = 0; k < DIR.length / 2; k++) {
                Point dir = DIR[k];
                int dx = top.x + dir.x;
                int dy = top.y + dir.y;//第一步
                if (visits[dx][dy] || !gameMap.isBerthAround(dx, dy)) {
                    continue; // 不可达或者访问过了
                }
                visits[dx][dy] = true;
                berthAroundPoints.add(new Point(dx, dy));
                queue.offer(new Point(dx, dy));
            }
        }
        //判题器漏洞，只能有6个点是泊位,保证泊位一定是2*3的
        for (int i = 0; i < DIR.length / 2; i++) {
            ArrayList<Point> points = gameMap.getBoatPoints(corePoint, i);
            boolean allBerth = true;
            for (Point point : points) {
                if (!gameMap.isBerth(point.x, point.y)) {
                    allBerth = false;
                    break;
                }
            }
            if (allBerth) {
                coreDirection = i;
                break;
            }
        }
        berthPoints = gameMap.getBoatPoints(corePoint, coreDirection);

        //机器人路径
        for (Point berthPoint : berthPoints) {
            Dijkstra dijkstra = new Dijkstra();
            dijkstra.init(berthPoint, gameMap);
            dijkstra.update();
            robotDijkstra.add(dijkstra);
        }
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                Point point = new Point(i, j);
                robotMinDistanceIndexes[i][j] = new ArrayList<>();
                robotMinDistance[i][j] = Integer.MAX_VALUE - 1;
                for (int k = 0; k < robotDijkstra.size(); k++) {
                    int distance = robotDijkstra.get(k).getMoveDistance(point);
                    if (distance <= robotMinDistance[i][j]) {
                        if (distance < robotMinDistance[i][j]) {
                            robotMinDistanceIndexes[i][j].clear();
                        }
                        robotMinDistance[i][j] = distance;
                        robotMinDistanceIndexes[i][j].add(k);
                    }
                }
                if (robotMinDistance[i][j] == Integer.MAX_VALUE - 1) {
                    robotMinDistance[i][j] = Integer.MAX_VALUE;
                }
            }
        }

        BoatDijkstra commonDij = new BoatDijkstra();
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                Arrays.fill(boatMinDistance[i][j], Integer.MAX_VALUE);
            }
        }
        //船路径
        for (Point aroundPoint : berthAroundPoints) {
            commonDij.init(aroundPoint, gameMap);
            commonDij.update();
            int flashDistance = (1 + 2 * (abs(corePoint.x - aroundPoint.x) + abs(corePoint.y - aroundPoint.y)));
            for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
                for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                    Point point = new Point(i, j);
                    for (int k = 0; k < DIR.length / 2; k++) {
                        int distance = commonDij.getMoveDistance(point, k);
                        distance += flashDistance;
                        if (distance < boatMinDistance[i][j][k]) {
                            boatMinDistance[i][j][k] = distance;
                        }
                    }
                }
            }
            if (littleTime) {//更新一次就行
                break;
            }
        }
        gameMap.updateBerthAndAround(berthAroundPoints,berthPoints, id);
    }

    @SuppressWarnings("all")
    public boolean robotCanReach(Point pos) {
        return robotMinDistance[pos.x][pos.y] != Integer.MAX_VALUE;
    }

    public int getRobotMinDistance(Point pos) {
        return robotMinDistance[pos.x][pos.y];
    }

    public boolean boatCanReach(Point pos, int dir) {
        return boatMinDistance[pos.x][pos.y][dir] != Integer.MAX_VALUE;
    }

    public int getBoatMinDistance(Point pos, int dir) {
        return boatMinDistance[pos.x][pos.y][dir];
    }


    public boolean inBerth(Point point) {
        //在berth范围内
        return gameMap.getBelongToBerthId(point) == id;
    }


}
