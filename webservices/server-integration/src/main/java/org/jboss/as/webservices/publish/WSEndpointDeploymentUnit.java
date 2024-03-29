/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.publish;

import java.security.AccessController;
import java.util.Map;
import java.util.StringTokenizer;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.SimpleAttachable;
import org.jboss.as.version.Stability;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

public class WSEndpointDeploymentUnit extends SimpleAttachable implements DeploymentUnit {

    private String deploymentName;

    public WSEndpointDeploymentUnit(ClassLoader loader, String context, Map<String,String> urlPatternToClassName, WebservicesMetaData metadata) {
        this(loader, context, urlPatternToClassName, new JBossWebMetaData(), metadata, null, null);
    }

    public WSEndpointDeploymentUnit(ClassLoader loader, String context, Map<String, String> urlPatternToClassName,
            JBossWebMetaData jbossWebMetaData, WebservicesMetaData metadata, JBossWebservicesMetaData jbwsMetaData,
            CapabilityServiceSupport capabilityServiceSupport) {
        this.deploymentName = context + ".deployment";

        JAXWSDeployment jaxwsDeployment = new JAXWSDeployment();
        if (jbossWebMetaData == null) {
            jbossWebMetaData = new JBossWebMetaData();
        }
        jbossWebMetaData.setContextRoot(context);
        String endpointName = null;
        String className = null;
        for (String urlPattern : urlPatternToClassName.keySet()) {
            className = urlPatternToClassName.get(urlPattern);
            endpointName = getShortName(className, urlPattern);
            addEndpoint(jbossWebMetaData, jaxwsDeployment, endpointName, className, urlPattern);
        }
        this.putAttachment(WSAttachmentKeys.CLASSLOADER_KEY, loader);
        this.putAttachment(WSAttachmentKeys.JAXWS_ENDPOINTS_KEY, jaxwsDeployment);
        this.putAttachment(WSAttachmentKeys.JBOSSWEB_METADATA_KEY, jbossWebMetaData);
        if (metadata != null) {
            this.putAttachment(WSAttachmentKeys.WEBSERVICES_METADATA_KEY, metadata);
        }
        if (jbwsMetaData != null) {
            this.putAttachment(WSAttachmentKeys.JBOSS_WEBSERVICES_METADATA_KEY, jbwsMetaData);
        }
        if (capabilityServiceSupport != null) {
            this.putAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT, capabilityServiceSupport);
        }
    }

    private String getShortName(String className, String urlPattern) {
        final StringTokenizer st = new StringTokenizer(urlPattern, "/*");
        final StringBuilder sb = new StringBuilder();
        String token = null;
        boolean first = true;
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            if (token != null) {
                if (!first) sb.append('.');
                sb.append(token);
                first = false;
            }
        }
        return first ? className : sb.toString();
    }

    private void addEndpoint(JBossWebMetaData jbossWebMetaData, JAXWSDeployment jaxwsDeployment, String endpointName, String className, String urlPattern) {
        if (urlPattern == null) {
            urlPattern = "/*";
        } else {
            urlPattern = urlPattern.trim();
            if (!urlPattern.startsWith("/")) {
                urlPattern = "/" + urlPattern;
            }
        }
        jaxwsDeployment.addEndpoint(new POJOEndpoint(endpointName, className, null, urlPattern, false));
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
        return currentServiceContainer();
    }

    @Override
    public Stability getStability() {
        // This is not a "real" deployment unit
        return Stability.DEFAULT;
    }

    //@Override
    public ModelNode getDeploymentSubsystemModel(String subsystemName) {
        throw new UnsupportedOperationException();
    }

    //@Override
    public ModelNode createDeploymentSubModel(String subsystemName, PathElement address) {
        throw new UnsupportedOperationException();
    }

    //@Override
    public ModelNode createDeploymentSubModel(String subsystemName, PathAddress address) {
        throw new UnsupportedOperationException();
    }

    //@Override
    public ModelNode createDeploymentSubModel(String subsystemName, PathAddress address, Resource resource) {
        throw new UnsupportedOperationException();
    }

    private static ServiceContainer currentServiceContainer() {
        if(System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
