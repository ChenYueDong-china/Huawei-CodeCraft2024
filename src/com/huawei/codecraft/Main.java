/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.huawei.codecraft;

import java.io.IOException;
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

    static Strategy strategy = new Strategy();

    public static void main(String[] args) throws IOException, InterruptedException {
        //Thread.sleep(15000);
//        schedule();
        // 如果在本地调试时不需要重启，在启动参数中添加restart，如：java -jar main.jar restart
        if (args.length == 0) {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("java", "-jar", "-Xmn512m", "-Xms1024m", "-Xmx1024m",
                    "-XX:TieredStopAtLevel=1", "main.jar", "restart");
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            p.waitFor();
        } else if (!args[0].equals("restart")) {
            System.out.println("err");
        } else {
            //do something
            //System.err.println("test");
            schedule();
        }
    }

    private static void schedule() throws IOException {
        strategy.init();
        strategy.mainLoop();
    }


}