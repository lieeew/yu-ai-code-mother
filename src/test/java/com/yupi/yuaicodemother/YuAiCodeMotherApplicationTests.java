package com.yupi.yuaicodemother;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class YuAiCodeMotherApplicationTests {

    @Test
    void contextLoads() {
    }

    public static void main(String[] args) {
        // 1. 获取主线程及其组（main组）
        Thread mainThread = Thread.currentThread();
        ThreadGroup mainGroup = mainThread.getThreadGroup();
        System.out.println("主线程: " + mainThread.getName() + ", 所属组: " + mainGroup.getName());

        // 2. 创建一个自定义子线程组（属于main组的子组）
        ThreadGroup subGroup = new ThreadGroup(mainGroup, "SubGroup");
        System.out.println("创建子组: " + subGroup.getName() + ", 父组: " + subGroup.getParent().getName());

        // 3. 在主线程组中创建一个子线程
        Thread childThreadInMain = new Thread(mainGroup, () -> {
            Thread current = Thread.currentThread();
            System.out.println("子线程（在main组）: " + current.getName() + ", 所属组: " + current.getThreadGroup().getName());
            try {
                Thread.sleep(5000); // 方便调试：在这里设置断点，观察线程栈
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        childThreadInMain.start();

        // 4. 在子线程组中创建一个子线程
        Thread childThreadInSub = new Thread(subGroup, () -> {
            Thread current = Thread.currentThread();
            System.out.println("子线程（在SubGroup）: " + current.getName() + ", 所属组: " + current.getThreadGroup().getName());
            try {
                Thread.sleep(5000); // 方便调试：在这里设置断点
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "ChildInSub");
        childThreadInSub.start();

        // 5. 枚举所有线程组和线程（用于调试观察）
        System.out.println("枚举所有活跃线程组:");
        mainGroup.getParent().list(); // 从system根组打印所有子组和线程

        // 保持主线程运行，便于调试
        try {
            Thread.sleep(10000); // 主线程睡眠，允许子线程运行；在IDEA中调试时可暂停这里
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
