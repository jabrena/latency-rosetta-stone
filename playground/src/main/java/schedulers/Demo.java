package schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ThreadLocalRandom;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class Demo {

    public static void main(String[] args) {

        Demo demo = new Demo();

        /*
        demo.example1();
        demo.example2();
        demo.example3();
        demo.example4();
        demo.example5();
         */

        demo.example6();
    }

    private void example1() {
        for (int i = 0; i < 10; i++) {
            System.out.println(
                    "count - "
                            + i
                            + " - "
                            + Thread.currentThread()
                            + " - number: "
                            + ThreadLocalRandom.current().nextLong());
        }
    }

    private void example2() {
        Flux.range(0, 10)
                .doOnNext(
                        i ->
                                System.out.println(
                                        "count - "
                                                + i
                                                + " - "
                                                + Thread.currentThread()
                                                + " - number: "
                                                + ThreadLocalRandom.current().nextLong()))
                .subscribe();
    }

    private void example3() {
        ForkJoinPool.commonPool()
                .submit(
                        () -> {
                            for (int i = 0; i < 10; i++) {
                                System.out.println(
                                        "count - "
                                                + i
                                                + " - "
                                                + Thread.currentThread()
                                                + " - number: "
                                                + ThreadLocalRandom.current().nextLong());
                            }
                        })
                .join();
    }

    private void example4() {
        Flux.range(0, 10)
                .doOnNext(
                        i ->
                                System.out.println(
                                        "count - "
                                                + i
                                                + " - "
                                                + Thread.currentThread()
                                                + " - number: "
                                                + ThreadLocalRandom.current().nextLong()))
                .subscribeOn(Schedulers.parallel())
                .blockLast();
    }

    private void example5() {
        Flux<Integer> observable
                = Flux
                .range(0, 10)
                .doOnNext(
                        i ->
                                System.out.println(
                                        "count - "
                                                + i
                                                + " - "
                                                + Thread.currentThread()
                                                + " - number: "
                                                + ThreadLocalRandom.current().nextLong()));
        observable
                .subscribeOn(Schedulers.parallel())
                .blockLast();
    }

    private void example6() {
        List<ForkJoinTask> taskList = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int _i = i;
            ForkJoinTask<?> submit =
                    ForkJoinPool.commonPool()
                            .submit(
                                    () -> {
                                        System.out.println(
                                                "count - "
                                                        + _i
                                                        + " - "
                                                        + Thread.currentThread()
                                                        + " - number: "
                                                        + ThreadLocalRandom.current().nextLong());
                                    });
            taskList.add(submit);
        }

        taskList.forEach(ForkJoinTask::join);
    }

}
