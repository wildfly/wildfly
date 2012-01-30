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

package org.jboss.as.clustering.lock.impl;

import org.jboss.as.clustering.impl.ClusteringImplLogger;
import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class SharedLocalYieldingClusterLockManagerService extends AsynchronousService<SharedLocalYieldingClusterLockManager> {

    public static ServiceName getServiceName(String name) {
        return CoreGroupCommunicationService.getServiceName(name).append("lock");
    }

    private final String name;
    private final InjectedValue<CoreGroupCommunicationService> service = new InjectedValue<CoreGroupCommunicationService>();
    private volatile SharedLocalYieldingClusterLockManager lockManager;

    public SharedLocalYieldingClusterLockManagerService(String name) {
        this.name = name;
    }

    public ServiceBuilder<SharedLocalYieldingClusterLockManager> build(ServiceTarget target) {
        return target.addService(getServiceName(this.name), this)
            .addDependency(CoreGroupCommunicationService.getServiceName(this.name), CoreGroupCommunicationService.class, this.service)
        ;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public SharedLocalYieldingClusterLockManager getValue() throws IllegalStateException, IllegalArgumentException {
        return this.lockManager;
    }

    @Override
    protected void start() throws Exception {
        CoreGroupCommunicationService service = this.service.getValue();
        this.lockManager = new SharedLocalYieldingClusterLockManager(this.name, service, service);
        this.lockManager.start();
    }

    @Override
    protected void stop() {
        if (this.lockManager != null) {
            try {
                this.lockManager.stop();
            } catch (Exception e) {
                ClusteringImplLogger.ROOT_LOGGER.lockManagerStopFailed(e);
            }
        }
    }
}
