package com.huawei.codecraft;

import java.io.IOException;
import java.util.ArrayList;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.*;

public class Workbench2 {

    int id;
    Point pos = new Point();
    int value;
    int minSellDistance;
    short[][] cs = new short[MAP_FILE_ROW_NUMS / 2 + 1][MAP_FILE_COL_NUMS / 2 + 1];

    int remainTime;
    GameMap map;

    public Workbench2(int id) {
        this.id = id;
        remainTime = WORKBENCH_EXIST_TIME;
    }

    public void input(GameMap map) throws IOException {
        this.map = map;
        String line = inStream.readLine();
        printMost(line);
        String[] parts = line.trim().split(" ");
        pos.x = Integer.parseInt(parts[0]);
        pos.y = Integer.parseInt(parts[1]);
        value = Integer.parseInt(parts[2]);
        if (value == 0) {
            return;
        }
        //初始化dij
        Dijkstra dijkstra = new Dijkstra();
        dijkstra.init(pos, map);
        dijkstra.update(WORKBENCH_MAX_SEARCH_DEEP, WORKBENCH_MAX_SEARCH_COUNT);
        //已自己为中心点更新
        int leftTopX = pos.x - MAP_FILE_ROW_NUMS / 4;
        int leftTopY = pos.x - MAP_FILE_COL_NUMS / 4;
        for (int i = 0; i < (MAP_FILE_ROW_NUMS / 2); i++) {
            for (int j = 0; j < (MAP_FILE_COL_NUMS / 2); j++) {
                if (map.isLegalPoint(i + leftTopX, j + leftTopY)) {
                    cs[i][j] = dijkstra.cs[i + leftTopX][j + leftTopY];
                } else {
                    cs[i][j] = Short.MAX_VALUE;
                }
            }
        }
    }

    //获得这个workbench到任意一个位置的最小距离
    int getMinDistance(Point point) {
        int leftTopX = pos.x - MAP_FILE_ROW_NUMS / 4;
        int leftTopY = pos.y - MAP_FILE_COL_NUMS / 4;
        int fakeX = point.x - leftTopX;
        int fakeY = point.y - leftTopY;
        if (fakeX >= 0 && fakeX <= (MAP_FILE_ROW_NUMS / 2) &&
                fakeY >= 0 && fakeY <= (MAP_FILE_COL_NUMS / 2)) {
            return cs[fakeX][fakeY] == Short.MAX_VALUE ? Integer.MAX_VALUE : cs[fakeX][fakeY] >> 2;
        }
        return Integer.MAX_VALUE;
    }

    //获得这个workbench到任意一个位置的最小距离
    public ArrayList<Point> moveFrom(Point point) {
        int leftTopX = pos.x - MAP_FILE_ROW_NUMS / 4;
        int leftTopY = pos.y - MAP_FILE_COL_NUMS / 4;
        int fakeX = point.x - leftTopX;
        int fakeY = point.y - leftTopY;
        Point source = new Point(fakeX, fakeY);
        if (cs[fakeX][fakeY] == Short.MAX_VALUE) {
            return new ArrayList<>();
        }
        ArrayList<Point> result = getRobotPathByCs(cs, source);
        if (result.size() == 1) {
            //此时大概率有问题
            printError("error start equal end");
            result.add(source);//多加一个
        }
        for (Point re : result) {
            re.x += leftTopX;
            re.y += leftTopY;
        }
        //细化，转成精细坐标
        return map.toDiscretePath(result);
    }

    //获得这个workbench到任意一个位置的最小距离
    public void setHeuristicCs(short[][] heuristicCs) {
        int leftTopX = pos.x - MAP_FILE_ROW_NUMS / 4;
        int leftTopY = pos.x - MAP_FILE_COL_NUMS / 4;
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                int fakeX = i - leftTopX;
                int fakeY = j - leftTopY;
                if (fakeX >= 0 && fakeX <= (MAP_FILE_ROW_NUMS / 2) &&
                        fakeY >= 0 && fakeY <= (MAP_FILE_COL_NUMS / 2)) {
                    heuristicCs[i][j] = cs[fakeX][fakeY];
                } else {
                    heuristicCs[i][j] = Short.MAX_VALUE;
                }
            }
        }

    }

    public boolean canReach(Point pos) {
        return getMinDistance(pos) != Integer.MAX_VALUE;
    }
}
