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

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.ServiceNames;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * Service that provides access to the BeanManger for a (sub)deployment
 *
 * @author Stuart Douglas
 *
 */
public class BeanManagerService implements Service<BeanManager> {

    public static final ServiceName NAME = ServiceNames.BEAN_MANAGER_SERVICE_NAME;

    private final InjectedValue<WeldBootstrapService> weldContainer = new InjectedValue<WeldBootstrapService>();
    private final String beanDeploymentArchiveId;
    private volatile BeanManagerImpl beanManager;

    public BeanManagerService(String beanDeploymentArchiveId) {
        this.beanDeploymentArchiveId = beanDeploymentArchiveId;
    }

    @Override
    public void start(StartContext context) throws StartException {
        beanManager = weldContainer.getValue().getBeanManager(beanDeploymentArchiveId);
    }

    @Override
    public void stop(StopContext context) {
        beanManager = null;
    }

    @Override
    public BeanManager getValue() throws IllegalStateException, IllegalArgumentException {
        return new BeanManagerProxy(beanManager);
    }

    public InjectedValue<WeldBootstrapService> getWeldContainer() {
        return weldContainer;
    }

    public static ServiceName serviceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append(BeanManagerService.NAME);
    }
}
