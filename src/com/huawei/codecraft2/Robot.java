package com.huawei.codecraft2;

import java.util.ArrayList;
import java.util.List;

/**
 * @ Description
 * @ Date 2024/4/9 20:30
 */
public class Robot {
    public static int robotNum;

    public int id;

    public int x;

    public int y;

    public int goods;

    public int status;

    public int mbx;

    public int mby;

    public boolean isPending;

    public String question;

    public static List<Robot> robots = new ArrayList<>();

    public static final int robotPrice = 2000;

    public Robot() {}


    static {
        for (int i = 0; i < 600; i ++) {
            robots.add(new Robot());
        }
    }
}
