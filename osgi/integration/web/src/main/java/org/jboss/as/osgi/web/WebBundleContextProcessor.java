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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.MessageBytes;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.as.web.ext.WebContextFactory;
import org.jboss.osgi.metadata.OSGiMetaData;

/**
 * Provide OSGi meatadata for WAB deployments
 *
 * @author Thomas.Diesler@jboss.com
 * @since 30-Nov-2012
 */
public class WebBundleContextProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        WarMetaData warMetaData = depUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        OSGiMetaData metadata = depUnit.getAttachment(OSGiConstants.OSGI_METADATA_KEY);
        if (warMetaData == null || metadata == null)
            return;

        // Attach the {@link WabContextFactory}
        depUnit.putAttachment(WebContextFactory.ATTACHMENT, new WabContextFactory());
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }

    static class WabContextFactory implements WebContextFactory {

        @Override
        public StandardContext createContext(DeploymentUnit depUnit) throws DeploymentUnitProcessingException {
            WabContext standardContext = new WabContext();
            return standardContext;
        }

        @Override
        public void postProcessContext(DeploymentUnit depUnit, StandardContext webContext) {
            // do nothing
        }
    }

    static class WabContext extends StandardContext {

        public WabContext() {
            super();
            pipeline.addValve(new WabContextValve());
        }
    }

    static class WabContextValve extends ValveBase {

        @Override
        public void invoke(Request request, Response response) throws IOException, ServletException {
            // Disallow any direct access to resources under WEB-INF or META-INF
            MessageBytes requestPathMB = request.getRequestPathMB();
            if ((requestPathMB.startsWithIgnoreCase("/OSGI-INF/", 0))
                || (requestPathMB.equalsIgnoreCase("/OSGI-INF"))
                || (requestPathMB.startsWithIgnoreCase("/OSGI-OPT/", 0))
                || (requestPathMB.equalsIgnoreCase("/OSGI-OPT"))) {
                notFound(response);
                return;
            }
            getNext().invoke(request, response);
        }

        private void notFound(HttpServletResponse response) {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException e) {
            }
        }
    }
}
