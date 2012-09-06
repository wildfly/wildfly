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

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletContext;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WebDeploymentService.ContextActivator;
import org.jboss.osgi.resolver.XBundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * This deployment unit processor registers the ServletContext as an OSGi Service for WAB deployments.
 *
 * @author David Bosschaert
 */
public class WabServletContextServiceProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        ContextActivator activator = depUnit.getAttachment(ContextActivator.ATTACHMENT_KEY);
        XBundle bundle = depUnit.getAttachment(OSGiConstants.BUNDLE_KEY);

        if (activator != null && bundle != null) {
            registerServletContext(bundle.getBundleContext(), activator.getContext().getServletContext());
        }
    }

    private void registerServletContext(BundleContext bundleContext, ServletContext sc) {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.web.symbolicname", bundleContext.getBundle().getSymbolicName());
        Dictionary<?,?> headers = bundleContext.getBundle().getHeaders();
        Object ver = headers.get(Constants.BUNDLE_VERSION);
        if (ver instanceof String)
            props.put("osgi.web.version", ver);

        Object wcp = headers.get("Web-ContextPath");
        if (wcp instanceof String)
            props.put("osgi.web.contextpath", wcp);

        bundleContext.registerService(ServletContext.class.getName(), sc, props);
        // Note that the OSGi Framework will automatically unregister the service when the bundle is stopped.
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // nothing to do
    }
}
