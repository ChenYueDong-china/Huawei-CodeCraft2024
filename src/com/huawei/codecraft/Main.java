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
            //老年代3700-512,新生代512,超过32kb对象直接放入老年代
            pb.command("java", "-jar", "-Xmn128m", "-Xms3800m", "-Xms3800m"
                    , "main.jar", "restart");
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
//        schedule();
    }

    private static void schedule() throws IOException, InterruptedException {
//        testLLM();
        strategy.init();
        strategy.mainLoop();
    }

    public static void testLLM() throws IOException, InterruptedException {
        BackgroundThread.Instance().init();
        int id1 = BackgroundThread.Instance().sendQuestion("下列哪种材质常用于快递包装中的填充物，具有轻便和环保的特点？ A. 塑料颗粒 B. 纸屑 C. 硬纸板 D. 木屑");
        int id2 = BackgroundThread.Instance().sendQuestion("使用哪种技术可以在长途运输过程中实时监控水果和蔬菜的状态？ A. RFID技术 B. GPS定位技术 C. 条形码技术 D. IoT技术");

//
//        int id2 = BackgroundThread.Instance().sendQuestion("下列哪种材质常用于快递包装中的填充物，具有轻便和环保的特点？ A. 塑料颗粒 B. 纸屑 C. 硬纸板 D. 木屑");
//        int id3 = BackgroundThread.Instance().sendQuestion("下列哪种材质常用于制作快递封箱胶带，具有良好的粘性和耐用性？ A. 透明胶带 B. 纸质胶带 C. 塑料胶带 D. 布质胶带");
//        int id4 = BackgroundThread.Instance().sendQuestion("使用GPS跟踪技术在电子产品长途运输中的主要目的是什么？ A. 控制环境温湿度 B. 实时跟踪货物位置 C. 减少运输成本 D. 自动装卸货物");
//        int id5 = BackgroundThread.Instance().sendQuestion("海上运输中，哪项措施不适用于防止货物受潮？ A. 使用密封集装箱 B. 在集装箱内放置干燥剂 C. 保持货物直接暴露在空气中 D. 使用防潮包装材料");
        Thread.sleep(1000);
        if (BackgroundThread.Instance().m_working) {
            Thread.sleep(1000);
        }
        System.out.println(BackgroundThread.Instance().getAnswer(id1));
        System.out.println(BackgroundThread.Instance().getAnswer(id2));
//        System.out.println(BackgroundThread.Instance().getAnswer(id3));
//        System.out.println(BackgroundThread.Instance().getAnswer(id4));
//        System.out.println(BackgroundThread.Instance().getAnswer(id5));
        BackgroundThread.Instance().exitThread();
    }

}
