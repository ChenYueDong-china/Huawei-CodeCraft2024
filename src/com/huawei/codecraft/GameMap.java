package com.huawei.codecraft;


import java.util.ArrayList;
import java.util.Arrays;

import static com.huawei.codecraft.Constants.*;
import static com.huawei.codecraft.Utils.Point;
import static com.huawei.codecraft.Utils.DIR;

public class GameMap {


    private final char[][] mapData = new char[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];//列到行
    private final boolean[][] robotDiscreteMapData = new boolean[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH];//0不可达 1可达

    public final short[][] robotCommonHeuristicCs
            = new short[MAP_FILE_ROW_NUMS][MAP_FILE_ROW_NUMS];//寻路得时候复用cs

    int curVisitId = 0;
    public final int[][] robotVisits
            = new int[MAP_FILE_ROW_NUMS][MAP_FILE_ROW_NUMS];//寻路得时候复用cs
    public final short[][] robotCommonCs
            = new short[MAP_FILE_ROW_NUMS][MAP_FILE_ROW_NUMS];//寻路得时候复用cs

    public final boolean[][] robotDiscreteMainChannel
            = new boolean[MAP_DISCRETE_HEIGHT][MAP_DISCRETE_WIDTH];//robot离散化之后是不是主干道
    private final boolean[][][] boatCanReach_
            = new boolean[MAP_FILE_ROW_NUMS][MAP_FILE_ROW_NUMS][DIR.length / 2];//船是否能到达
    private final boolean[][][] boatIsAllInMainChannel_
            = new boolean[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][DIR.length / 2];//整艘船都在主航道上
    private final boolean[][][] boatHasOneInMainChannel_
            = new boolean[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][DIR.length / 2];//有至少一个点在主航道上

    private final int[][] boatAroundBerthId = new int[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];//船闪现能到达得泊位,只有在靠泊区和泊位有值
    private final int[][] partOfBerthId = new int[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS];//这个点如果是泊位，那么id是啥

    public final int[][][] boatVisits
            = new int[MAP_FILE_ROW_NUMS][MAP_FILE_ROW_NUMS][DIR.length / 2];//寻路得时候复用cs
    public final short[][][] boatCommonCs
            = new short[MAP_FILE_ROW_NUMS][MAP_FILE_COL_NUMS][DIR.length / 2];//寻路得时候复用cs
    public final boolean[][] commonConflictPoints
            = new boolean[MAP_FILE_ROW_NUMS][MAP_FILE_ROW_NUMS];//寻路得时候复用cs

    public final boolean[][] commonNoResultPoints
            = new boolean[MAP_FILE_ROW_NUMS][MAP_FILE_ROW_NUMS];//寻路得时候复用cs
    public final int[][] curWorkbenchId
            = new int[MAP_FILE_ROW_NUMS][MAP_FILE_ROW_NUMS];//寻路得时候复用cs

    public int boatGetFlashBerthId(int x, int y) {
        return boatAroundBerthId[x][y];
    }

    public void updateBerthAndAround(ArrayList<Point> AroundPoints, ArrayList<Point> corePoints, int berthId) {
        for (Point point : AroundPoints) {
            boatAroundBerthId[point.x][point.y] = berthId;
        }
        for (Point corePoint : corePoints) {
            partOfBerthId[corePoint.x][corePoint.y] = berthId;
        }
    }

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

    public boolean boatCanReach(int x, int y, int dir) {
        return isLegalPoint(x, y) && boatCanReach_[x][y][dir];
    }

    public boolean boatCanReach(Point corePoint, int dir) {
        return boatCanReach(corePoint.x, corePoint.y, dir);
    }

    public int getRotationDir(int curDir, int nextDir) {
        int[] data = {0, 3, 1, 2, 0};
        for (int i = 1; i < data.length; i++) {
            if (data[i] == nextDir && data[i - 1] == curDir) {
                return 0;
            }
        }
        for (int i = 1; i < data.length; i++) {
            if (data[i - 1] == nextDir && data[i] == curDir) {
                return 1;
            }
        }
        return -1;
    }

    public boolean isBoatMainChannel(int x, int y) {
        return (x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
                (mapData[x][y] == '~'//主航道
                        || mapData[x][y] == 'S'//购买地块
                        || mapData[x][y] == 'K'//靠泊区
                        || mapData[x][y] == 'B'//泊位
                        || mapData[x][y] == 'c'//交通地块
                        || mapData[x][y] == 'T'));//交货点
    }

    public int getBelongToBerthId(Point point) {
        return partOfBerthId[point.x][point.y];
    }


    public int getDiscreteBelongToBerthId(int x, int y) {
        assert x % 2 == 1 && y % 2 == 1;//偶数点是额外添加的点，不是真实点
        return partOfBerthId[x / 2][y / 2];
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

    public boolean isRobotMainChannel(int x, int y) {
        return x >= 0 && x < MAP_FILE_ROW_NUMS && y >= 0 && y < MAP_FILE_COL_NUMS &&
                (mapData[x][y] == '>'//陆地主干道
                        || mapData[x][y] == 'R'//机器人购买处
                        || mapData[x][y] == 'B'//泊位
                        || mapData[x][y] == 'c');//海陆
    }

    public boolean isRobotDiscreteMainChannel(int x, int y) {
        //最外层是墙壁
        return (x > 0 && x < MAP_DISCRETE_HEIGHT - 1 && y > 0 && y < MAP_DISCRETE_WIDTH - 1 &&
                (robotDiscreteMainChannel[x][y]));//海陆
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
        initRobotsDiscrete();
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                for (int k = 0; k < (DIR.length / 2); k++) {
                    boolean canReach = true, oneIn = false, allIn = true;
                    ArrayList<Point> boatPoints = getBoatPoints(new Point(i, j), k);
                    //都是主航道点
                    for (Point point : boatPoints) {
                        if (isBoatMainChannel(point.x, point.y)) {
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
        for (int[] part : partOfBerthId) {
            Arrays.fill(part, -1);
        }
        for (int[] around : boatAroundBerthId) {
            Arrays.fill(around, -1);
        }
        for (int[] id : curWorkbenchId) {
            Arrays.fill(id, -1);
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
        for (boolean[] mainChannel : robotDiscreteMainChannel) {
            Arrays.fill(mainChannel, true);
        }
        for (boolean[] data : robotDiscreteMapData) {
            Arrays.fill(data, true);
        }
        for (int i = 0; i < MAP_FILE_ROW_NUMS; i++) {
            for (int j = 0; j < MAP_FILE_COL_NUMS; j++) {
                if (!isRobotMainChannel(i, j)) {
                    //其他东西
                    Point point = posToDiscrete(i, j);
                    robotDiscreteMainChannel[point.x][point.y] = false;
                    //周围八个点和这个点都是不可达点,其实只需要四个点，因为其他四个不可达
                    for (Point dir : DIR) {
                        Point tmp = point.add(dir);
                        robotDiscreteMainChannel[tmp.x][tmp.y] = false;//不是主干道，需要检测
                    }
                }
                if (!robotCanReach(i, j)) {
                    //海洋或者障碍;
                    Point point = posToDiscrete(i, j);
                    robotDiscreteMapData[point.x][point.y] = false;
                    //周围八个点和这个点都是不可达点
                    for (Point dir : DIR) {
                        Point tmp = point.add(dir);
                        robotDiscreteMapData[tmp.x][tmp.y] = false;//不可达
                    }
                }
            }
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
