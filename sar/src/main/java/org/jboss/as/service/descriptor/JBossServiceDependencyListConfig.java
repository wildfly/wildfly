package org.jboss.as.service.descriptor;

import java.io.Serializable;

public class JBossServiceDependencyListConfig implements Serializable {

    private static final long serialVersionUID = 634835167098065568L;

    private String optionalAttributeName;
    private JBossServiceDependencyConfig[] dependencyConfigs;

    public String getOptionalAttributeName() {
        return optionalAttributeName;
    }
    public void setOptionalAttributeName(String optionalAttributeName) {
        this.optionalAttributeName = optionalAttributeName;
    }
    public JBossServiceDependencyConfig[] getDependencyConfigs() {
        return dependencyConfigs;
    }
    public void setDependencyConfigs(JBossServiceDependencyConfig[] dependencyConfigs) {
        this.dependencyConfigs = dependencyConfigs;
    }
}
