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

package org.jboss.as.osgi.web;



import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WebDeploymentService.ContextActivator;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;

/**
 * Start/stop an OSGi webapp context according to bundle lifecycle changes.
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 26-Jun-2012
 */
public class WebContextActivationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        ContextActivator activator = depUnit.getAttachment(ContextActivator.ATTACHMENT_KEY);
        XBundle bundle = depUnit.getAttachment(OSGiConstants.BUNDLE_KEY);
        if (activator != null && bundle != null) {
            // Add the {@link ContextActivator} to the {@link XBundleRevision}
            XBundleRevision brev = bundle.getBundleRevision();
            brev.addAttachment(ContextActivator.class, activator);

            // Start the context when the bundle will get started automatically
            Deployment deployment = bundle.adapt(Deployment.class);
            if (deployment.isAutoStart()) {
                activator.startAsync();
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        ContextActivator activator = depUnit.getAttachment(ContextActivator.ATTACHMENT_KEY);
        XBundle bundle = depUnit.getAttachment(OSGiConstants.BUNDLE_KEY);
        if (activator != null && bundle != null) {
            bundle.adapt(Deployment.class).removeAttachment(ContextActivator.class);
        }
    }
}
