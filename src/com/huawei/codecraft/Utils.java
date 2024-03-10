package com.huawei.codecraft;

import java.io.*;
import java.util.ArrayList;
import java.util.function.Function;

import static java.lang.Math.*;

public class Utils {
    public static BufferedReader inStream = new BufferedReader(new InputStreamReader(System.in));

//    static {
//        try {
//            inStream = new BufferedReader(new FileReader("in.txt"));
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private static final boolean ERROR = false;

    public static void printERROR(String s) {
        if (ERROR) {
            System.err.println(s);
        }
    }

    private static final boolean MOST = false;

    public static void printMOST(String s) {
        if (MOST) {
            System.err.println(s);
        }
    }

    private static final boolean DEBUG = false;

    public static void printDEBUG(String s) {
        if (DEBUG
        ) {
            System.err.println(s);
        }
    }

    public static final PrintStream outStream = new PrintStream(new BufferedOutputStream(System.out), true);

    public static final double ALG_EPS = 0.00001;


    public static boolean DoubleEqual(double a, double b) {
        return abs(a - b) < ALG_EPS;
    }

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

        Point div(int i) {
            return new Point(x / i, y / i);
        }

        Point add(Point v) {
            return new Point(x + v.x, y + v.y);
        }

        Point sub(Point v) {
            return new Point(x - v.x, y - v.y);
        }

        Point addEquals(Point v) {
            x += v.x;
            y += v.y;
            return this;
        }

        boolean equal(Point point) {
            return point.x == x && point.y == y;
        }

        boolean equal(int x, int y) {
            return this.x == x && this.y == y;
        }

