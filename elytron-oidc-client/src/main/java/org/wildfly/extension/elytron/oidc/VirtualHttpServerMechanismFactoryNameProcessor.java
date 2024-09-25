/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.jboss.as.server.security.SecurityMetaData.ATTACHMENT_KEY;
import static org.jboss.as.server.security.VirtualDomainMarkerUtility.virtualDomainName;
import static org.jboss.as.server.security.VirtualDomainUtil.setTopLevelDeploymentSecurityMetaData;
import static org.jboss.as.web.common.VirtualHttpServerMechanismFactoryMarkerUtility.isVirtualMechanismFactoryRequired;
import static org.jboss.as.web.common.VirtualHttpServerMechanismFactoryMarkerUtility.virtualMechanismFactoryName;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.security.AdvancedSecurityMetaData;
import org.jboss.as.server.security.SecurityMetaData;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.msc.service.ServiceName;

/**
 * A {@code DeploymentUnitProcessor} to set the {@code ServiceName} of any virtual HTTP server mechanism factory to be used
 * by the deployment.
 *
 * @author <a href="mailto:fjuma@jboss.com">Farah Juma</a>
 */
public class VirtualHttpServerMechanismFactoryNameProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return;
        }

        LoginConfigMetaData loginConfig = warMetaData.getMergedJBossWebMetaData().getLoginConfig();
        SecurityMetaData securityMetaData = deploymentUnit.getAttachment(ATTACHMENT_KEY);
        if (securityMetaData != null
                && (loginConfig != null && OidcActivationProcessor.OIDC_AUTH_METHOD.equals(loginConfig.getAuthMethod()))
                && isVirtualMechanismFactoryRequired(deploymentUnit)) {
            AdvancedSecurityMetaData advancedSecurityMetaData = new AdvancedSecurityMetaData();
            advancedSecurityMetaData.setHttpServerAuthenticationMechanismFactory(virtualMechanismFactoryName(deploymentUnit));
            ServiceName virtualDomainName = virtualDomainName(deploymentUnit);
            advancedSecurityMetaData.setSecurityDomain(virtualDomainName); // virtual mechanism factory implies virtual security domain
            deploymentUnit.putAttachment(ATTACHMENT_KEY, advancedSecurityMetaData);
            setTopLevelDeploymentSecurityMetaData(deploymentUnit, virtualDomainName);
        }
    }

}
