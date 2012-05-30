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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

/**
 * @author Paul Ferraro
 */
public class SharedLocalYieldingClusterLockManagerService implements Service<SharedLocalYieldingClusterLockManager> {

    public static ServiceName getServiceName(String name) {
        return CoreGroupCommunicationService.getServiceName(name).append("lock");
    }

    private final String name;
    private final Value<CoreGroupCommunicationService> service;
    private volatile SharedLocalYieldingClusterLockManager lockManager;

    public SharedLocalYieldingClusterLockManagerService(String name, Value<CoreGroupCommunicationService> service) {
        this.name = name;
        this.service = service;
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
    public void start(StartContext context) throws StartException {
        CoreGroupCommunicationService service = this.service.getValue();
        this.lockManager = new SharedLocalYieldingClusterLockManager(this.name, service, service);
        try {
            this.lockManager.start();
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (this.lockManager != null) {
            try {
                this.lockManager.stop();
            } catch (Exception e) {
                ClusteringImplLogger.ROOT_LOGGER.lockManagerStopFailed(e);
            }
        }
    }
}
