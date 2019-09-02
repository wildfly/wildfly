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
package org.jboss.as.naming.deployment;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Adds a service that depends on all JNDI bindings from the deployment to be up.
 * <p/>
 * As binding services are not children of the root deployment unit service this service
 * is necessary to ensure the deployment is not considered complete until add bindings are up
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class JndiNamingDependencyProcessor implements DeploymentUnitProcessor {

    private static final ServiceName JNDI_DEPENDENCY_SERVICE = ServiceName.of("jndiDependencyService");
    private static final String JNDI_AGGREGATION_SERVICE_SUFFIX = "componentJndiDependencies";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        ServiceName namingStoreServiceName = support.getCapabilityServiceName(NamingService.CAPABILITY_NAME);
        //this will always be up but we need to make sure the naming service is
        //not shut down before the deployment is undeployed when the container is shut down
        phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, namingStoreServiceName);

        final ServiceName serviceName = serviceName(deploymentUnit.getServiceName());
        final ServiceBuilder<?> serviceBuilder = phaseContext.getServiceTarget().addService(serviceName, new RuntimeBindReleaseService());
        addAllDependencies(serviceBuilder, deploymentUnit.getAttachmentList(Attachments.JNDI_DEPENDENCIES));
        Map<ServiceName, Set<ServiceName>> compJndiDeps = deploymentUnit.getAttachment(Attachments.COMPONENT_JNDI_DEPENDENCIES);
        Set<ServiceName> aggregatingServices = installComponentJndiAggregatingServices(phaseContext.getServiceTarget(), compJndiDeps);
        addAllDependencies(serviceBuilder, aggregatingServices);
        if (deploymentUnit.getParent() != null) {
            addAllDependencies(serviceBuilder, deploymentUnit.getParent().getAttachment(Attachments.JNDI_DEPENDENCIES));
            compJndiDeps = deploymentUnit.getParent().getAttachment(Attachments.COMPONENT_JNDI_DEPENDENCIES);
            aggregatingServices = installComponentJndiAggregatingServices(phaseContext.getServiceTarget(), compJndiDeps);
            addAllDependencies(serviceBuilder, aggregatingServices);
        }
        serviceBuilder.requires(namingStoreServiceName);
        serviceBuilder.install();
    }

    private static Set<ServiceName> installComponentJndiAggregatingServices(final ServiceTarget target, final Map<ServiceName, Set<ServiceName>> mappings) {
        if (mappings == null) return Collections.emptySet();
        final Set<ServiceName> retVal = new HashSet<>();
        ServiceBuilder sb;
        ServiceName sn;
        for (final Map.Entry<ServiceName, Set<ServiceName>> mapping : mappings.entrySet()) {
            sn = mapping.getKey().append(JNDI_AGGREGATION_SERVICE_SUFFIX);
            retVal.add(sn);
            sb = target.addService(sn);
            for (final ServiceName depName : mapping.getValue()) sb.requires(depName);
            sb.install();
        }
        return retVal;
    }

    private static void addAllDependencies(final ServiceBuilder sb, final Collection<ServiceName> dependencies) {
        for (final ServiceName dependency : dependencies) {
            sb.requires(dependency);
        }
    }

    public static ServiceName serviceName(final ServiceName deploymentUnitServiceName) {
        return deploymentUnitServiceName.append(JNDI_DEPENDENCY_SERVICE);
    }

    public static ServiceName serviceName(final DeploymentUnit deploymentUnit) {
        return serviceName(deploymentUnit.getServiceName());
    }

    @Override
    public void undeploy(final DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.JNDI_DEPENDENCIES);
    }

}
