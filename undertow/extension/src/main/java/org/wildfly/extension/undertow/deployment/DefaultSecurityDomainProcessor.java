/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.deployment;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

public class DefaultSecurityDomainProcessor implements DeploymentUnitProcessor {

    private String defaultSecurityDomain;
    public DefaultSecurityDomainProcessor(String securityDomain) {
        defaultSecurityDomain = securityDomain;
    }
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        phaseContext.getDeploymentUnit().putAttachment(UndertowAttachments.DEFAULT_SECURITY_DOMAIN, defaultSecurityDomain);

    }

    @Override
    public void undeploy(DeploymentUnit context) {
        context.removeAttachment(UndertowAttachments.DEFAULT_SECURITY_DOMAIN);

    }
}
