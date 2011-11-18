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

public class WSEndpointDeploymentUnit extends SimpleAttachable implements DeploymentUnit {

    private String deploymentName;

    public WSEndpointDeploymentUnit(ClassLoader loader, String context, Map<String,String> urlPatternToClassName) {
        this.deploymentName = context + ".deployment";

        JBossWebMetaData jbossWebMetaData = new JBossWebMetaData();
        JAXWSDeployment jaxwsDeployment = new JAXWSDeployment();
        jbossWebMetaData.setContextRoot(context);
        for (String urlPattern : urlPatternToClassName.keySet()) {
            addEndpoint(jbossWebMetaData, jaxwsDeployment, urlPatternToClassName.get(urlPattern), urlPattern);
        }
        this.putAttachment(WSAttachmentKeys.JBOSSWEB_METADATA_KEY, jbossWebMetaData);
        this.putAttachment(WSAttachmentKeys.JAXWS_ENDPOINTS_KEY, jaxwsDeployment);
        this.putAttachment(WSAttachmentKeys.CLASSLOADER_KEY, loader);
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