        public int mul(Point p) {
            return x * p.x + y * p.y;
        }
    }

    public static final Point[] DIR = {
            new Point(1, 0),//右移
            new Point(-1, 0),//左移
            new Point(0, -1),//上移
            new Point(0, 1),//下移
            new Point(-1, -1),
            new Point(1, 1),
            new Point(-1, 1),
            new Point(1, -1),

    };

    public static ArrayList<Point> getPathByCs(int[][] cs, Point target) {
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

    public static class Vec2 // 向量或坐标
    {
        double x, y;


        public Vec2() {
        }

        public Vec2(Vec2 v) {
            this.x = v.x;
            this.y = v.y;
        }

        public Vec2(double x, double y) {
            this.x = x;
            this.y = y;
        }

        Vec2 sub() {
            return new Vec2(-x, -y);
        }

        Vec2 add(Vec2 v) {
            return new Vec2(x + v.x, y + v.y);
        }

        Vec2 sub(Vec2 v) {
            return new Vec2(x - v.x, y - v.y);
        }

        Vec2 mul(double v) {
            return new Vec2(x * v, y * v);
        }

        Vec2 divide(double v) {
            return new Vec2(x / v, y / v);
        }

        Vec2 addEquals(Vec2 v) {
            x += v.x;
            y += v.y;
            return this;
        }

        Vec2 mulEquals(double fMul) {
            x *= fMul;
            y *= fMul;
            return this;
        }


        boolean equals(Vec2 v) {
            return abs(x - v.x) < 0.0001 && abs(y - v.y) < 0.0001;
        }

        double Length() {
            return (double) sqrt(x * x + y * y);
        }

        double LengthSquare() {
            return x * x + y * y;
        }

        boolean IsZero() {
            return abs(x) < ALG_EPS && abs(y) < ALG_EPS;
        }

        Vec2 Unit()    // 求单位向量
        {
            double len = Length();
            return new Vec2(x / len, y / len);
        }
    }


    public static class Line {
        Vec2 a, b;  // 两个点标识一条线

        Line() {

        }

        Line(Vec2 a, Vec2 b) {
            this.a = a;
            this.b = b;
        }
    }

    public static double getAngle(Vec2 pos, Vec2 target) {
        Vec2 vec2 = target.sub(pos);
        if (vec2.Length() > 0.01) {
            return atan2(vec2.x, vec2.y);
        }
        return 0;
    }


    // 将弧度标准化为 [-pi, pi]
    public static double StandardizingAngle(double fAngle) {
        fAngle -= round(fAngle / (2 * PI)) * (2 * PI);

        while (fAngle > PI) {
            fAngle -= 2 * PI;
        }

        while (fAngle < -PI) {
            fAngle += 2 * PI;
        }

        return fAngle;
    }

    /**
     * 获得theta的相反方向
     *
     * @param theta 角度
     * @return 相反的角度
     */
    public static double getReverseTheta(double theta) {
        if (theta > 0) {
            return theta - PI;
        } else {
            return theta + PI;
        }
    }

    public static double sqr(double x) {
        return x * x;
    }


    // 求距离的平方
    public static double DistanceSqr(Vec2 p1, Vec2 p2) {
        return sqr(p1.x - p2.x) + sqr(p1.y - p2.y);
    }

    // 求距离
    public static double Distance(Vec2 p1, Vec2 p2) {
        return (double) sqrt(sqr(p1.x - p2.x) + sqr(p1.y - p2.y));
    }


    public static double Xmult(Vec2 p1, Vec2 p2, Vec2 p0) {
        return (p1.x - p0.x) * (p2.y - p0.y) - (p2.x - p0.x) * (p1.y - p0.y);
    }

    public static double Xmult(Vec2 v1, Vec2 v2) {
        return (v1.x) * (v2.y) - (v2.x) * (v1.y);
    }

    public static double Dot(Vec2 a, Vec2 b) {
        return a.x * b.x + a.y * b.y;
    }

    public static Vec2 Cross(double s, Vec2 a) {
        return new Vec2(-s * a.y, s * a.x);
    }


    //计算两直线交点,注意事先判断直线是否平行
    public static Vec2 Intersection(Vec2 u1, Vec2 u2, Vec2 v1, Vec2 v2) {
        Vec2 ret = new Vec2(u1);
        double t = ((u1.x - v1.x) * (v1.y - v2.y) - (u1.y - v1.y) * (v1.x - v2.x))
                / ((u1.x - u2.x) * (v1.y - v2.y) - (u1.y - u2.y) * (v1.x - v2.x));
        ret.x += (u2.x - u1.x) * t;
        ret.y += (u2.y - u1.y) * t;
        return ret;
    }


    //点到线段上的最近点
    public static Vec2 PtoSeg(Vec2 p, Line l) {
        Vec2 t = new Vec2(p);
        t.x += l.a.y - l.b.y;
        t.y += l.b.x - l.a.x;
        if (Xmult(l.a, t, p) * Xmult(l.b, t, p) > 0)
            return DistanceSqr(p, l.a) < DistanceSqr(p, l.b) ? l.a : l.b;
        return Intersection(p, t, l.a, l.b);
    }

    //点到线段距离
    public static double PtoSegDistance(Vec2 p, Line l) {
        Vec2 t = new Vec2(p);
        t.x += l.a.y - l.b.y;
        t.y += l.b.x - l.a.x;
        if (Xmult(l.a, t, p) * Xmult(l.b, t, p) > 0) {
            double fDistanceSqrA = DistanceSqr(p, l.a);
            double fDistanceSqrB = DistanceSqr(p, l.b);

            return fDistanceSqrA < fDistanceSqrB ? sqrt(fDistanceSqrA) : sqrt(fDistanceSqrB);
        }
        return abs(Xmult(p, l.a, l.b)) / Distance(l.a, l.b);
    }

    //点到线段距离, 并且当不能映射到线段时返回默认值
    public static double PtoSegDistance(Vec2 p, Line l, double fDefault) {
        Vec2 t = new Vec2(p);
        t.x += l.a.y - l.b.y;
        t.y += l.b.x - l.a.x;
        return Xmult(l.a, t, p) * Xmult(l.b, t, p) > 0 ? fDefault : abs(Xmult(p, l.a, l.b)) / Distance(l.a, l.b);
    }

    // 点到直线上的最近点
    public static Vec2 PtoLine(Vec2 p, Line l) {
        Vec2 t = new Vec2();
        t.x += l.a.y - l.b.y;
        t.y += l.b.x - l.a.x;
        return Intersection(p, t, l.a, l.b);
    }

    //点到直线距离
    public static double DisPtoLine(Vec2 p, Line l) {
        return abs(Xmult(p, l.a, l.b)) / Distance(l.a, l.b);
    }


    //判两点在线段异侧,点在线段上返回0
    public static boolean OppositeSide(Vec2 p1, Vec2 p2, Line l) {
        return Xmult(l.a, p1, l.b) * Xmult(l.a, p2, l.b) < -ALG_EPS;
    }


    //判两线段相交,不包括端点和部分重合
    public static boolean Intersect(Line u, Line v) {
        return OppositeSide(u.a, u.b, v) && OppositeSide(v.a, v.b, u);
    }

    // 判断直线相交
    public static Vec2 Intersection(Line u, Line v) {
        Vec2 ret = u.a;
        double t = ((u.a.x - v.a.x) * (v.a.y - v.b.y) - (u.a.y - v.a.y) * (v.a.x - v.b.x))
                / ((u.a.x - u.b.x) * (v.a.y - v.b.y) - (u.a.y - u.b.y) * (v.a.x - v.b.x));
        ret.x += (u.b.x - u.a.x) * t;
        ret.y += (u.b.y - u.a.y) * t;
        return ret;
    }

    // 判断圆和直线相交
    public static boolean IntersecLineCircle(Vec2 c, double r, Line line) {
        return DisPtoLine(c, line) < r + ALG_EPS;
    }


    // 计算圆和直线的交点
    public static void IntersectionLineCircle(Vec2 c, double r, Line line, Vec2 p1, Vec2 p2) {
        Vec2 p = c;
        double t;
        p.x += line.a.y - line.b.y;
        p.y += line.b.x - line.a.x;
        p = Intersection(p, c, line.a, line.b);
        t = (double) (sqrt(r * r - Distance(p, c) * Distance(p, c)) / Distance(line.a, line.b));
        p1.x = p.x + (line.b.x - line.a.x) * t;
        p1.y = p.y + (line.b.y - line.a.y) * t;
        p2.x = p.x - (line.b.x - line.a.x) * t;
        p2.y = p.y - (line.b.y - line.a.y) * t;
    }


    // 三点求圆心
    public static boolean GetCircleCenter(Vec2 p1, Vec2 p2, Vec2 p3, Vec2 out) {
        double x1 = p1.x;
        double y1 = p1.y;
        double x2 = p2.x;
        double y2 = p2.y;
        double x3 = p3.x;
        double y3 = p3.y;

        double A = x1 * (y2 - y3) - y1 * (x2 - x3) + x2 * y3 - x3 * y2;

        if (abs(A) < ALG_EPS)
            return false;

        double B = (sqr(x1) + sqr(y1)) * (y3 - y2) + (sqr(x2) + sqr(y2)) * (y1 - y3) + (sqr(x3) + sqr(y3)) * (y2 - y1);
        double C = (sqr(x1) + sqr(y1)) * (x2 - x3) + (sqr(x2) + sqr(y2)) * (x3 - x1) + (sqr(x3) + sqr(y3)) * (x1 - x2);

        out.x = -B / (2 * A);
        out.y = -C / (2 * A);
        return true;
    }


    // 计算向量在指定角度上的投影长度
    public static double GetShadow(Vec2 sVec2, double fAngle) {
        Vec2 stAnother = new Vec2((double) cos(fAngle), (double) sin(fAngle));
        return Dot(sVec2, stAnother);
    }
/*
inline double GetShadow(Vec2 sVec2, Vec2 stAnother)
{
    return Dot(sVec2, stAnother);
}*/

    // 判断两个区间是否重叠
    public static boolean IntervalOverlaped(double a1, double b1, double a2, double b2) {
        return !(b1 < a2 || b2 < a1);

    }

    // 求垂直向量
    public static Vec2 GetVerticalVec(Vec2 v) {
        double x = v.y;
        double y = -v.x;
        return new Vec2(x, y);
    }


    public static double InversIncFunc(Function<Double, Double> f, double result) {
        return InversIncFunc(f, result, 0, 10000);
    }


    // 二分反函数计算, 通过一个结果查值
// f函数必须单调递增
    public static double InversIncFunc(Function<Double, Double> f, double result, double minVal, double maxVal) {
        while (minVal + 0.0001 < maxVal) {
            double mid = (minVal + maxVal) / 2;
            if (f.apply(mid) < result) {
                minVal = mid;
            } else {
                maxVal = mid;
            }
        }
        return (minVal + maxVal) / 2;
    }

    public static boolean fgets(char[] result, BufferedReader inStream) {
        String s;
        try {
            s = inStream.readLine();
            printMOST(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (s == null) {
            return false;
        }
        System.arraycopy(s.toCharArray(), 0, result, 0, s.length());
        return true;
    }


}
