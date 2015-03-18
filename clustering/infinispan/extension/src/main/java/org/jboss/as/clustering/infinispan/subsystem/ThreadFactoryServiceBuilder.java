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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.ThreadFactory;

import org.jboss.as.threads.ThreadFactoryService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.Builder;

/**
 * Builder wrapping threads subsystem {@link ThreadFactory}.
 *
 * @author Radoslav Husar
 * @version Apr 2015
 */
class ThreadFactoryServiceBuilder extends ExecutorServiceNameBuilder implements Builder<ThreadFactory> {

    private String threadGroupName;
    private Integer priority;
    private String namePattern;

    ThreadFactoryServiceBuilder(final String cacheContainerName) {
        super(cacheContainerName);
    }

    @Override
    public ServiceBuilder<ThreadFactory> build(ServiceTarget target) {
        // Configure the service
        ThreadFactoryService service = new ThreadFactoryService();
        service.setNamePattern(namePattern);
        service.setPriority(priority);
        service.setThreadGroupName(threadGroupName);

        return target
                .addService(this.getServiceName(), service)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ServiceName getServiceName() {
        return super.getServiceName().append("thread-factory");
    }

    ThreadFactoryServiceBuilder setThreadGroupName(final String threadGroupName) {
        this.threadGroupName = threadGroupName;
        return this;
    }

    ThreadFactoryServiceBuilder setPriority(final Integer priority) {
        this.priority = priority;
        return this;
    }

    ThreadFactoryServiceBuilder setNamePattern(final String namePattern) {
        this.namePattern = namePattern;
        return this;
    }
}
