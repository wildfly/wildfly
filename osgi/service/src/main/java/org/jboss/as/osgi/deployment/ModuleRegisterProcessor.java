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

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.service.ModuleRegistrationTracker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.resolver.XEnvironment;

/**
 * Processes deployments that have a Module attached.
 *
 * If so, register the module with the {@link XEnvironment}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 03-Jun-2011
 */
public class ModuleRegisterProcessor implements DeploymentUnitProcessor {

    private final ModuleRegistrationTracker registrationTracker;

    public ModuleRegisterProcessor(ModuleRegistrationTracker tracker) {
        this.registrationTracker = tracker;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // Don't register EAR nor WAR deployments
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, depUnit) || DeploymentTypeMarker.isType(DeploymentType.WAR, depUnit))
            return;

        // Don't register Bundle deployments
        if (depUnit.hasAttachment(OSGiConstants.BUNDLE_KEY))
            return;

        // Don't register private module deployments
        final Module module = depUnit.getAttachment(Attachments.MODULE);
        final ModuleSpecification moduleSpecification = depUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (module == null || moduleSpecification.isPrivateModule())
            return;

        OSGiMetaData metadata = depUnit.getAttachment(OSGiConstants.OSGI_METADATA_KEY);
        registrationTracker.registerModule(module, metadata);
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        final Module module = depUnit.getAttachment(Attachments.MODULE);
        registrationTracker.unregisterModule(module);
    }
}
