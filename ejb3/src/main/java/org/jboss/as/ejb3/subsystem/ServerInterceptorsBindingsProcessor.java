/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.interceptor.server.ServerInterceptorCache;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class ServerInterceptorsBindingsProcessor implements DeploymentUnitProcessor {

    private final ServerInterceptorCache serverInterceptorCache;

    public ServerInterceptorsBindingsProcessor(final ServerInterceptorCache serverInterceptorCache){
        this.serverInterceptorCache = serverInterceptorCache;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        for (final ComponentDescription componentDescription : eeModuleDescription.getComponentDescriptions()) {
            if (!(componentDescription instanceof EJBComponentDescription)) {
                continue;
            }
            final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentDescription;
            ejbComponentDescription.setServerInterceptorCache(serverInterceptorCache);
        }
    }
}
