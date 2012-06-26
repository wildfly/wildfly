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
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.osgi.framework.AbstractBundleRevisionAdaptor;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XBundleRevisionBuilderFactory;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.osgi.framework.BundleContext;

/**
 * Processes deployments that have a Module attached.
 *
 * If so, register the module with the {@link XEnvironment}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 03-Jun-2011
 */
public class ModuleRegisterProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // Create the {@link ModuleRegisterService}
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final XBundle bundle = depUnit.getAttachment(Attachments.INSTALLED_BUNDLE_KEY);
        final Module module = depUnit.getAttachment(Attachments.MODULE);
        final ModuleSpecification moduleSpecification = depUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (bundle == null && module != null && moduleSpecification.isPrivateModule() == false) {
            LOGGER.infoRegisterModule(module.getIdentifier());
            try {
                final BundleContext context = depUnit.getAttachment(Attachments.SYSTEM_CONTEXT_KEY);
                XBundleRevisionBuilderFactory factory = new XBundleRevisionBuilderFactory() {
                    @Override
                    public XBundleRevision createResource() {
                        return new AbstractBundleRevisionAdaptor(context, module);
                    }
                };
                OSGiMetaData metadata = depUnit.getAttachment(OSGiConstants.OSGI_METADATA_KEY);
                XEnvironment env = depUnit.getAttachment(OSGiConstants.ENVIRONMENT_KEY);
                XResourceBuilder builder = XBundleRevisionBuilderFactory.create(factory);
                if (metadata != null) {
                    builder.loadFrom(metadata);
                } else {
                    builder.loadFrom(module);
                }
                XBundleRevision brev = (XBundleRevision) builder.getResource();
                env.installResources(brev);
                depUnit.putAttachment(OSGiConstants.REGISTERED_MODULE_KEY, brev);
            } catch (Throwable th) {
                throw MESSAGES.deploymentFailedToRegisterModule(th, module);
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        final XBundleRevision brev = depUnit.removeAttachment(OSGiConstants.REGISTERED_MODULE_KEY);
        if (brev != null) {
            LOGGER.infoUnregisterModule(brev.getModuleIdentifier());
            XEnvironment env = depUnit.getAttachment(OSGiConstants.ENVIRONMENT_KEY);
            env.uninstallResources(brev);
        }
    }
}
