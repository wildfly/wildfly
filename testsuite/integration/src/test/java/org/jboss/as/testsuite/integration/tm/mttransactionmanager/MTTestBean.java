/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.tm.mttransactionmanager;

import org.jboss.logging.Logger;

/**
 * Server Side MultiThread TM test.
 * <p/>
 * Based on mbean.TMTest
 * <p/>
 * Note: This used to be a MBean in AS6, now it is a POJO.
 *
 * @author <a href="dimitris@jboss.org">Dimitris Andreadis</a>
 * @author istudens@redhat.com
 */
public class MTTestBean {
    private static final Logger log = Logger.getLogger(MTTestBean.class);

    public void testMTOperations(String test, MTOperation[][] ops) throws Exception {
        log.info("*** Starting test: " + test);
        MTOperation.init(log);

        // find out how many MTOperation[]
        // we'll execute each in a separate thread
        int numOfThreads = ops.length;
        log.info("Number of Threads: " + numOfThreads);

        Thread[] threads = new Thread[numOfThreads];
        ExecTask[] tasks = new ExecTask[numOfThreads];
        for (int i = 0; i < numOfThreads; i++) {
            tasks[i] = new ExecTask(i, ops[i]);
            threads[i] = new Thread(tasks[i]);
            threads[i].start();
        }

        // join the threads before returning and check
        // if any of threads exited with an exception
        Exception caughtException = null;
        for (int i = 0; i < numOfThreads; i++) {
            try {
                threads[i].join();
                if (tasks[i].exception != null) {
                    // remember any exception caught; order here is not important,
                    // since we don't know which thread finished first.
                    caughtException = tasks[i].exception;
                }
            } catch (InterruptedException e) {
                // retry
                i--;
            }
        }
        log.info("*** Finished test: " + test);
        MTOperation.destroy();

        if (caughtException != null) {
            throw caughtException;
        }
    }

    private class ExecTask implements Runnable {
        int threadId;
        MTOperation[] ops;
        Exception exception;

        public ExecTask(int threadId, MTOperation[] ops) {
            this.threadId = threadId;
            this.ops = ops;
        }

        public void run() {
            log.info("Starting thread: " + Thread.currentThread().getName());
            try {
                for (int i = 0; i < ops.length; ++i) {
                    ops[i].perform();
                }
            } catch (Exception e) {
                exception = e;
            } finally {
                log.info("Finished thread: " + Thread.currentThread().getName());
            }
        }
    }
}
