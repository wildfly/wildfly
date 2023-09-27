/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.ejb3.interceptor.server.ClientInterceptorCache;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.ejb.client.EJBClientInterceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class ClientInterceptorsBindingsProcessor implements DeploymentUnitProcessor {

    private final ClientInterceptorCache clientInterceptorCache;

    public ClientInterceptorsBindingsProcessor(final ClientInterceptorCache clientInterceptorCache) {
        this.clientInterceptorCache = clientInterceptorCache;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        try {
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            List<EJBClientInterceptor> clientInterceptors = deploymentUnit.getAttachment(org.jboss.as.ejb3.subsystem.Attachments.STATIC_EJB_CLIENT_INTERCEPTORS);
            if (clientInterceptors == null) {
                clientInterceptors = new ArrayList<>();
            }
            for (final Class<? extends EJBClientInterceptor> interceptorClass : clientInterceptorCache.getClientInterceptors()) {
                clientInterceptors.add(interceptorClass.newInstance());
            }
            deploymentUnit.putAttachment(org.jboss.as.ejb3.subsystem.Attachments.STATIC_EJB_CLIENT_INTERCEPTORS, clientInterceptors);

        } catch (InstantiationException | IllegalAccessException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }
}
