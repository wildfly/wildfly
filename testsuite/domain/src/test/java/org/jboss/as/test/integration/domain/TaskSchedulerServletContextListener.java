/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.domain;

import org.jboss.logging.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

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
