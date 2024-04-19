package com.huawei.codecraft;


import java.io.IOException;
import java.util.*;

import static com.huawei.codecraft.Utils.*;
import static com.huawei.codecraft.Constants.*;


public class SimpleRobot {
    public int type = 0;
    public boolean belongToMe = false;
    public int id;
    public Point lastP = new Point();
    public Point p = new Point();
    public int lastNum = 0;
    public int num = 0;
    public Queue<Point> path = new ArrayDeque<>();//保存50帧路径
    public Stack<Integer> goodList = new Stack<>();//保存50帧路径

    public int lastDir = -1;
    public int noMoveTime = 0;

    public void input() throws IOException {
        String line = inStream.readLine();
        printMost(line);
        String[] parts = line.trim().split(" ");
        id = Integer.parseInt(parts[0]);
        lastNum = num;
        num = Integer.parseInt(parts[1]);
        lastP = new Point(p);
        p.x = Integer.parseInt(parts[2]);
        p.y = Integer.parseInt(parts[3]);
        if (!lastP.equal(p)) {
            noMoveTime++;
            //移动了
            lastDir = getDir(p.sub(lastP));
            if (lastDir == -1) {
                lastDir = 0;
            }
        } else {
            noMoveTime = 0;
        }
        path.offer(new Point(p));
        if (path.size() > SIMPLE_ROBOT_PATH_LENGTH) {
            path.poll();
        }
    }

}
