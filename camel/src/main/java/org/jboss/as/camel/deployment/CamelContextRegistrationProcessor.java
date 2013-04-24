/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.camel.deployment;

import static org.jboss.as.camel.CamelMessages.MESSAGES;

import org.apache.camel.CamelContext;
import org.jboss.as.camel.CamelConstants;
import org.jboss.as.camel.CamelContextRegistry;
import org.jboss.as.camel.CamelContextRegistry.CamelContextRegistration;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Register a {@link CamelContext} with the {@link CamelContextRegistry}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public class CamelContextRegistrationProcessor implements DeploymentUnitProcessor {

    AttachmentKey<CamelContextRegistration> CAMEL_CONTEXT_REGISTRATION_KEY = AttachmentKey.create(CamelContextRegistration.class);

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelContext camelContext = depUnit.getAttachment(CamelConstants.CAMEL_CONTEXT_KEY);
        if (camelContext == null)
            return;

        // Register the camel context
        CamelContextRegistry registry = depUnit.getAttachment(CamelConstants.CAMEL_CONTEXT_REGISTRY_KEY);
        try {
            CamelContextRegistration registration = registry.registerCamelContext(camelContext);
            depUnit.putAttachment(CAMEL_CONTEXT_REGISTRATION_KEY, registration);
        } catch (Exception ex) {
            throw MESSAGES.cannotStartCamelContext(ex, camelContext);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        // Unregister the camel context
        CamelContextRegistration registration = depUnit.getAttachment(CAMEL_CONTEXT_REGISTRATION_KEY);
        if (registration != null) {
            registration.unregister();
        }
    }
}
