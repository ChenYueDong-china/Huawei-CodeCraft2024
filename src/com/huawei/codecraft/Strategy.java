package com.huawei.codecraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.huawei.codecraft.Utils.*;
import static com.huawei.codecraft.Constants.*;

public class Strategy {


    private int money;


    private String[] ch = new String[MAP_FILE_ROW_NUMS + 1];


    public static int workbenchId = 0;
    private final HashMap<Integer, Workbench> workbenches = new HashMap<>();
    private final Robot[] robots = new Robot[ROBOTS_PER_PLAYER];

    private final Berth[] berths = new Berth[BERTH_PER_PLAYER];

    private final Boat[] boats = new Boat[BOATS_PER_PLAYER];

    public void init() throws IOException {
        for (int i = 1; i <= MAP_FILE_ROW_NUMS; i++) {
            ch[i] = inStream.readLine();
            printMOST(ch[i]);
        }
        //码头
        for (int i = 0; i < BERTH_PER_PLAYER; i++) {
            String s = inStream.readLine();
            printMOST(s);
            String[] parts = s.trim().split(" ");
            int id = Integer.parseInt(parts[0]);
            berths[id] = new Berth();
            berths[id].x = Integer.parseInt(parts[1]);
            berths[id].y = Integer.parseInt(parts[2]);
            berths[id].transport_time = Integer.parseInt(parts[3]);
            berths[id].loading_speed = Integer.parseInt(parts[4]);
        }
        String s = inStream.readLine().trim();
        printMOST(s);
        int boatCapacity = Integer.parseInt(s);
        //船
        for (int i = 0; i < BOATS_PER_PLAYER; i++) {
            boats[i] = new Boat(boatCapacity);
        }
        //机器人
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            robots[i] = new Robot();
        }

        String okk = inStream.readLine();
        printMOST(okk);
        outStream.print("OK\n");
    }

    public void mainLoop() throws IOException {

        while (input()) {
            Random rand = new Random();
            for (int i = 0; i < ROBOTS_PER_PLAYER; i++)
                System.out.printf("move %d %d" + System.lineSeparator(), i, rand.nextInt(4) % 4);
            outStream.print("OK\n");
        }

    }

    private boolean input() throws IOException {
        String line = inStream.readLine();
        printMOST(line);
        if (line == null) {
            return false;
        }
        String[] parts = line.trim().split(" ");
        frameId = Integer.parseInt(parts[0]);
        money = Integer.parseInt(parts[1]);

        line = inStream.readLine();
        printMOST(line);
        parts = line.trim().split(" ");


        //新增工作台
        int num = Integer.parseInt(parts[0]);
        ArrayList<Integer> deleteIds = new ArrayList<>();//满足为0的删除
        for (Map.Entry<Integer, Workbench> entry : workbenches.entrySet()) {
            Workbench workbench = entry.getValue();
            workbench.remainTime--;
            if (workbench.remainTime == 0) {
                deleteIds.add(entry.getKey());
            }
        }
        for (Integer id : deleteIds) {
            workbenches.remove(id);
        }
        for (int i = 1; i <= num; i++) {
            Workbench workbench = new Workbench(workbenchId);
            workbench.input();
            workbenches.put(workbenchId, workbench);
            workbenchId++;
        }

        //机器人
        for (int i = 0; i < ROBOTS_PER_PLAYER; i++) {
            robots[i].input();
        }
        //船
        for (int i = 0; i < 5; i++) {
            boats[i].input();
        }
        String okk = inStream.readLine();
        printMOST(okk);
        return true;
    }
}
