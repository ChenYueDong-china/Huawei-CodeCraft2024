package com.huawei.codecraft2;

import com.huawei.codecraft2.Boat;
import com.huawei.codecraft2.Robot;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;

/**
 * @ Description
 * @ Date 2024/4/9 20:30
 */
public class Main {

    public static void main(String[] args) {
        Control.init();

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());  // 处理智能问答时的中文内容，建议指定标准输入字符集为UTF-8
        while (scanner.hasNext()) {
            Control.frameId = scanner.nextInt();
            Control.money = scanner.nextInt();
            Control.frameInput(scanner);
            if (Robot.robotNum < 4) {
                System.out.println("lbot " + TheMap.robotPurchasePoints.get(0)[0] + " " + TheMap.robotPurchasePoints.get(0)[1] + " 0");
                System.out.flush();
            }
            if (Boat.boatNum < 2) {
                System.out.println("lboat " + TheMap.boatPurchasePoints.get(0)[0] + " " + TheMap.boatPurchasePoints.get(0)[1]);
                System.out.flush();
            }
            Random random = new Random();
            int dir = random.nextInt(4);
            for (int i = 0; i < 4; i ++) {
                System.out.println("move " + i + " " + dir);
            }

            for (int i = 0; i < 2; i ++) {
                Boolean flag = random.nextBoolean();
                if (flag) {
                    System.out.println("ship " + i);
                } else {
                    System.out.println("rot " + i + " " + random.nextInt(2));
                }
            }
            System.out.println("OK");
            System.out.flush();
        }
    }

}