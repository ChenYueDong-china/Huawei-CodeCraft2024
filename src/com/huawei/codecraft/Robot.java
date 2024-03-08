package com.huawei.codecraft;

import java.io.IOException;
import java.util.ArrayList;

import static com.huawei.codecraft.Utils.inStream;
import static com.huawei.codecraft.Utils.printMOST;
import static com.huawei.codecraft.Utils.Point;

class Robot {

    public int id;
    public boolean carry;
    public Point pos = new Point();
    public int status;

    public boolean assign;
    public ArrayList<Point> path = new ArrayList<>();

    public Robot() {
    }


    public void input() throws IOException {
        String line = inStream.readLine();
        printMOST(line);
        String[] parts = line.trim().split(" ");
        carry = Integer.parseInt(parts[0]) == 1;
        pos.x = Integer.parseInt(parts[1]);
        pos.y = Integer.parseInt(parts[2]);
        status = Integer.parseInt(parts[3]);
        assign = false;
        path.clear();
    }
}



