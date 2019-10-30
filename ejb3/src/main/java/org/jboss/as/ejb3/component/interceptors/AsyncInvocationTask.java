/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.component.interceptors;

import static java.lang.Math.max;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.wildfly.common.Assert;

/**
 * runnable used to invoke local ejb async methods
 *
* @author Stuart Douglas
*/
public abstract class AsyncInvocationTask implements Runnable, Future<Object> {
    private final CancellationFlag cancelledFlag;

    private static final int ST_RUNNING = 0;
    private static final int ST_DONE = 1;
    private static final int ST_CANCELLED = 2;
    private static final int ST_FAILED = 3;

    private volatile int status = ST_RUNNING;
    private Object result;
    private Exception failed;

    public AsyncInvocationTask(final CancellationFlag cancelledFlag) {
        this.cancelledFlag = cancelledFlag;
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        if (status != ST_RUNNING) {
            return status == ST_CANCELLED;
        }
        if (cancelledFlag.cancel(mayInterruptIfRunning)) {
            status = ST_CANCELLED;
            done();
            return true;
        }
        return false;
    }

    protected abstract Object runInvocation() throws Exception;

    public void run() {
        synchronized (this) {
            if (! cancelledFlag.runIfNotCancelled()) {
                status = ST_CANCELLED;
                done();
                return;
            }
        }
        Object result;
        try {
            result = runInvocation();
        } catch (Exception e) {
            setFailed(e);
            return;
        }
        Future<?> asyncResult = (Future<?>) result;
        try {
            if(asyncResult != null) {
                result = asyncResult.get();
            }
        } catch (InterruptedException e) {
            setFailed(new IllegalStateException(e));
            return;
        } catch (ExecutionException e) {
            try {
                throw e.getCause();
            } catch (Exception ex) {
                setFailed(ex);
                return;
            } catch (Throwable throwable) {
                setFailed(new UndeclaredThrowableException(throwable));
                return;
            }
        }
        setResult(result);
        return;
    }

    private synchronized void setResult(final Object result) {
        this.result = result;
        status = ST_DONE;
        done();
    }

    private synchronized void setFailed(final Exception e) {
        this.failed = e;
        status = ST_FAILED;
        done();
    }

    private void done() {
        notifyAll();
    }

    @Override
    public boolean isCancelled() {
        return status == ST_CANCELLED;
    }

    @Override
    public boolean isDone() {
        return status != ST_RUNNING;
    }

    @Override
    public synchronized Object get() throws InterruptedException, ExecutionException {
        for (;;) switch (status) {
            case ST_RUNNING: wait(); break;
            case ST_CANCELLED: throw EjbLogger.ROOT_LOGGER.taskWasCancelled();
            case ST_FAILED: throw new ExecutionException(failed);
            case ST_DONE: return result;
            default: throw Assert.impossibleSwitchCase(status);
        }
    }

    @Override
    public synchronized Object get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long remaining = unit.toNanos(timeout);
        long start = System.nanoTime();
        for (;;) switch (status) {
            case ST_RUNNING: {
                if (remaining <= 0L) {
                    throw EjbLogger.ROOT_LOGGER.failToCompleteTaskBeforeTimeOut(timeout, unit);
                }
                // round up to the nearest millisecond
                wait((remaining + 999_999L) / 1_000_000L);
                remaining -= max(0L, System.nanoTime() - start);
                break;
            }
            case ST_CANCELLED: throw EjbLogger.ROOT_LOGGER.taskWasCancelled();
            case ST_FAILED: throw new ExecutionException(failed);
            case ST_DONE: return result;
            default: throw Assert.impossibleSwitchCase(status);
        }
    }
}
