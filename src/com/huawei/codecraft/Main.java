/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.huawei.codecraft;

import java.util.Random;
import java.util.Scanner;

import static com.huawei.codecraft.Utils.printERROR;
import static com.huawei.codecraft.Utils.printMOST;

/**
 * Main
 *
 * @since 2024-02-05
 */
public class Main {
    private static final int n = 200;
    private static final int robot_num = 10;
    private static final int berth_num = 10;
    private static final int N = 210;

    private int money, boat_capacity, id;
    private String[] ch = new String[N];
    private int[][] gds = new int[N][N];

    private Robot[] robot = new Robot[robot_num + 10];
    private Berth[] berth = new Berth[berth_num + 10];
    private Boat[] boat = new Boat[10];

    private void init() {
        Scanner scanf = new Scanner(System.in);
        for (int i = 1; i <= n; i++) {
            ch[i] = scanf.nextLine();
            printMOST(ch[i]);
        }
        for (int i = 0; i < berth_num; i++) {
            int id = scanf.nextInt();
            berth[id] = new Berth();
            berth[id].x = scanf.nextInt();
            berth[id].y = scanf.nextInt();
            berth[id].transport_time = scanf.nextInt();
            berth[id].loading_speed = scanf.nextInt();
            printMOST(id + " " + berth[id].x + " " + berth[id].y + " " + berth[id].transport_time + " " + berth[id].loading_speed);
        }
        this.boat_capacity = scanf.nextInt();
        scanf.nextLine();
        String okk = scanf.nextLine();
        printMOST(okk);
        System.out.println("OK");
        System.out.flush();
    }

    private int input() {
        Scanner scanf = new Scanner(System.in);
        this.id = scanf.nextInt();
        this.money = scanf.nextInt();
        printMOST(this.id + " " + this.money);
        int num = scanf.nextInt();
        printMOST(num + "");
        for (int i = 1; i <= num; i++) {
            int x = scanf.nextInt();
            int y = scanf.nextInt();
            int val = scanf.nextInt();
            printMOST(x + " " + y + " " + val);
        }
        for (int i = 0; i < robot_num; i++) {
            robot[i] = new Robot();
            robot[i].goods = scanf.nextInt();
            robot[i].x = scanf.nextInt();
            robot[i].y = scanf.nextInt();
            int sts = scanf.nextInt();
            printMOST(robot[i].goods + " " + robot[i].x + " " + robot[i].y + " " + sts);
        }
        for (int i = 0; i < 5; i++) {
            boat[i] = new Boat();
            boat[i].status = scanf.nextInt();
            boat[i].pos = scanf.nextInt();
            printMOST(boat[i].status + " " + boat[i].pos);
        }
        scanf.nextLine();
        String okk = scanf.nextLine();
        printMOST(okk);
        return id;
    }

    public static void main(String[] args) {
        Main mainInstance = new Main();
        mainInstance.init();
        for (int zhen = 1; zhen <= 15000; zhen++) {
            int id = mainInstance.input();
            Random rand = new Random();
            for (int i = 0; i < robot_num; i++)
                System.out.printf("move %d %d" + System.lineSeparator(), i, rand.nextInt(4) % 4);
            if (zhen == 1) {
                System.out.println("ship 0 0");//0去0
                System.out.println("ship 1 0");
            }
            if (zhen == 500) {
                System.out.println("go 0");
                System.out.println("go 1");
//                System.out.println("ship 1 2");
            }
            System.out.println("OK");
            System.out.flush();
        }
    }

    class Robot {
        int x, y, goods;
        int status;
        int mbx, mby;

        public Robot() {
        }

        public Robot(int startX, int startY) {
            this.x = startX;
            this.y = startY;
        }
    }

    class Berth {
        int x;
        int y;
        int transport_time;
        int loading_speed;

        public Berth() {
        }

        public Berth(int x, int y, int transport_time, int loading_speed) {
            this.x = x;
            this.y = y;
            this.transport_time = transport_time;
            this.loading_speed = loading_speed;
        }
    }

    class Boat {
        int num;
        int pos;
        int status;
    }
}
