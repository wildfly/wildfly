package org.wildfly.clustering.diagnostics.extension;

/**
 * Describes a SFSB Deployment
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class SFSBDeployment {

    private final String deploymentName;
    private final String component;

    public SFSBDeployment(String deploymentName, String component) {
        this.deploymentName = deploymentName;
        this.component = component;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getComponent() {
        return component;
    }

    @Override
    public String toString() {
        return "SFSBDeployment: " +
                " deploymentName = " + deploymentName +
                ", componentName " + component;
    }
}
