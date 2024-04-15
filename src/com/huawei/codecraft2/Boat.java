package com.huawei.codecraft2;

import java.util.ArrayList;
import java.util.List;

/**
 * @ Description
 * @ Date 2024/4/9 20:41
 */
public class Boat {
    public static int boatNum;

    public static int boatCapacity;

    public static List<Boat> boats = new ArrayList<>();

    public int id;

    public int x;

    public int y;

    public int dir;

    public int num;

    public int s;

    public int status;

    public static final int boatPrice = 8000;


    public Boat() {
        x = 0;
        y = 0;
        dir = 0;
        num = 0;
        s = 0;
        status = 0;
    }


    static {
        for (int i = 0; i < 200; i++) {
            boats.add(new Boat());
        }
    }

}
