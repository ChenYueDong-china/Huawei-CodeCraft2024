package com.huawei.codecraft;

import java.util.*;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;
import static java.lang.Math.abs;

public class Berth {

    int id;
    public int curBoatId = -1;
    Point corePoint = new Point();
    int coreDirection = 0;
    int loadingSpeed;
    int minSellDistance;
    int goodsNums;//泊位目前没装的货物数量

    Queue<Integer> goods = new ArrayDeque<>();

    int totalValue = 0;//货物总价值
    int totalGoodsNums;//来的总货，判断泊位好坏

    public short[][] robotMinDistance = new short[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];
    public short[][][] boatMinDistance = new short[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][DIR.length / 2];//走启发

    public ArrayList<Point> berthPoints = new ArrayList<>();
    public ArrayList<Point> berthAroundPoints = new ArrayList<>();//也包含berths
    private GameMap gameMap;


    public void init(GameMap gameMap) {
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
                if (!gameMap.isLegalPoint(dx, dy) || visits[dx][dy] || !gameMap.isBerthAround(dx, dy)) {
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
        for (short[] distance : robotMinDistance) {
            Arrays.fill(distance, Short.MAX_VALUE);
        }
        Dijkstra dijkstra = new Dijkstra();
        for (Point berthPoint : berthPoints) {
            dijkstra.init(berthPoint, gameMap);
            dijkstra.update();
            for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
                for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                    short distance = dijkstra.getMoveDistance(i, j);
                    if (distance < robotMinDistance[i][j]) {
                        robotMinDistance[i][j] = distance;
                    }
                }
            }
        }

        BoatDijkstra commonDij = new BoatDijkstra();
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                Arrays.fill(boatMinDistance[i][j], Short.MAX_VALUE);
            }
        }
        //船路径
//        long l1 = System.currentTimeMillis();
        ArrayList<Point> searchPoint = new ArrayList<>();
        searchPoint.add(berthAroundPoints.get(0));//起始点和离船最远2个点，因为竞争很可能需要闪现
        for (int i = berthAroundPoints.size() - 4; i < berthAroundPoints.size(); i++) {
            searchPoint.add(berthAroundPoints.get(i));
        }
        for (Point aroundPoint : searchPoint) {
            commonDij.init(aroundPoint, gameMap);
            commonDij.update(BERTH_MAX_BOAT_SEARCH_DEEP);
            short flashDistance = (short) (1 + 2 * (abs(corePoint.x - aroundPoint.x) + abs(corePoint.y - aroundPoint.y)));
            for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
                for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                    Point point = new Point(i, j);
                    for (int k = 0; k < DIR.length / 2; k++) {
                        short distance = commonDij.getMoveDistance(point, k);
                        distance += flashDistance;
                        if (distance < boatMinDistance[i][j][k]) {
                            boatMinDistance[i][j][k] = distance;
                        }
                    }
                }
            }
        }
//        long l2 = System.currentTimeMillis();
//        printError("berthInitTime:" + (l2 - l1));
        gameMap.updateBerthAndAround(berthAroundPoints, berthPoints, id);
    }

    @SuppressWarnings("all")
    public boolean robotCanReach(Point pos) {
        return getRobotMinDistance(pos) != Short.MAX_VALUE;
    }

    public int getRobotMinDistance(Point pos) {
        return robotMinDistance[pos.x][pos.y];
    }

    public boolean boatCanReach(Point pos, int dir) {
        return getBoatMinDistance(pos, dir) != Short.MAX_VALUE;
    }

    public int getBoatMinDistance(Point pos, int dir) {
        return boatMinDistance[pos.x][pos.y][dir];
    }


    public boolean inBerth(Point point) {
        //在berth范围内
        return gameMap.getBelongToBerthId(point) == id;
    }


}
