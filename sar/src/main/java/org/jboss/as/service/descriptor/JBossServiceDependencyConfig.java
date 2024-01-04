/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service.descriptor;

import java.io.Serializable;

/**
 * Configuration for a service dependency.
 *
 * @author John E. Bailey
 */
public class JBossServiceDependencyConfig implements Serializable {
    private static final long serialVersionUID = 7058092116435789802L;

    private String dependencyName;
    private JBossServiceConfig serviceConfig;
    private String proxyType;
    private String optionalAttributeName;

    public String getDependencyName() {
        return dependencyName;
    }

    public void setDependencyName(String dependencyName) {
        this.dependencyName = dependencyName;
    }

    public JBossServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(JBossServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public String getOptionalAttributeName() {
        return optionalAttributeName;
    }

    public void setOptionalAttributeName(String optionalAttributeName) {
        this.optionalAttributeName = optionalAttributeName;
    }
}
