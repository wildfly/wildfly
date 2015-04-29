/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.tomcat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WebMetaDataHelper;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.ws.common.integration.WSConstants;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.deployment.WSFServlet;

/**
 * The modifier of jboss web meta data. It configures WS transport for every webservice endpoint plus propagates WS stack
 * specific context parameters if required.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
final class WebMetaDataModifier {

    WebMetaDataModifier() {
        super();
    }

    /**
     * Modifies web meta data to configure webservice stack transport and properties.
     *
     * @param dep webservice deployment
     */
    void modify(final Deployment dep) {
        final JBossWebMetaData jbossWebMD = WSHelper.getOptionalAttachment(dep, JBossWebMetaData.class);

        if (jbossWebMD != null) {
            this.configureEndpoints(dep, jbossWebMD);
            this.modifyContextRoot(dep, jbossWebMD);
        }
    }

    /**
     * Configures transport servlet class for every found webservice endpoint.
     *
     * @param dep webservice deployment
     * @param jbossWebMD web meta data
     */
    private void configureEndpoints(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        final String transportClassName = this.getTransportClassName(dep);
        WSLogger.ROOT_LOGGER.trace("Modifying servlets");

        // get a list of the endpoint bean class names
        final Set<String> epNames = new HashSet<String>();
        for (Endpoint ep : dep.getService().getEndpoints()) {
            epNames.add(ep.getTargetBeanName());
        }

        // fix servlet class names for endpoints
        for (final ServletMetaData servletMD : jbossWebMD.getServlets()) {
            final String endpointClassName = ASHelper.getEndpointClassName(servletMD);
            if (endpointClassName != null && endpointClassName.length() > 0) { // exclude JSP
                if (epNames.contains(endpointClassName)) {
                    // set transport servlet
                    servletMD.setServletClass(WSFServlet.class.getName());
                    WSLogger.ROOT_LOGGER.tracef("Setting transport class: %s for endpoint: %s", transportClassName, endpointClassName);
                    final List<ParamValueMetaData> initParams = WebMetaDataHelper.getServletInitParams(servletMD);
                    // configure transport class name
                    WebMetaDataHelper.newParamValue(WSFServlet.STACK_SERVLET_DELEGATE_CLASS, transportClassName, initParams);
                    // configure webservice endpoint
                    WebMetaDataHelper.newParamValue(Endpoint.SEPID_DOMAIN_ENDPOINT, endpointClassName, initParams);
                } else if (endpointClassName.startsWith("org.apache.cxf")) {
                    throw WSLogger.ROOT_LOGGER.invalidWSServlet(endpointClassName);
                }
            }
        }
    }

    /**
     * Modifies context root.
     *
     * @param dep webservice deployment
     * @param jbossWebMD web meta data
     */
    private void modifyContextRoot(final Deployment dep, final JBossWebMetaData jbossWebMD) {
        final String contextRoot = dep.getService().getContextRoot();
        if (WSLogger.ROOT_LOGGER.isTraceEnabled()) {
            WSLogger.ROOT_LOGGER.tracef("Setting context root: %s for deployment: %s", contextRoot, dep.getSimpleName());
        }
        jbossWebMD.setContextRoot(contextRoot);
    }

    /**
     * Returns stack specific transport class name.
     *
     * @param dep webservice deployment
     * @return stack specific transport class name
     * @throws IllegalStateException if transport class name is not found in deployment properties map
     */
    private String getTransportClassName(final Deployment dep) {
        String transportClassName = (String) dep.getProperty(WSConstants.STACK_TRANSPORT_CLASS);
        if (transportClassName == null) throw WSLogger.ROOT_LOGGER.missingDeploymentProperty(WSConstants.STACK_TRANSPORT_CLASS);
        return transportClassName;
    }

}
