package com.huawei.codecraft;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

import static com.huawei.codecraft.Utils.*;
import static com.huawei.codecraft.Constants.*;

public class SimpleBoat {
    int id;
    int lastNum;
    int num;
    boolean belongToMe;
    int noMoveTime;
    int localId = -1;

    PointWithDirection pointWithDirection = new PointWithDirection(new Point(), -1);
    PointWithDirection lastPointWithDirection = new PointWithDirection(new Point(), -1);

    Point corePoint = new Point(-1, -1);

    public Queue<PointWithDirection> path = new ArrayDeque<>();//保存50帧路径
    int direction = -1;
    int status = 0;
    int lastStatus = 0;
    int value = 0;

    public void input() throws IOException {
        String line = inStream.readLine();
        printMost(line);
        String[] parts = line.trim().split(" ");
        id = Integer.parseInt(parts[0]);
        lastNum = num;
        num = Integer.parseInt(parts[1]);
        if (num == 0) {
            value = 0;
        }
        lastPointWithDirection = pointWithDirection;
        corePoint.x = Integer.parseInt(parts[2]);
        corePoint.y = Integer.parseInt(parts[3]);
        direction = Integer.parseInt(parts[4]);
        pointWithDirection = new PointWithDirection(new Point(corePoint), direction);
        lastStatus = status;
        status = Integer.parseInt(parts[5]);
        path.offer(pointWithDirection);
        if (path.size() > SIMPLE_BOAT_PATH_LENGTH) {
            path.poll();
        }
    }
}
