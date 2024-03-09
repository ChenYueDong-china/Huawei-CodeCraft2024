package com.huawei.codecraft;

import java.io.IOException;
import java.util.ArrayList;

import static com.huawei.codecraft.Utils.*;

public class Robot {

    public int id;
    public boolean carry;
    public int carryValue;
    public Point lastBuyPos = new Point();
    public int estimateUnloadTime;
    public Point pos = new Point();
    public int status;

    public boolean assigned;
    public int targetBerthId = -1;//目标卖工作台id,这个是berth了，其实都一样
    public int targetWorkBenchId = -1;//目标买id，其实都一样
    public ArrayList<Point> path = new ArrayList<>();

    public Strategy strategy;

    public Robot(Strategy strategy) {
    }


    public void input() throws IOException {
        String line = inStream.readLine();
        printMOST(line);
        String[] parts = line.trim().split(" ");
        carry = Integer.parseInt(parts[0]) == 1;
        pos.x = Integer.parseInt(parts[1]);
        pos.y = Integer.parseInt(parts[2]);
        status = Integer.parseInt(parts[3]);
        assigned = false;
        path.clear();
    }

    public void buy() {
        outStream.printf("get %d\n", id);
        carry = true;
        carryValue = strategy.workbenches.get(targetWorkBenchId).value;
        lastBuyPos = new Point(strategy.workbenches.get(targetWorkBenchId).pos);
        strategy.workbenches.remove(targetWorkBenchId);//销毁工作台
        targetWorkBenchId = -1;
    }
}



