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
package org.jboss.as.weld.services;

import java.util.function.Consumer;
import java.util.function.Supplier;
import jakarta.enterprise.inject.spi.BeanManager;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.ServiceNames;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.weld.bean.builtin.BeanManagerProxy;

/**
 * Service that provides access to the BeanManger for a (sub)deployment
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class BeanManagerService implements Service<BeanManager> {

    public static final ServiceName NAME = ServiceNames.BEAN_MANAGER_SERVICE_NAME;
    private final Consumer<BeanManager> beanManagerConsumer;
    private final Supplier<WeldBootstrapService> weldContainerSupplier;
    private final String beanDeploymentArchiveId;
    private volatile BeanManagerProxy beanManager;

    public BeanManagerService(final String beanDeploymentArchiveId,
                              final Consumer<BeanManager> beanManagerConsumer,
                              final Supplier<WeldBootstrapService> weldContainerSupplier) {
        this.beanDeploymentArchiveId = beanDeploymentArchiveId;
        this.beanManagerConsumer = beanManagerConsumer;
        this.weldContainerSupplier = weldContainerSupplier;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        beanManager = new BeanManagerProxy(weldContainerSupplier.get().getBeanManager(beanDeploymentArchiveId));
        beanManagerConsumer.accept(beanManager);
    }

    @Override
    public void stop(final StopContext context) {
        beanManagerConsumer.accept(null);
        beanManager = null;
    }

    @Override
    public BeanManager getValue() {
        return beanManager;
    }

    /**
     * Gets the Bean Manager MSC service name relative to the Deployment Unit.
     * <p>
     * Modules outside of weld subsystem should use WeldCapability instead to get the name of the Bean Manager service
     * associated to the deployment unit.
     *
     * @param deploymentUnit The deployment unit to be used.
     *
     * @return The Bean Manager service name.
     */
    public static ServiceName serviceName(final DeploymentUnit deploymentUnit) {
        return ServiceNames.beanManagerServiceName(deploymentUnit);
    }

}
