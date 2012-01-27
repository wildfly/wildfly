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
package org.jboss.as.clustering;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jboss.logging.Logger;

/**
 * Utility class that accepts objects into a queue and maintains a separate thread that reads them off the queue and passes them
 * to a registered "processor".
 *
 * @todo find a better home for this than the cluster module
 *
 * @author <a href="mailto://brian.stansberry@jboss.com">Brian Stansberry</a>
 */
class AsynchEventHandler implements Runnable {
    /**
     * Interface implemented by classes able to process the objects placed into an AsynchEventHandler's queue.
     */
    public static interface AsynchEventProcessor {
        void processEvent(Object event);
    }

    private String name;
    /** The LinkedQueue of events to pass to our processor */
    private BlockingQueue<Object> events = new LinkedBlockingQueue<Object>();
    /** Whether we're blocking on the queue */
    private boolean blocking;
    private AsynchEventProcessor processor;
    private boolean stopped = true;
    private Thread handlerThread;
    private final ClusteringImplLogger log;

    /**
     * Create a new AsynchEventHandler.
     *
     * @param processor object to which objects placed in the queue should be handed when dequeued
     * @param name name for this instance. Appended to the processor's class name to create a log category, and used to name to
     *        handler thread
     */
    public AsynchEventHandler(AsynchEventProcessor processor, String name) {
        super();
        this.processor = processor;
        if (name == null)
            name = "AsynchEventHandler";
        this.name = name;
        this.log = Logger.getMessageLogger(ClusteringImplLogger.class, processor.getClass().getName() + "." + name);
    }

    /**
     * Place the given object in the queue.
     *
     * @param event the object to asynchronously pass to the AsynchEventHandler.
     *
     * @throws InterruptedException if the thread is interrupted while blocking on the queue.
     */
    public void queueEvent(Object event) throws InterruptedException {
        if (event != null)
            events.add(event);
    }

    @Override
    public void run() {
        log.debugf("Begin %s Thread", name);
        stopped = false;
        boolean intr = false;
        try {
            while (!stopped) {
                try {
                    blocking = true;
                    Object event = events.take();
                    blocking = false;

                    if (!stopped) {
                        processor.processEvent(event);
                    }
                } catch (InterruptedException e) {
                    intr = true;
                    blocking = false;
                    if (stopped) {
                        log.debugf("%s Thread interrupted", name);
                        break;
                    }
                    log.threadInterrupted(e, name);
                } catch (Throwable t) {
                    log.errorHandlingAsyncEvent(t);
                }
            }
            log.debugf("End %s Thread", name);
        } finally {
            if (intr)
                Thread.currentThread().interrupt();
        }
    }

    /**
     * Starts the handler thread.
     */
    public void start() {
        handlerThread = new Thread(this, name + " Thread");
        handlerThread.start();
    }

    /**
     * Stops the handler thread.
     */
    public void stop() {
        stopped = true;
        if (blocking)
            handlerThread.interrupt(); // it's just waiting on the LinkedQueue

        if (handlerThread.isAlive()) {
            // Give it up to 100ms to finish whatever it's doing
            try {
                handlerThread.join(100);
            } catch (Exception ignored) {
            }
        }

        if (handlerThread.isAlive())
            handlerThread.interrupt(); // kill it
    }

    public boolean isStopped() {
        return stopped;
    }
}
