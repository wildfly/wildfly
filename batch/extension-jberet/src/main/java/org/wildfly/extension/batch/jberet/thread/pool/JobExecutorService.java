/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.batch.jberet.thread.pool;

import org.jberet.spi.JobExecutor;
import org.jboss.as.threads.ManagedJBossThreadPoolExecutorService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JobExecutorService implements Service<JobExecutor> {

    private final InjectedValue<ManagedJBossThreadPoolExecutorService> threadPoolInjector = new InjectedValue<>();
    private WildFlyJobExecutor jobExecutor;

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        jobExecutor = new WildFlyJobExecutor(threadPoolInjector.getValue());
    }

    @Override
    public synchronized void stop(final StopContext context) {
        jobExecutor = null;
    }

    @Override
    public JobExecutor getValue() throws IllegalStateException, IllegalArgumentException {
        return jobExecutor;
    }

    public InjectedValue<ManagedJBossThreadPoolExecutorService> getThreadPoolInjector() {
        return threadPoolInjector;
    }
}
