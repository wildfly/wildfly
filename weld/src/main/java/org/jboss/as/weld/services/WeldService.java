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
import org.jboss.as.weld.services.bootstrap.WeldEjbInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldEjbServices;
import org.jboss.as.weld.services.bootstrap.WeldJpaInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldResourceInjectionServices;
import org.jboss.as.weld.services.bootstrap.WeldSecurityServices;
import org.jboss.as.weld.services.bootstrap.WeldTransactionServices;
import org.jboss.as.weld.services.bootstrap.WeldValidationServices;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.jboss.weld.validation.spi.ValidationServices;

/**
 * The weld service.
 * <p>
 * Thread Safety: This class is immutable, and the underlying {@link WeldContainer} is thread safe.
 *
 * @author Stuart Douglas
 *
 */
public class WeldService implements Service<WeldContainer> {

    private static final Logger log = Logger.getLogger("org.jboss.weld");

    public static final ServiceName SERVICE_NAME = ServiceName.of("WeldService");

    private final WeldContainer weldContainer;

    private final InjectedValue<WeldEjbInjectionServices> ejbInjectionServices = new InjectedValue<WeldEjbInjectionServices>();
    private final InjectedValue<WeldEjbServices> ejbServices = new InjectedValue<WeldEjbServices>();
    private final InjectedValue<WeldJpaInjectionServices> jpaInjectionServices = new InjectedValue<WeldJpaInjectionServices>();
    private final InjectedValue<WeldResourceInjectionServices> resourceInjectionServices = new InjectedValue<WeldResourceInjectionServices>();
    private final InjectedValue<WeldSecurityServices> securityServices = new InjectedValue<WeldSecurityServices>();
    private final InjectedValue<WeldTransactionServices> weldTransactionServices = new InjectedValue<WeldTransactionServices>();
    private final InjectedValue<WeldValidationServices> validationServices = new InjectedValue<WeldValidationServices>();

    public WeldService(WeldContainer weldContainer) {
        this.weldContainer = weldContainer;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            log.info("Starting weld service");
            // set up injected services
            weldContainer.addWeldService(EjbServices.class, ejbServices.getValue());
            weldContainer.addWeldService(SecurityServices.class, securityServices.getValue());
            weldContainer.addWeldService(TransactionServices.class, weldTransactionServices.getValue());
            weldContainer.addWeldService(ValidationServices.class, validationServices.getValue());

            // TODO: this is a complete hack. We will need one instance of each service per bean deployment archive
            for (BeanDeploymentArchive bda : weldContainer.getBeanDeploymentArchives()) {
                bda.getServices().add(EjbInjectionServices.class, ejbInjectionServices.getValue());
                bda.getServices().add(JpaInjectionServices.class, jpaInjectionServices.getValue());
                bda.getServices().add(ResourceInjectionServices.class, resourceInjectionServices.getValue());
            }
            // start weld
            weldContainer.start();
        } catch (Exception e) {
            weldContainer.stop();
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        log.info("Stopping weld service");
        weldContainer.stop();
    }

    @Override
    public WeldContainer getValue() throws IllegalStateException {
        return weldContainer;
    }

    public InjectedValue<WeldTransactionServices> getWeldTransactionServices() {
        return weldTransactionServices;
    }

    public InjectedValue<WeldEjbInjectionServices> getEjbInjectionServices() {
        return ejbInjectionServices;
    }

    public InjectedValue<WeldEjbServices> getEjbServices() {
        return ejbServices;
    }

    public InjectedValue<WeldJpaInjectionServices> getJpaInjectionServices() {
        return jpaInjectionServices;
    }

    public InjectedValue<WeldResourceInjectionServices> getResourceInjectionServices() {
        return resourceInjectionServices;
    }

    public InjectedValue<WeldSecurityServices> getSecurityServices() {
        return securityServices;
    }

    public InjectedValue<WeldValidationServices> getValidationServices() {
        return validationServices;
    }

}
