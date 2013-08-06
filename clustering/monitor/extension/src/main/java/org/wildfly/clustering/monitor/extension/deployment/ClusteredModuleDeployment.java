package org.wildfly.clustering.monitor.extension.deployment;

import java.util.Map;

import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;

/**
 * Information representing a clustered module deployment.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusteredModuleDeployment {

    private DeploymentModuleIdentifier identifier;
    private ClusteredWarDeploymentInformation war;
    private Map<String, ClusteredEjbDeploymentInformation> ejbs;

    public ClusteredModuleDeployment(DeploymentModuleIdentifier identifier, ClusteredWarDeploymentInformation war, Map<String, ClusteredEjbDeploymentInformation> ejbs) {
        this.identifier = identifier;
        this.war = war;
        this.ejbs = ejbs;
    }

    public DeploymentModuleIdentifier getIdentifier() {
        return identifier;
    }

    public ClusteredWarDeploymentInformation getWar() {
        return war;
    }

    public Map<String, ClusteredEjbDeploymentInformation> getEjbs() {
        return ejbs;
    }

    @Override
    public String toString() {
        return "ClusteredModuleDeployment: " +
                "identifier = " + identifier +
                ", sessionCacheContainer = " + (war != null ? war.toString() : "N/A") +
                ", sessionCache = " + (ejbs != null ? ejbs.toString() : "N/A");
    }
}
