package com.huawei.codecraft;

import java.io.IOException;

import static com.huawei.codecraft.Utils.inStream;
import static com.huawei.codecraft.Utils.printMOST;
import static com.huawei.codecraft.Utils.Point;

class Robot {
    boolean carry;
    Point pos = new Point();
    int status;

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
    }
}



