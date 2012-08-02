/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import org.jboss.as.web.ext.WebContextFactory;
import org.jboss.osgi.resolver.XBundle;

/**
 * This DUP adds a custom WebContextFactory attachment to the DeploymentUnit which is used by JBoss Web.
 * Later we can call back into the custom WebContextFactory to set the OSGi BundleContext in the
 * ServletContext.
 *
 * @author David Bosschaert
 */
public class WabServletContextFactoryProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();

        XBundle bundle = depUnit.getAttachment(OSGiConstants.BUNDLE_KEY);
        if (bundle != null) {
            WabServletContextFactory wscf = new WabServletContextFactory();
            depUnit.putAttachment(WebContextFactory.ATTACHMENT, wscf);
            bundle.getBundleRevision().addAttachment(WabServletContextFactory.class, wscf);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        context.removeAttachment(WebContextFactory.ATTACHMENT);

        XBundle bundle = context.getAttachment(OSGiConstants.BUNDLE_KEY);
        if (bundle != null) {
            bundle.getBundleRevision().removeAttachment(WabServletContextFactory.class);
        }
    }
}