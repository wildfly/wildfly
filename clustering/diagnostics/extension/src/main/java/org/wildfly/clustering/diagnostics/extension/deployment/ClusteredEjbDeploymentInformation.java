package org.wildfly.clustering.diagnostics.extension.deployment;

import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;

/**
 * Information representing a <distributable> war module deployment.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusteredEjbDeploymentInformation {

    private DeploymentModuleIdentifier identifier ;
    private String beanName;
    private String sessionCacheContainer;
    private String sessionCache;

    public ClusteredEjbDeploymentInformation(DeploymentModuleIdentifier identifier, String beanName) {
        this.identifier = identifier;
        this.beanName = beanName ;
    }

    public DeploymentModuleIdentifier getIdentifier() {
        return identifier;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getSessionCacheContainer() {
        return sessionCacheContainer;
    }

    public void setSessionCacheContainer(String sessionCacheContainer) {
        this.sessionCacheContainer = sessionCacheContainer;
    }

    public String getSessionCache() {
        return sessionCache;
    }

    public void setSessionCache(String sessionCache) {
        this.sessionCache = sessionCache;
    }

    @Override
    public String toString() {
        return "ClusteredEjbDeploymentInformation: " +
                "identifier = " + identifier +
                "beanName = " + beanName +
                ", sessionCacheContainer = " + sessionCacheContainer +
                ", sessionCache = " + sessionCache ;
    }
}
