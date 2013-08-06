package org.wildfly.clustering.monitor.extension.deployment;

import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;

/**
 * Information representing a <distributable> war module deployment.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusteredWarDeploymentInformation {

    private DeploymentModuleIdentifier identifier ;
    private String sessionCacheContainer;
    private String sessionCache;

    public ClusteredWarDeploymentInformation(DeploymentModuleIdentifier identifier, String sessionCacheContainer, String sessionCache) {
        this.identifier = identifier;
        this.sessionCacheContainer = sessionCacheContainer;
        this.sessionCache = sessionCache;
    }

    public DeploymentModuleIdentifier getIdentifier() {
        return identifier;
    }

    public String getSessionCacheContainer() {
        return sessionCacheContainer;
    }

    public String getSessionCache() {
        return sessionCache;
    }

    @Override
    public String toString() {
        return "ClusteredWarDeploymentInformation: " +
                "identifier = " + identifier +
                ", sessionCacheContainer = " + sessionCacheContainer +
                ", sessionCache = " + sessionCache ;
    }
}
