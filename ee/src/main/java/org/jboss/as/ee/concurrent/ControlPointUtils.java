/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ee.concurrent;

import org.wildfly.extension.requestcontroller.ControlPoint;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

/**
 * Class that manages the control point for executor services
 *
 * @author Stuart Douglas
 */
public class ControlPointUtils {

    public static Runnable doWrap(Runnable runnable,ControlPoint controlPoint) {
        if(controlPoint == null) {
            return runnable;
        }
        try {
            controlPoint.forceBeginRequest();
            return new ControlledRunnable(runnable, controlPoint);
        } catch (Exception e) {
            throw new RejectedExecutionException(e);
        }
    }

    public static <T> Callable<T> doWrap(Callable<T> callable, ControlPoint controlPoint) {
        if(controlPoint == null) {
            return callable;
        }
        try {
            controlPoint.forceBeginRequest();
            return new ControlledCallable<>(callable, controlPoint);
        } catch (Exception e) {
            throw new RejectedExecutionException(e);
        }
    }

    /**
     * Runnable that wraps a a runnable to allow server suspend/resume to work correctly.
     *
     */
    static class ControlledRunnable implements Runnable, ManagedTask {

        private final Runnable runnable;
        private final ControlPoint controlPoint;

        ControlledRunnable(Runnable runnable, ControlPoint controlPoint) {
            this.runnable = runnable;
            this.controlPoint = controlPoint;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } finally {
                controlPoint.requestComplete();
            }
        }
        @Override
        public Map<String, String> getExecutionProperties() {
            if(runnable instanceof ManagedTask) {
                ((ManagedTask) runnable).getExecutionProperties();
            }
            return null;
        }

        @Override
        public ManagedTaskListener getManagedTaskListener() {
            if(runnable instanceof ManagedTask) {
                ((ManagedTask) runnable).getManagedTaskListener();
            }
            return null;
        }
    }

    /**
     * Runnable that wraps a a runnable to allow server suspend/resume to work correctly.
     *
     */
    static class ControlledCallable<T> implements Callable<T>, ManagedTask {

        private final Callable<T> callable;
        private final ControlPoint controlPoint;

        ControlledCallable(Callable<T> callable, ControlPoint controlPoint) {
            this.callable = callable;
            this.controlPoint = controlPoint;
        }

        @Override
        public T call() throws Exception {
            try {
                return callable.call();
            } finally {
                controlPoint.requestComplete();
            }
        }

        @Override
        public Map<String, String> getExecutionProperties() {
            if(callable instanceof ManagedTask) {
                ((ManagedTask) callable).getExecutionProperties();
            }
            return null;
        }

        @Override
        public ManagedTaskListener getManagedTaskListener() {
            if(callable instanceof ManagedTask) {
                ((ManagedTask) callable).getManagedTaskListener();
            }
            return null;
        }
    }
}
