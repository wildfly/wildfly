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

import org.jboss.as.weld.WeldContainer;
import org.jboss.as.weld.WeldLogger;
import org.jboss.as.weld.services.bootstrap.WeldResourceInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldSecurityServices;
import org.jboss.as.weld.services.bootstrap.WeldTransactionServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.transaction.spi.TransactionServices;

/**
 * The weld service.
 * <p>
 * Thread Safety: This class is immutable, and the underlying {@link WeldContainer} is thread safe.
 *
 * @author Stuart Douglas
 *
 */
public class WeldService implements Service<WeldContainer> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("WeldService");

    private final WeldContainer weldContainer;
    private final String deploymentName;

    private final InjectedValue<WeldResourceInjectionServices> resourceInjectionServices = new InjectedValue<WeldResourceInjectionServices>();
    private final InjectedValue<WeldSecurityServices> securityServices = new InjectedValue<WeldSecurityServices>();
    private final InjectedValue<WeldTransactionServices> weldTransactionServices = new InjectedValue<WeldTransactionServices>();

    public WeldService(WeldContainer weldContainer, final String deploymentName) {
        this.weldContainer = weldContainer;
        this.deploymentName = deploymentName;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            WeldLogger.DEPLOYMENT_LOGGER.startingWeldService(deploymentName);
            // set up injected services
            weldContainer.addWeldService(SecurityServices.class, securityServices.getValue());
            weldContainer.addWeldService(TransactionServices.class, weldTransactionServices.getValue());

            for (BeanDeploymentArchive bda : weldContainer.getBeanDeploymentArchives()) {
                bda.getServices().add(ResourceInjectionServices.class, resourceInjectionServices.getValue());
            }
            // start weld
            weldContainer.start();
        } catch (Exception e) {
            try {
                weldContainer.stop();
            } catch(Exception ex) {

            }
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        WeldLogger.DEPLOYMENT_LOGGER.stoppingWeldService(deploymentName);
        weldContainer.stop();
    }

    @Override
    public WeldContainer getValue() throws IllegalStateException {
        return weldContainer;
    }

    public InjectedValue<WeldTransactionServices> getWeldTransactionServices() {
        return weldTransactionServices;
    }

    public InjectedValue<WeldResourceInjectionServices> getResourceInjectionServices() {
        return resourceInjectionServices;
    }

    public InjectedValue<WeldSecurityServices> getSecurityServices() {
        return securityServices;
    }

}
