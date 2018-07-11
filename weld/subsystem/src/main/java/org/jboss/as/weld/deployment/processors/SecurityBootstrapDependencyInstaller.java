/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.deployment.processors;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.security.service.SimpleSecurityManager;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld.services.bootstrap.WeldSecurityServices;
import org.jboss.as.weld.spi.BootstrapDependencyInstaller;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 *
 * @author Martin Kouba
 */
public class SecurityBootstrapDependencyInstaller implements BootstrapDependencyInstaller {

    @Override
    public ServiceName install(ServiceTarget serviceTarget, DeploymentUnit deploymentUnit, boolean jtsEnabled) {
        final WeldSecurityServices service = new WeldSecurityServices();

        final ServiceName serviceName = deploymentUnit.getServiceName().append(WeldSecurityServices.SERVICE_NAME);

        final ServiceBuilder sb = serviceTarget.addService(serviceName, service);
        final CapabilityServiceSupport capabilities = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (capabilities.hasCapability("org.wildfly.legacy-security")) {
            sb.addDependency(SimpleSecurityManagerService.SERVICE_NAME, SimpleSecurityManager.class, service.getSecurityManagerValue());
        }
        sb.install();
        return serviceName;
    }

}
