package org.example;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



/**
 * @Author: Deng.
 * @Description:
 * @Date Created in $YEAR -$MONTH -$DAY $TIME
 * @Modified By:
 */
public class Main {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 200, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<>(500));

        /*
        * 1.10个核心线程是使用时才进行创建，没使用时不创建
        * 2.源码中，当任务数小于核心线程数，就会去创建新的线程，而不是复用之前的空闲线程
        * 3.本质上线程池是不公平的，当阻塞队列满了之后，后来的任务会直接用临时线程进行创建，如上面的参数就会有200-10=190个临时线程
        * 4.临时线程和核心线程本质上都是一样的（通过addWorker方法）,所以当临时线程和核心线程长时间不用的时候，且总线程数大于核心线程数
        *   不会优先销毁临时线程，而是销毁最先拿不到任务的线程（销毁相当于推出了while循环自动退出）。当总数小于等于核心线程数时，就会无限阻塞等待任务
        * 5.当线程出现异常的时候，该线程会被销毁并先创建了一个线程
        * 6.shutDown：把阻塞队列中的任务执行完再去关闭线程->把状态位改为shutdown
        *   shunDownNow中即使队列中还有任务都会立刻关闭线程（前提还是要把当前的任务执行完后，因为关闭线程的实质是退出循环，在不抛出异常的情况下需要进入下一轮循环才能退出）->把状态位改为stop
                if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                    decrementWorkerCount();
                    return null;
                }
        * 7.位于阻塞中的线程，会调用信号量中断线程发出异常，并把timeout设置为false。
        * */

        executor.execute(new Runnable() {
            @Override
            public void run() {

            }
        });



    }

}