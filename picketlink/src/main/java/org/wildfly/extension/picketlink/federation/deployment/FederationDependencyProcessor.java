/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.picketlink.federation.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.picketlink.federation.config.IDPConfiguration;
import org.wildfly.extension.picketlink.federation.service.IdentityProviderService;
import org.wildfly.extension.picketlink.federation.service.PicketLinkFederationService;
import org.wildfly.extension.picketlink.federation.service.ServiceProviderService;

/**
 * <p> {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} that configures the necessary dependencies for a given
 * federation deployment. </p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class FederationDependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier MODULE_ORG_PICKETLINK = ModuleIdentifier.create("org.picketlink");

    public static final Phase PHASE = Phase.DEPENDENCIES;
    public static final int PRIORITY = 0;

    /**
     * Key under which a {@link org.wildfly.extension.picketlink.federation.service.PicketLinkFederationService} that should be used to
     * configure a deployment would be found.
     */
    static final AttachmentKey<PicketLinkFederationService> DEPLOYMENT_ATTACHMENT_KEY = AttachmentKey.create(PicketLinkFederationService.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        ServiceName federationServiceName = getFederationService(phaseContext);
        if (federationServiceName != null) {
            addDependency(phaseContext, federationServiceName);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // no-op
    }

    private ServiceName getFederationService(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deployment = phaseContext.getDeploymentUnit();
        ServiceRegistry serviceRegistry = phaseContext.getServiceRegistry();

        // We assume the mgmt ops that trigger IdentityProviderAddHandler or ServiceProviderAddHandler
        // run before the OperationStepHandler that triggers deploy. If not, that's a user mistake.
        // Since those handlers run first, we can count on MSC having services *registered* even
        // though we cannot count on them being *started*.
        ServiceController<?> service = serviceRegistry.getService(IdentityProviderService
                .createServiceName(deployment.getName()));

        if (service == null) {
            service = serviceRegistry.getService(ServiceProviderService.createServiceName(deployment.getName()));
        } else {
            IdentityProviderService identityProviderService = (IdentityProviderService) service.getService();
            IDPConfiguration idpType = identityProviderService.getValue().getConfiguration();

            if (idpType.isExternal()) {
                return null;
            }
        }

        if (service == null) {
            return null;
        }

        return service.getName();
    }

    private void addDependency(DeploymentPhaseContext phaseContext, ServiceName federationServiceName) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        ModuleLoader moduleLoader = Module.getBootModuleLoader();

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, MODULE_ORG_PICKETLINK, false, false, true, false));

        phaseContext.addDeploymentDependency(federationServiceName, DEPLOYMENT_ATTACHMENT_KEY);
    }
}
