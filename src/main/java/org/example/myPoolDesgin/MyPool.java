package org.example.myPoolDesgin;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: Deng.
 * @Description:自定义线程池，理解线程池是怎么使用的
 * @Date Created in 2023 -08 -07 11:36
 * @Modified By:
 */


public class MyPool {
    public static void main(String[] args) {
        ThreadPool pool = new ThreadPool(2, 1000, TimeUnit.MILLISECONDS, 6, (queue, task) -> {
            //1.死等
//            queue.put(task);
            //2.超市等待
//            queue.offer(task, 500, TimeUnit.MILLISECONDS);
            //3.让调用者自己放弃
//            System.out.println("放弃任务："+task);
            //4.抛出异常，之后的任务也不会执行了
//            throw new Exception();
            //5.主线程自己去执行任务
            task.run();
            //淘汰掉队列中存在最早的任务
        });




//        for (int i = 0; i < 5; i++) {
//            int j = i ;
//            pool.execute(()->{
//                System.out.println("-------"+j);
//
//            });
//        }



//        //模拟任务数超过任务队列
//        for (int i = 0; i < 15; i++) {
//            int j = i ;
//            pool.execute(()->{ //相当于还是主线程先去尝试放给线程池中的worker进行执行，如果放不进就会死等
//                System.out.println("-------"+j);
//                try {
//                    Thread.sleep(40000L);
//                    System.out.println("-----------"+j+"-----任务休眠结束");
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//        }
        //模拟任务数超过任务队列，并且带超时时间
        for (int i = 0; i < 15; i++) {
            int j = i ;
            pool.execute(()->{ //相当于还是主线程先去尝试放给线程池中的worker进行执行，如果放不进就会死等
                System.out.println("-------"+j);
                try {
                    Thread.sleep(40000L);
                    System.out.println("-----------"+j+"-----任务休眠结束");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}




class ThreadPool{
    private BlockingQueue<Runnable> taskQueue ; //任务队列
    private HashSet<Worker> workers = new HashSet();//线程集合

    private int coreSize; // 核心线程数

    private long timeout; // 线程的超时时间

    private TimeUnit unit; // 时间的单位

    private RejectPolicy<Runnable> rejectPolicy;//拒绝策略

    public ThreadPool(int coreSize, long timeout, TimeUnit unit, int capacity,RejectPolicy<Runnable> rejectPolicy) {
        this.coreSize = coreSize;
        this.timeout = timeout;
        this.unit = unit;
        this.taskQueue =  new BlockingQueue<>(capacity);
        this.rejectPolicy = rejectPolicy;
    }

    //线程的包装类（可以理解为线程的抽象类），用于执行相关任务
    class Worker extends Thread{
        private Runnable task;


        public Worker(Runnable task){
            this.task = task;

        }

        @Override
        public void run() {
            //执行任务
            /*
            * 1.当task不为空，则需要执行任务
            * 2.当task执行完毕，则需要从阻塞队列中执行任务(反复执行)
            * */
//            while (task != null ||  (task = taskQueue.take()) != null){//无限等待
            while (task != null ||  (task = taskQueue.poll(timeout, unit)) != null){//带超时时间
                try {
                    System.out.println(String.format("正在执行run任务，任务的对象是%s", task));
                    task.run();
                }catch (Exception e){

                }finally {
                    System.out.println(String.format("task任务结束，任务的对象是%s", task));
                    task = null;
                }
            }
            //一旦退出循环，说明此时阻塞队列已经没有任务了，可以把这个线程移除掉
            synchronized (workers){
                System.out.println(String.format("当前 %s 的worker获取不到新的任务，被移除", this));
                workers.remove(this);
            }
        }
    }



    /* 任务执行逻辑
    * 1.当线程池中没有线程，则创建线程执行任务
    * 2.如果线程池中线程不足，则把任务放入阻塞队列
    * */

    public void execute(Runnable task){
        //将给worker来执行
        /*
        * 判断：任务数和核心数大小
        * 1.如果任务数超过coreSize，则加入阻塞队列存起来
        * 2.如果没有，则把任务加入线程池
        * */

        synchronized (workers) {
            if (workers.size()<coreSize){
                Worker worker = new Worker(task);
                System.out.println(String.format("需要新增worker, 线程为%s, worker为%s",task, worker));
                workers.add(worker);
                worker.start();//开启新的线程，这里就相当于脱离主线程了
            }else {
                System.out.println(String.format("当前线程数大于等于核心线程数，将任务加入任务队列"));
                //当阻塞队列满时，拒绝策略有
                /*
                * 1.死等-->put（）taskQueue.put(task);
                * 2.带超时时间的等-->offer()
                * 3.放弃任务执行
                * 4.主线程抛出异常，不继续放任务
                * 5.让调用者自己去执行任务
                * */
                //把权力下放，让线程池的使用者进行决定：策略模式->提供接口，不提供具体方法


                taskQueue.tryPut(rejectPolicy, task);
            }
        }

    }

}

class BlockingQueue<T>{
    //1.任务队列,双向列表
    private Deque<T> queue = new ArrayDeque<>();

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    //2.锁，取任务的时候需要互斥操作，避免一个任务给多个消费线程取
    private ReentrantLock lock = new ReentrantLock();

    //3.条件变量：消费者、生产者往队列取或者存的时候需要有限制
    private Condition fullWaitSet = lock.newCondition();//生产者
    private Condition emptyWaitSet = lock.newCondition();//消费者

    //4.容量
    private int capacity;

    //5.阻塞获取
    public T take(){
        lock.lock();
        try {
            //首先需要查看队列是否有元素，没有元素则需要等待
            while (queue.isEmpty()){
                System.out.println("阻塞队列为空，需要阻塞等待");
                try {
                    emptyWaitSet.await();
                    System.out.println("阻塞队列被唤醒待");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //当队列不空时，获取投元素并返回
            T e = queue.removeFirst();
            fullWaitSet.signal();
            return e;

        }finally {
            lock.unlock();
        }
    }

    //6.阻塞添加
    public void put(T task){
        lock.lock();
        try {
            //首先看队列是否满了，满了则不能添加，需要阻塞
            while (queue.size() == capacity){
                try {
                    System.out.println(String.format("等待加入任务队列%s", task));
                    fullWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //有空位，则需要放入队列尾部
            System.out.println(String.format("加入任务队列%s", task));
            queue.addLast(task);
            //放入后，说明此时队列起码有一个元素了，需要唤醒消费者信号量
            emptyWaitSet.signal();
        }finally {
            lock.unlock();
        }
    }

    //7.获得队列大小,加锁目的是防止获取长度的时候有其他线程插入
    public int size(){
        lock.lock();
        try{
            return queue.size();
        }finally {
            lock.unlock();
        }
    }

    //8.带超时的等待,如果一直取不到队列中的元素，说明生产者根本没有生产
    public T poll(long timeout, TimeUnit timeUnit){
        lock.lock();

        try {
            //首先需要查看队列是否有元素，没有元素则需要等待
            long nanos = timeUnit.toNanos(timeout);//将超时时间统一转换为纳秒
            while (queue.isEmpty()){
                try {
                    //返回的是剩余时间
                    nanos = emptyWaitSet.awaitNanos(nanos);
                    if (nanos<=0){
                        //表示已经超时了，无需再等等，直接返回null
                        return null;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //当队列不空时，获取投元素并返回
            T e = queue.removeFirst();
            fullWaitSet.signal();
            return e;

        }finally {
            lock.unlock();
        }
    }

    //9.带超时时间的阻塞添加
    public boolean offer(T task, long timeout, TimeUnit unit){
        lock.lock();
        try {
            long nanos = unit.toNanos(timeout);
            //首先看队列是否满了，满了则不能添加，需要阻塞
            while (queue.size() == capacity){
                try {
                    System.out.println(String.format("等待加入任务队列%s", task));
                    if (nanos<=0){//超过超时时间，添加失败
                        return false;
                    }
                    nanos = fullWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //有空位，则需要放入队列尾部
            System.out.println(String.format("加入任务队列%s", task));
            queue.addLast(task);
            //放入后，说明此时队列起码有一个元素了，需要唤醒消费者信号量
            emptyWaitSet.signal();
            return true;
        }finally {
            lock.unlock();
        }
    }

    public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
        lock.lock();
        try {
            /*
             * 1.判断队列是否已经满了
             * */
            if(queue.size() == capacity){
                rejectPolicy.reject(this, task);
            }else {//有空闲，直接加入队列就行
                //有空位，则需要放入队列尾部
                System.out.println(String.format("加入任务队列%s", task));
                queue.addLast(task);
                //放入后，说明此时队列起码有一个元素了，需要唤醒消费者信号量
                emptyWaitSet.signal();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }

    }
}


@FunctionalInterface
interface RejectPolicy<T>{
    void reject(BlockingQueue<T> queue, T task) throws Exception;
}

