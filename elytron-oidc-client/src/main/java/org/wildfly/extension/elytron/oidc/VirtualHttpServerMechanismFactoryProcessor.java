/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron.oidc;

import static org.jboss.as.server.security.VirtualDomainUtil.clearVirtualDomainMetaDataSecurityDomain;
import static org.jboss.as.server.security.VirtualDomainUtil.configureVirtualDomain;
import static org.jboss.as.server.security.VirtualDomainUtil.getVirtualDomainMetaData;
import static org.jboss.as.server.security.VirtualDomainUtil.isVirtualDomainCreated;
import static org.jboss.as.web.common.VirtualHttpServerMechanismFactoryMarkerUtility.isVirtualMechanismFactoryRequired;
import static org.jboss.as.web.common.VirtualHttpServerMechanismFactoryMarkerUtility.virtualMechanismFactoryName;

import java.util.function.Consumer;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.security.VirtualDomainMarkerUtility;
import org.jboss.as.server.security.VirtualDomainMetaData;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;
import org.wildfly.security.http.oidc.OidcMechanismFactory;
import org.wildfly.security.http.oidc.OidcSecurityRealm;

/**
 * A {@link DeploymentUnitProcessor} to install a virtual HTTP server authentication mechanism factory if required.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class VirtualHttpServerMechanismFactoryProcessor implements DeploymentUnitProcessor {

    private static final String VIRTUAL_REALM = "virtual";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() != null || !isVirtualMechanismFactoryRequired(deploymentUnit)) {
            return;  // Only interested in installation if this is really the root deployment.
        }

        ServiceName virtualMechanismFactoryName = virtualMechanismFactoryName(deploymentUnit);
        ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(virtualMechanismFactoryName);

        final HttpServerAuthenticationMechanismFactory virtualMechanismFactory = new OidcMechanismFactory();
        final Consumer<HttpServerAuthenticationMechanismFactory> mechanismFactoryConsumer = serviceBuilder.provides(virtualMechanismFactoryName);
        serviceBuilder.setInstance(Service.newInstance(mechanismFactoryConsumer, virtualMechanismFactory));
        serviceBuilder.setInitialMode(Mode.ON_DEMAND);
        serviceBuilder.install();

        if (! isVirtualDomainCreated(deploymentUnit)) {
            ServiceName virtualDomainName = VirtualDomainMarkerUtility.virtualDomainName(deploymentUnit);
            VirtualDomainMetaData virtualDomainMetaData = getVirtualDomainMetaData(deploymentUnit);
            serviceBuilder = serviceTarget.addService(virtualDomainName);

            SecurityDomain.Builder virtualDomainBuilder = SecurityDomain.builder()
                    .addRealm(VIRTUAL_REALM, new OidcSecurityRealm()).build()
                    .setDefaultRealmName(VIRTUAL_REALM)
                    .setPermissionMapper((permissionMappable, roles) -> LoginPermission.getInstance());
            configureVirtualDomain(virtualDomainMetaData, virtualDomainBuilder);
            SecurityDomain virtualDomain = virtualDomainBuilder.build();
            if (virtualDomainMetaData != null) {
                virtualDomainMetaData.setSecurityDomain(virtualDomain);
            }
            Consumer<SecurityDomain> securityDomainConsumer = serviceBuilder.provides(new ServiceName[]{virtualDomainName});
            serviceBuilder.setInstance(Service.newInstance(securityDomainConsumer, virtualDomain));
            serviceBuilder.setInitialMode(Mode.ON_DEMAND);
            serviceBuilder.install();
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        clearVirtualDomainMetaDataSecurityDomain(deploymentUnit);
    }

}