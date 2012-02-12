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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * runnable used to invoke local ejb async methods
 *
* @author Stuart Douglas
*/
public abstract class AsyncInvocationTask implements Runnable, Future {
    private final CancellationFlag cancelledFlag;

    private volatile boolean running = false;

    private boolean done = false;
    private Object result;
    private Exception failed;

   public AsyncInvocationTask( final CancellationFlag cancelledFlag) {
        this.cancelledFlag = cancelledFlag;
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        if (!mayInterruptIfRunning && running) {
            return false;
        }
        cancelledFlag.set(true);
        if (!running) {
            done();
            return true;
        }
        return false;
    }

    protected abstract Object runInvocation() throws Exception;

    public void run() {
        synchronized (this) {
            running = true;
            if (cancelledFlag.get()) {
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
        done();
    }

    private synchronized void setFailed(final Exception e) {
        this.failed = e;
        done();
    }

    private void done() {
        done = true;
        notifyAll();
    }

    @Override
    public boolean isCancelled() {
        return cancelledFlag.get();
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public synchronized Object get() throws InterruptedException, ExecutionException {
        while (!isDone()) {
            wait();
        }
        if (failed != null) {
            throw new ExecutionException(failed);
        }
        return result;
    }

    @Override
    public synchronized Object get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!isDone()) {
            wait(unit.toMillis(timeout));
            if (!isDone()) {
                throw MESSAGES.failToCompleteTaskBeforeTimeOut(timeout, unit);
            }
        }
        if (cancelledFlag.get()) {
            throw MESSAGES.taskWasCancelled();
        } else if (failed != null) {
            throw new ExecutionException(failed);
        }
        return result;
    }
}
