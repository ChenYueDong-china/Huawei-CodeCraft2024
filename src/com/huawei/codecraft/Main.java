/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.huawei.codecraft;

import java.io.IOException;

/**
 * Main
 *
 * @since 2024-02-05
 */
public class Main {

    static Strategy strategy = new Strategy();

    public static void main(String[] args) throws IOException, InterruptedException {
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
            schedule();
        }
    }

    private static void schedule() throws IOException {
        //todo 多个船只碰撞避免，用java测试
        //todo 决策很难搞，12号再看看吧
        strategy.init();
        strategy.mainLoop();
    }


}
