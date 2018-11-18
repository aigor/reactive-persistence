package org.coinen.reactive.persistence.ReactivePersistence; /**
 * Copyright (C) Zoomdata, Inc. 2012-2018. All rights reserved.
 */

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("Duplicates")
final class AppSchedulers {
    private AppSchedulers() { }

    static Scheduler ioScheduler(String prefix, int nThreads) {
        return Schedulers
            .fromExecutor(Executors.newFixedThreadPool(nThreads, new ThreadFactory() {
                private final AtomicInteger id = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName(prefix + "-" + id.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            }));
    }

    static ThreadPoolExecutor newExecutor(String prefix, int nThreads) {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads, new ThreadFactory() {
                private final AtomicInteger id = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName(prefix + "-" + id.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            });
    }
}
