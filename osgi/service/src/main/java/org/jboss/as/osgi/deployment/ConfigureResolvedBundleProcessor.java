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

package org.jboss.as.osgi.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Attempt to resolve an OSGi deployment.
 *
 * If successful attach the resulting {@link BundleWiring}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Jun-2012
 */
public class ConfigureResolvedBundleProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        XBundle bundle = depUnit.getAttachment(Attachments.INSTALLED_BUNDLE);
        if (bundle == null || bundle.isResolved() == false)
            return;

        // No {@link Module} attachment for OSGi deployments that use content delegation
        ModuleSpecification moduleSpec = depUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (moduleSpec.getResourceRootDelegation() != null)
            return;

        XBundleRevision brev = bundle.getBundleRevision();
        Module module = brev.getModuleClassLoader().getModule();
        depUnit.putAttachment(Attachments.MODULE_IDENTIFIER, module.getIdentifier());
        depUnit.putAttachment(Attachments.MODULE, module);
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
