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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.catalina.core.StandardContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.web.ext.WebContextFactory;
import org.osgi.framework.BundleContext;

/**
 * This class is passed to JBoss Web as an attachment to the DeploymentUnit for WAB deployments. Once the OSGi
 * BundleContext is available, the OSGi-web lifecycle interceptor will call back into this class to set it in
 * ServletContext.
 *
 * @author David Bosschaert
 */
class WabServletContextFactory implements WebContextFactory {
    private static final String OSGI_BUNDLECONTEXT = "osgi-bundlecontext";

    private BundleContext bundleContext;
    private final List<StandardContext> contexts = new ArrayList<StandardContext>();

    @Override
    public StandardContext createContext(DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        StandardContext webContext = new StandardContext();
        synchronized (contexts) {
            contexts.add(webContext);

            if (bundleContext != null)
                webContext.getServletContext().setAttribute(OSGI_BUNDLECONTEXT, bundleContext);
        }

        return webContext;
    }

    @Override
    public void postProcessContext(DeploymentUnit deploymentUnit, StandardContext webContext) {
    }

    void setBundleContext(BundleContext bc) {
        synchronized (contexts) {
            bundleContext = bc;

            for (StandardContext ctx : contexts) {
                ServletContext sctx = ctx.getServletContext();
                sctx.setAttribute(OSGI_BUNDLECONTEXT, bc);
            }
        }
    }
}