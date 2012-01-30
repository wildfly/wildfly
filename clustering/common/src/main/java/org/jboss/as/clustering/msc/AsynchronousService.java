/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.msc;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Base service class that executes service start/stop asynchronously.
 * @author Paul Ferraro
 */
public abstract class AsynchronousService<T> implements Service<T> {

    private final Executor executor = Executors.newCachedThreadPool();
    private final boolean startAsynchronously;
    private final boolean stopAsynchronously;

    protected AsynchronousService() {
        this(true, true);
    }

    protected AsynchronousService(boolean startAsynchronously, boolean stopAsynchronously) {
        this.startAsynchronously = startAsynchronously;
        this.stopAsynchronously = stopAsynchronously;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        if (this.startAsynchronously) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        AsynchronousService.this.start();
                        context.complete();
                    } catch (Exception e) {
                        context.failed(new StartException(e));
                    }
                }
            };
            context.asynchronous();
            this.executor.execute(task);
        } else {
            try {
                this.start();
            } catch (Exception e) {
                throw new StartException(e);
            }
        }
    }

    @Override
    public void stop(final StopContext context) {
        if (this.stopAsynchronously) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        AsynchronousService.this.stop();
                    } finally {
                        context.complete();
                    }
                }
            };
            context.asynchronous();
            this.executor.execute(task);
        } else {
            this.stop();
        }
    }

    protected abstract void start() throws Exception;

    protected abstract void stop();
}
