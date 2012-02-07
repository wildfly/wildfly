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
package org.jboss.as.webservices.publish;

import java.util.Map;

import org.jboss.as.controller.PathElement;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.SimpleAttachable;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

public class WSEndpointDeploymentUnit extends SimpleAttachable implements DeploymentUnit {

    private String deploymentName;

    public WSEndpointDeploymentUnit(ClassLoader loader, String context, Map<String,String> urlPatternToClassName, WebservicesMetaData metadata) {
        this.deploymentName = context + ".deployment";

        JBossWebMetaData jbossWebMetaData = new JBossWebMetaData();
        JAXWSDeployment jaxwsDeployment = new JAXWSDeployment();
        jbossWebMetaData.setContextRoot(context);
        for (String urlPattern : urlPatternToClassName.keySet()) {
            addEndpoint(jbossWebMetaData, jaxwsDeployment, urlPatternToClassName.get(urlPattern), urlPattern);
        }
        this.putAttachment(WSAttachmentKeys.CLASSLOADER_KEY, loader);
        this.putAttachment(WSAttachmentKeys.JAXWS_ENDPOINTS_KEY, jaxwsDeployment);
        this.putAttachment(WSAttachmentKeys.JBOSSWEB_METADATA_KEY, jbossWebMetaData);
        this.putAttachment(WSAttachmentKeys.WEBSERVICES_METADATA_KEY, metadata);
    }

    private void addEndpoint(JBossWebMetaData jbossWebMetaData, JAXWSDeployment jaxwsDeployment, String className, String urlPattern) {
        if (urlPattern == null) {
            urlPattern = "/*";
        } else {
            urlPattern = urlPattern.trim();
            if (!urlPattern.startsWith("/")) {
                urlPattern = "/" + urlPattern;
            }
        }
        jaxwsDeployment.addEndpoint(new POJOEndpoint(className, urlPattern));
    }

    @Override
    public ServiceName getServiceName() {
        return ServiceName.JBOSS.append("ws-endpoint-deployment").append(deploymentName);
    }

    @Override
    public DeploymentUnit getParent() {
        return null;
    }

    @Override
    public String getName() {
        return deploymentName;
    }

    @Override
    public ServiceRegistry getServiceRegistry() {
        return WSServices.getContainerRegistry();
    }

    @Override
    public ModelNode getDeploymentSubsystemModel(String subsystemName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelNode createDeploymentSubModel(String subsystemName, PathElement address) {
        throw new UnsupportedOperationException();
    }

}