package org.jboss.as.webservices.publish;

import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathElement;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.SimpleAttachable;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.as.webservices.util.WebMetaDataHelper;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

public class WSEndpointDeploymentUnit extends SimpleAttachable implements DeploymentUnit {

    private String deploymentName;

    public WSEndpointDeploymentUnit(ClassLoader loader, String context, Map<String,String> urlPatternToClassName) {
        this.deploymentName = context + ".deployment";

        JBossWebMetaData jbossWebMetaData = new JBossWebMetaData();
        jbossWebMetaData.setContextRoot(context);
        for (String urlPattern : urlPatternToClassName.keySet()) {
            addEndpoint(jbossWebMetaData, urlPatternToClassName.get(urlPattern), urlPattern);
        }
        this.putAttachment(WSAttachmentKeys.JBOSSWEB_METADATA_KEY, jbossWebMetaData);
        this.putAttachment(WSAttachmentKeys.CLASSLOADER_KEY, loader);
    }

    private void addEndpoint(JBossWebMetaData jbossWebMetaData, String className, String urlPattern) {
        final JBossServletsMetaData servlets = WebMetaDataHelper.getServlets(jbossWebMetaData);
        WebMetaDataHelper.newServlet(className, className, servlets);
        final List<ServletMappingMetaData> servletMappings = WebMetaDataHelper.getServletMappings(jbossWebMetaData);
        if (urlPattern == null) {
            urlPattern = "/*";
        } else {
            urlPattern = urlPattern.trim();
            if (!urlPattern.startsWith("/")) {
                urlPattern = "/" + urlPattern;
            }
        }
        final List<String> urlPatterns = WebMetaDataHelper.getUrlPatterns(urlPattern);
        WebMetaDataHelper.newServletMapping(className, urlPatterns, servletMappings);
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
        throw new RuntimeException("Can't get the deployment submodel from a " + WSEndpointDeploymentUnit.class + " instance");
    }

    @Override
    public ModelNode createDeploymentSubModel(String subsystemName, PathElement address) {
        throw new RuntimeException("Can't create a deployment submodel from a " + WSEndpointDeploymentUnit.class + " instance");
    }

}