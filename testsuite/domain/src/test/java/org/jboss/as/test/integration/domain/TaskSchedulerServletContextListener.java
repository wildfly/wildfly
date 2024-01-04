/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain;

import org.jboss.logging.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class TaskSchedulerServletContextListener implements ServletContextListener {

    static final Logger logger = Logger.getLogger(TaskSchedulerServletContextListener.class.getName());

    @Resource
    private ManagedExecutorService executor;

    static class ReSchedulingTask implements Runnable {

        private static int tasksCounter = 0;
        private static final int MAX_TASKS = 10;

        private static AtomicInteger nextTaskID = new AtomicInteger(0);
        private Integer taskID;
        private Executor executor;

        public ReSchedulingTask(Executor executor) {
            tasksCounter++;
            this.executor = executor;
            this.taskID = nextTaskID.incrementAndGet();
        }

        private void doIt() {
            logger.trace("Task #" + this.taskID + " is doing its job (sleeping) now...");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                logger.trace("Task #" + this.taskID + " got interrupted!");
                return;
            }
            logger.trace("... task #" + this.taskID + " has completed its job.");
        }

        @Override
        public void run() {
            // Doing my job
            doIt();
            // Rescheduling a new instance of me
            logger.trace("Task #" + this.taskID + " is rescheduling a new task now...");
            if (tasksCounter <= MAX_TASKS) {
                this.executor.execute(new ReSchedulingTask(this.executor));
            }
            logger.trace("... task #" + this.taskID + " has completed rescheduling a new task.");
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        logger.trace("Context destroyed.");
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        logger.trace("Context initialized.");
        logger.trace("Got executor: " + this.executor);
        logger.trace("Now scheduling a task from the ServletContextListener...");
        this.executor.execute(new ReSchedulingTask(this.executor));
        logger.trace("... completed scheduling a new task from the ServletContextListener.");
    }

}
