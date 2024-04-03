package com.huawei.codecraft;


import java.util.ArrayList;
import java.util.Arrays;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.Point;
import static com.huawei.codecraft.Utils.DIR;

public class GameMap {


    private final char[][] mapData = new char[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];//列到行
    private final boolean[][] robotDiscreteMapData = new boolean[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH];//0不可达 1可达
    private final boolean[][][] boatCanReach_
            = new boolean[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH][DIR.length / 2];//船是否能到达
    private final boolean[][][] boatIsAllInMainChannel_
            = new boolean[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH][DIR.length / 2];//整艘船都在主航道上
    private final boolean[][][] boatHasOneInMainChannel_
            = new boolean[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH][DIR.length / 2];//有至少一个点在主航道上


    public boolean isLegalPoint(int x, int y) {
        return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS);
    }

    public boolean boatCanReach(int x, int y) {
        return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
                (mapData[x][y] == '*'//海洋
                        || mapData[x][y] == '~'//主航道
                        || mapData[x][y] == 'K'//靠泊区
                        || mapData[x][y] == 'B'//泊位
                        || mapData[x][y] == 'C'//交通地块
                        || mapData[x][y] == 'c'//交通地块,加主航道
                        || mapData[x][y] == 'S'//购买地块
                        || mapData[x][y] == 'T'//交货点
                )
        );
    }

    public boolean isBerthAround(int x, int y) {
        return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
                (mapData[x][y] == 'K'//靠泊区
                        || mapData[x][y] == 'B'//泊位
                )
        );
    }

    public boolean isBerth(int x, int y) {
        return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
                mapData[x][y] == 'B'//泊位
        );
    }

    public boolean boatCanReach(Point corePoint, int dir) {
        return boatCanReach_[corePoint.x][corePoint.y][dir];
    }

    public int getRotationDir(int curDir, int nextDir) {
        int[] data = {0, 3, 1, 2, 0};
        boolean clockwise = false;
        for (int i = 1; i < data.length; i++) {
            if (data[i] == nextDir && data[i - 1] == curDir) {
                clockwise = true;
                break;
            }
        }
        return clockwise ? 0 : 1;
    }

    public boolean isMainChannel(int x, int y) {
        return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
                (mapData[x][y] == '~'//主航道
                        || mapData[x][y] == 'S'//购买地块
                        || mapData[x][y] == 'K'//靠泊区
                        || mapData[x][y] == 'B'//泊位
                        || mapData[x][y] == 'c'//交通地块
                        || mapData[x][y] == 'T'));//交货点
    }


    public boolean boatIsAllInMainChannel(Point corePoint, int direction) {
        return boatIsAllInMainChannel_[corePoint.x][corePoint.y][direction];
    }

    public boolean boatHasOneInMainChannel(Point corePoint, int direction) {
        //核心点和方向，求出是否整艘船都在主航道
        //核心点一定在方向左下角
        return boatHasOneInMainChannel_[corePoint.x][corePoint.y][direction];
    }

    public ArrayList<Point> getBoatPoints(Point corePoint, int direction) {
        ArrayList<Point> points = new ArrayList<>();
        for (int i = 0; i < BOAT_LENGTH; i++) {
            points.add(corePoint.add(DIR[direction].mul(i)));
        }
        Point nextPoint = new Point(corePoint);
        if (direction == 0) {
            nextPoint.addEquals(new Point(1, 0));
        } else if (direction == 1) {
            nextPoint.addEquals(new Point(-1, 0));
        } else if (direction == 2) {
            nextPoint.addEquals(new Point(0, 1));
        } else {
            nextPoint.addEquals(new Point(0, -1));
        }
        for (int i = 0; i < BOAT_LENGTH; i++) {
            points.add(nextPoint.add(DIR[direction].mul(i)));
        }
        return points;
    }

    public boolean robotCanReach(int x, int y) {
        return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
                (mapData[x][y] == '.' || mapData[x][y] == '>' || mapData[x][y] == 'R'
                        || mapData[x][y] == 'B'
                        || mapData[x][y] == 'C'
                        || mapData[x][y] == 'c'));//不是海洋或者障碍
    }

    public boolean robotCanReachDiscrete(int x, int y) {
        return (x > 0 && x < MAP_DISCRETE_HEIGHT - 1 && y > 0 && y < MAP_DISCRETE_WIDTH - 1 &&
                (robotDiscreteMapData[x][y]));//0和最后一行或者列是墙，不判断
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
        initRobotsDiscrete();
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                for (int k = 0; k < (DIR.length / 2); k++) {
                    boolean canReach = true, oneIn = false, allIn = true;
                    ArrayList<Point> boatPoints = getBoatPoints(new Point(i, j), k);
                    //都是主航道点
                    for (Point point : boatPoints) {
                        if (isMainChannel(point.x, point.y)) {
                            oneIn = true;
                        } else {
                            allIn = false;
                        }
                        if (!boatCanReach(point.x, point.y)) {
                            canReach = false;
                        }
                    }
                    boatCanReach_[i][j][k] = canReach;
                    boatHasOneInMainChannel_[i][j][k] = oneIn;
                    boatIsAllInMainChannel_[i][j][k] = allIn;
                }
            }
        }

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


    private void initRobotsDiscrete() {
        for (boolean[] data : robotDiscreteMapData) {
            Arrays.fill(data, true);
        }
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                if (mapData[i][j] == '*' || mapData[i][j] == '~' || mapData[i][j] == '#'
                        || mapData[i][j] == 'S'
                        || mapData[i][j] == 'K'
                        || mapData[i][j] == 'T') {
                    //海洋或者障碍;
                    Point point = posToDiscrete(i, j);
                    robotDiscreteMapData[point.x][point.y] = false;
                    //周围八个点和这个点都是不可达点
                    for (Point dir : DIR) {
                        Point tmp = point.add(dir);
                        if (robotCanReachDiscrete(tmp.x, tmp.y)) {
                            robotDiscreteMapData[tmp.x][tmp.y] = false;//不可达
                        }
                    }
                }
            }
        }
        //四条墙壁,不能走
        Arrays.fill(robotDiscreteMapData[0], false);
        Arrays.fill(robotDiscreteMapData[MAP_DISCRETE_WIDTH - 1], false);
        for (int i = 0; i < MAP_DISCRETE_HEIGHT; i++) {
            robotDiscreteMapData[i][0] = false;
        }
        for (int i = 0; i < MAP_DISCRETE_HEIGHT; i++) {
            robotDiscreteMapData[i][MAP_DISCRETE_WIDTH - 1] = false;
        }
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
