/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment;

import java.util.function.BiFunction;

import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainService.ApplicationSecurityDomain;
import org.jboss.as.ejb3.subsystem.ApplicationSecurityDomainService.Registration;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A service that sets up the security domain mapping for an EJB deployment.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah</a>
 */
public class EJBSecurityDomainService implements Service<Void> {
    public static final ServiceName SERVICE_NAME = ServiceName.of("ejb3", "security-domain");

    private final InjectedValue<ApplicationSecurityDomain> applicationSecurityDomain = new InjectedValue<>();
    private final DeploymentUnit deploymentUnit;
    private Registration registration;

    public EJBSecurityDomainService(final DeploymentUnit deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        ApplicationSecurityDomain applicationSecurityDomain = getApplicationSecurityDomain();
        BiFunction<String, ClassLoader, Registration> securityFunction = applicationSecurityDomain != null ? applicationSecurityDomain.getSecurityFunction() : null;
        if (securityFunction != null) {
            final String deploymentName = deploymentUnit.getParent() == null ? deploymentUnit.getName() : deploymentUnit.getParent().getName() + "." + deploymentUnit.getName();
            final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
            final ClassLoader classLoader = module.getClassLoader();
            registration = securityFunction.apply(deploymentName, classLoader);
        }
    }

    @Override
    public synchronized void stop(StopContext context) {
        if (registration != null) {
            registration.cancel();
        }
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    public Injector<ApplicationSecurityDomain> getApplicationSecurityDomainInjector() {
        return applicationSecurityDomain;
    }

    private ApplicationSecurityDomain getApplicationSecurityDomain() {
        return applicationSecurityDomain.getOptionalValue();
    }

}
