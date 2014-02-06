package org.wildfly.clustering.diagnostics.extension;

/**
 * Describes a web application deployment.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class WebDeployment {

    private final String deploymentName ;
    private final String virtualHost ;
    private final String contextRoot ;

    public WebDeployment(String deploymentName, String virtualHost, String contextRoot) {
        this.deploymentName = deploymentName;
        this.virtualHost = virtualHost;
        this.contextRoot = contextRoot;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    @Override
    public String toString() {
        return "WebDeployment: " +
                "deploymentName = " + deploymentName +
                ", virtualHost = " + virtualHost +
                ", contextRoot = " + contextRoot ;
    }
}
