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
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Start/Stop the {@link CamelContext}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public class CamelContextActivationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelContext camelContext = depUnit.getAttachment(CamelConstants.CAMEL_CONTEXT_KEY);
        if (camelContext == null)
            return;

        // Start the camel context
        try {
            camelContext.start();
        } catch (Exception ex) {
            throw MESSAGES.cannotStartCamelContext(ex, camelContext);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        // Stop the camel context
        CamelContext camelContext = depUnit.getAttachment(CamelConstants.CAMEL_CONTEXT_KEY);
        if (camelContext != null) {
            try {
                camelContext.stop();
            } catch (Exception ex) {
                throw MESSAGES.cannotStopCamelContext(ex, camelContext);
            }
        }
    }
}
