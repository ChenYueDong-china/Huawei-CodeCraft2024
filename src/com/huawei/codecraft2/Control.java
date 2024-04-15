package com.huawei.codecraft2;

import com.huawei.codecraft2.Berth;
import com.huawei.codecraft2.Boat;
import com.huawei.codecraft2.Robot;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.huawei.codecraft2.Boat.boats;

/**
 * @ Description
 * @ Date 2024/4/10 9:34
 */
public class Control {
    public static int money;

    public static int frameId;

    public static int curFrameRobotNums;

    public static int curFrameBoatNums;

    public static int ownRobotNums;

    public static int ownBoatNums;

    public static int pendingRobotNums;

    public static List<Integer> ownRobotIds = new ArrayList<>();

    public static List<Integer> ownBoatIds = new ArrayList<>();

    public static void init() {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
        for (int i = 0; i < TheMap.MAP_SIZE; i++) {
            String currentLine = scanner.nextLine();
            TheMap.GRID[i] = currentLine.toCharArray();
            String ok = scanner.nextLine();
            System.out.println("OK");
            System.out.flush();
        }
        TheMap.processMap();

        TheMap.BERTH_NUM = scanner.nextInt();
        for (int i = 0; i < TheMap.BERTH_NUM; i++) {
            int id = scanner.nextInt();
            int x = scanner.nextInt();
            int y = scanner.nextInt();
            int loadingSpeed = scanner.nextInt();
            TheMap.berths.add(new Berth(x, y, loadingSpeed));
        }

        com.huawei.codecraft2.Boat.boatCapacity = scanner.nextInt();

        String ok = scanner.nextLine();
        System.out.println("OK");
        System.out.flush();
    }

    public static void frameInput(Scanner scanner) {

        String nxt = scanner.nextLine().trim();
        while (nxt.isEmpty()) {
            nxt = scanner.nextLine().trim();
        }
        int curFrameGoodNums = Integer.parseInt(nxt);
        for (int i = 0; i < curFrameGoodNums; i++) {
            int x = scanner.nextInt();
            int y = scanner.nextInt();
            int value = scanner.nextInt();
        }
        nxt = scanner.nextLine().trim();
        while (nxt.isEmpty()) {
            nxt = scanner.nextLine().trim();
        }
        curFrameRobotNums = Integer.parseInt(nxt);
        for (int i = 0; i < curFrameRobotNums; i++) {
            int id = scanner.nextInt();
            int goods = scanner.nextInt();
            int x = scanner.nextInt();
            int y = scanner.nextInt();
            Robot.robots.get(i).id = id;
            Robot.robots.get(i).goods = goods;
            Robot.robots.get(i).x = x;
            Robot.robots.get(i).y = y;
        }

        nxt = scanner.nextLine().trim();
        while (nxt.isEmpty()) {
            nxt = scanner.nextLine().trim();
        }
        curFrameBoatNums = Integer.parseInt(nxt);
        for (int i = 0; i < curFrameBoatNums; i++) {
            int id = scanner.nextInt();
            int num = scanner.nextInt();
            int x = scanner.nextInt();
            int y = scanner.nextInt();
            int dir = scanner.nextInt();
            int s = scanner.nextInt();
            boats.get(i).id = id;
            boats.get(i).num = num;
            boats.get(i).x = x;
            boats.get(i).y = y;
            boats.get(i).dir = dir;
            boats.get(i).s = s;
        }

        nxt = scanner.nextLine().trim();
        while (nxt.isEmpty()) {
            nxt = scanner.nextLine().trim();
        }
        ownRobotNums = Integer.parseInt(nxt);
        for (int i = 0; i < ownRobotNums; i++) {
            ownRobotIds.add(scanner.nextInt());
        }
        Robot.robotNum = ownRobotNums;

        nxt = scanner.nextLine().trim();
        while (nxt.isEmpty()) {
            nxt = scanner.nextLine().trim();
        }
        pendingRobotNums = Integer.parseInt(nxt);
        for (int i = 0; i < pendingRobotNums; i++) {
            String line = scanner.nextLine();
            String[] items = line.split(" ");
            int id = Integer.parseInt(items[0]);
            String question = String.join(" ", Arrays.copyOfRange(items, 1, items.length));
            Robot.robots.get(id).isPending = true;
            Robot.robots.get(id).question = question;
        }

        nxt = scanner.nextLine().trim();
        while (nxt.isEmpty()) {
            nxt = scanner.nextLine().trim();
        }
        ownBoatNums = Integer.parseInt(nxt);
        for (int i = 0; i < ownBoatNums; i++) {
            ownBoatIds.add(scanner.nextInt());
        }
        String ok = scanner.nextLine();
        if (!ok.equals("OK")) {
            ok = scanner.nextLine();
        }
        Boat.boatNum = ownBoatNums;
    }

}
