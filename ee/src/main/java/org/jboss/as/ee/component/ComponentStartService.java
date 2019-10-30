/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service wrapper for a {@link Component} which starts and stops the component instance.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ComponentStartService implements Service<Component> {

    private final InjectedValue<BasicComponent> component = new InjectedValue<BasicComponent>();
    private final InjectedValue<ExecutorService> executor = new InjectedValue<ExecutorService>();

    /**
     * {@inheritDoc}
     */
    public void start(final StartContext context) throws StartException {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    getValue().start();
                    context.complete();
                } catch (Throwable e) {
                    context.failed(new StartException(e));
                }
            }
        };
        try {
            executor.getValue().submit(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop(final StopContext context) {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    getValue().stop();
                } finally {
                    context.complete();
                }
            }
        };
        try {
            executor.getValue().submit(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    /**
     * {@inheritDoc}
     */
    public BasicComponent getValue() throws IllegalStateException, IllegalArgumentException {
        return component.getValue();
    }

    /**
     * Get the component injector.
     *
     * @return the component injector
     */
    public Injector<BasicComponent> getComponentInjector() {
        return component;
    }

    public InjectedValue<ExecutorService> getExecutorInjector() {
        return executor;
    }
}
