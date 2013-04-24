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

import java.io.IOException;
import java.net.URL;
import org.apache.camel.CamelContext;
import org.jboss.as.camel.CamelConstants;
import org.jboss.as.camel.CamelContextFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;

/**
 * Processes deployments that can create a {@link CamelContext}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public class CamelContextCreateProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final String runtimeName = depUnit.getName();

        URL contextDefinitionURL = null;
        try {
            if (runtimeName.endsWith(CamelConstants.NAME_SUFFIX_CONTEXT_XML)) {
                contextDefinitionURL = depUnit.getAttachment(Attachments.DEPLOYMENT_CONTENTS).asFileURL();
            } else {
                VirtualFile rootFile = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
                for (VirtualFile child : rootFile.getChild("META-INF").getChildren()) {
                    if (child.getName().endsWith("-context.xml")) {
                        contextDefinitionURL = child.asFileURL();
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            throw MESSAGES.cannotCreateCamelContext(ex, runtimeName);
        }

        if (contextDefinitionURL == null)
            return;

        // Create the camel context
        CamelContext camelContext;
        try {
            Module module = depUnit.getAttachment(Attachments.MODULE);
            camelContext = CamelContextFactory.createSpringCamelContext(contextDefinitionURL, module.getClassLoader());
        } catch (Exception ex) {
            throw MESSAGES.cannotCreateCamelContext(ex, runtimeName);
        }

        // Add the camel context to the deployemnt
        depUnit.putAttachment(CamelConstants.CAMEL_CONTEXT_KEY, camelContext);
        // Add a dependency on the {@link CamelContextRegistry} to the next phase
        phaseContext.addDeploymentDependency(CamelConstants.CAMEL_CONTEXT_REGISTRY_NAME, CamelConstants.CAMEL_CONTEXT_REGISTRY_KEY);
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        depUnit.removeAttachment(CamelConstants.CAMEL_CONTEXT_KEY);
    }
}
