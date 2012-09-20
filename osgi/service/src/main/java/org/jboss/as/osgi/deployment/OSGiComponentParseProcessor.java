/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.osgi.framework.BundleActivator;

/**
 * Create a {@link ComponentDescription} for the {@link BundleActivator}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 02-Sep-2012
 */
public class OSGiComponentParseProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        OSGiMetaData metadata = depUnit.getAttachment(OSGiConstants.OSGI_METADATA_KEY);
        if (metadata == null || metadata.getBundleActivator() == null)
            return;

        String componentClass = metadata.getBundleActivator();
        String componentName = BundleActivator.class.getSimpleName();
        EEModuleDescription moduleDescription = depUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        ComponentDescription componentDescription = new ComponentDescription(componentName, componentClass, moduleDescription, depUnit.getServiceName());
        moduleDescription.addComponent(componentDescription);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
