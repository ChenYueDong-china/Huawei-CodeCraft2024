package com.huawei.codecraft;

import java.io.*;
import java.util.ArrayList;

public class Utils {
    @SuppressWarnings("all")
    public static BufferedReader inStream = new BufferedReader(new InputStreamReader(System.in));

//    static {
//        try {
//            inStream = new BufferedReader(new FileReader("in.txt"));
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private static final boolean ERROR = true;

    public static void printError(String s) {
        if (ERROR) {
            System.err.println(s);
        }
    }

    private static final boolean MOST = false;

    public static void printMost(String s) {
        if (MOST) {
            System.err.println(s);
        }
    }

    private static final boolean DEBUG = false;

    @SuppressWarnings("all")
    public static void printDebug(String s) {
        if (DEBUG) {
            System.err.println(s);
        }
    }

    public static final PrintStream outStream = System.out;


    public static class Point {
        int x, y;

        public Point() {
        }

        public Point(Point v) {
            this.x = v.x;
            this.y = v.y;
        }

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @SuppressWarnings("all")
        Point div(int i) {
            return new Point(x / i, y / i);
        }

        Point add(Point v) {
            return new Point(x + v.x, y + v.y);
        }

        Point sub(Point v) {
            return new Point(x - v.x, y - v.y);
        }

        @SuppressWarnings("all")
        Point addEquals(Point v) {
            x += v.x;
            y += v.y;
            return this;
        }

        boolean equal(Point point) {
            return point.x == x && point.y == y;
        }

        public boolean equals(Point point) {
            return x == point.x && y == point.y;
        }

        boolean equal(int x, int y) {
            return this.x == x && this.y == y;
        }

        public int mul(Point p) {
            return x * p.x + y * p.y;
        }

        public Point mul(int i) {
            return new Point(x * i, y * i);
        }
    }

    public static class PointWithDirection {
        public Point point;
        public int direction;

        public PointWithDirection(Point point, int direction) {
            this.point = point;
            this.direction = direction;
        }
    }

    public static final Point[] DIR = {
            new Point(0, 1),//右移
            new Point(0, -1),//左移
            new Point(-1, 0),//上移
            new Point(1, 0),//下移
            new Point(-1, -1),
            new Point(1, 1),
            new Point(-1, 1),
            new Point(1, -1),

    };

    public static PointWithDirection getBoatRotationPoint(PointWithDirection pointWithDirection, boolean clockwise) {
        Point corePint = pointWithDirection.point;
        int originDir = pointWithDirection.direction;
        int[] data;
        if (clockwise) {
            data = new int[]{0, 3, 1, 2, 0};
        } else {
            data = new int[]{0, 2, 1, 3, 0};
        }
        int nextDir = -1;
        for (int i = 0; i < DIR.length / 2; i++) {
            if (originDir == data[i]) {
                nextDir = data[i + 1];
                break;
            }
        }
        //位置
        Point[] data2;
        if (clockwise) {
            data2 = new Point[]{new Point(0, 0), new Point(2, 2), new Point(2, 0), new Point(0, 2)};
        } else {
            data2 = new Point[]{new Point(0, 0), new Point(0, 2), new Point(1, 1), new Point(-1, 1)};
        }
        Point nextPoint = corePint.add(data2[nextDir]).sub(data2[originDir]);
        return new PointWithDirection(nextPoint, nextDir);

    }

    public static int getIntInput() throws IOException {
        String line = inStream.readLine();
        printMost(line);
        return Integer.parseInt(line.trim().split(" ")[0]);
    }

    public static ArrayList<Point> getRobotPathByCs(int[][] cs, Point target) {
        ArrayList<Point> result = new ArrayList<>();
        Point t = new Point(target);
        result.add(new Point(t));
        while (cs[t.x][t.y] != 0) {
            t.addEquals(DIR[(cs[t.x][t.y] & 3) ^ 1]);
            result.add(new Point(t));
        }
        return result;
    }

    public static int getDir(Point point) {
        for (int i = 0; i < DIR.length; i++) {
            if (DIR[i].mul(point) > 0) {
                return i;
            }
        }
        assert false;
        return -1;
    }

    public static void fgets(char[] result, BufferedReader inStream) {
        String s;
        try {
            s = inStream.readLine();
            printMost(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (s == null) {
            return;
        }
        System.arraycopy(s.toCharArray(), 0, result, 0, s.length());
    }


}
