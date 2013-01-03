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

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.msc.service.ServiceName;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.osgi.framework.Bundle;

/**
 * Handle deferred deployemnt phases.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 25-Sep-2012
 */
public class DeferredPhaseProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        XBundle bundle = depUnit.getAttachment(OSGiConstants.BUNDLE_KEY);
        if (bundle == null || bundle.isFragment())
            return;

        // Add a dependency on the Bundle RESOLVED service
        BundleManager bundleManager = depUnit.getAttachment(OSGiConstants.BUNDLE_MANAGER_KEY);
        ServiceName bundleResolve = bundleManager.getServiceName(bundle, Bundle.RESOLVED);
        phaseContext.addDeploymentDependency(bundleResolve, AttachmentKey.create(Object.class));

        // Add a dependency on the Module service
        XBundleRevision brev = bundle.getBundleRevision();
        ServiceName moduleService = ServiceModuleLoader.moduleServiceName(brev.getModuleIdentifier());
        phaseContext.addDeploymentDependency(moduleService, Attachments.MODULE);

        // Defer the module phase if the bundle is not resolved
        if (bundle.isResolved() == false) {
            depUnit.putAttachment(Attachments.DEFERRED_ACTIVATION_COUNT, new AtomicInteger());
            DeploymentUtils.addDeferredModule(depUnit);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
