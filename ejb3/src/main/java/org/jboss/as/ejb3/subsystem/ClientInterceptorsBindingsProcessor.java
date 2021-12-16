/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
            final List<EJBClientInterceptor> clientInterceptors = new ArrayList<>();
            for (final Class<? extends EJBClientInterceptor> interceptorClass : clientInterceptorCache.getClientInterceptors()) {
                clientInterceptors.add(interceptorClass.newInstance());
            }
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            deploymentUnit.putAttachment(org.jboss.as.ejb3.subsystem.Attachments.STATIC_EJB_CLIENT_INTERCEPTORS, clientInterceptors);

        } catch (InstantiationException | IllegalAccessException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }
}
