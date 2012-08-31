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

import static org.jboss.as.osgi.OSGiLogger.LOGGER;

import java.util.Collections;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.Attachments.BundleState;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.BundleManager;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XResolveContext;
import org.jboss.osgi.resolver.XResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.resolver.ResolutionException;

/**
 * Attach the {@link Module} for a resolved OSGi bundle.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 01-Jul-2012
 */
public class BundleResolveProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        Deployment deployment = depUnit.getAttachment(OSGiConstants.DEPLOYMENT_KEY);
        XBundle bundle = depUnit.getAttachment(OSGiConstants.BUNDLE_KEY);
        if (bundle == null || !deployment.isAutoStart())
            return;

        // Only process the top level deployment
        if (depUnit.getParent() != null)
            return;

        resolveBundle(phaseContext, bundle);
    }

    static void resolveBundle(DeploymentPhaseContext phaseContext, XBundle bundle) {
        XBundleRevision brev = bundle.getBundleRevision();
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        XEnvironment env = depUnit.getAttachment(OSGiConstants.ENVIRONMENT_KEY);
        XResolver resolver = depUnit.getAttachment(OSGiConstants.RESOLVER_KEY);
        BundleManager bundleManager = depUnit.getAttachment(OSGiConstants.BUNDLE_MANAGER_KEY);
        XResolveContext context = resolver.createResolveContext(env, Collections.singleton(brev), null);
        try {
            LOGGER.debugf("Resolve: %s", depUnit.getName());
            resolver.resolveAndApply(context);
            depUnit.putAttachment(Attachments.BUNDLE_STATE_KEY, BundleState.RESOLVED);

            // Add a dependency on the Bundle RESOLVED service
            ServiceName bundleResolve = bundleManager.getServiceName(bundle, Bundle.RESOLVED);
            phaseContext.addDeploymentDependency(bundleResolve, AttachmentKey.create(Object.class));

            // Add a dependency on the Module service
            ServiceName moduleService = ServiceModuleLoader.moduleServiceName(brev.getModuleIdentifier());
            phaseContext.addDeploymentDependency(moduleService, Attachments.MODULE);
        } catch (ResolutionException ex) {
            LOGGER.warnCannotResolve(ex.getUnresolvedRequirements());
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
