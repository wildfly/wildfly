package org.jboss.as.ejb3.deployment;

import java.io.Serializable;

/**
 * Identifier for a deployed module, consisting of application + distinct + module name.
 *
 * @author Stuart Douglas
 */
public final class DeploymentModuleIdentifier implements Serializable {

    private final String applicationName;
    private final String moduleName;
    private final String distinctName;

    public DeploymentModuleIdentifier(String applicationName, String moduleName, String distinctName) {
        if (applicationName == null) {
            throw new IllegalArgumentException("Application name cannot be null");
        }
        if (moduleName == null) {
            throw new IllegalArgumentException("Module name cannot be null");
        }
        if (distinctName == null) {
            throw new IllegalArgumentException("Distinct name cannot be null");
        }
        this.applicationName = applicationName;
        this.moduleName = moduleName;
        this.distinctName = distinctName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getDistinctName() {
        return distinctName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeploymentModuleIdentifier that = (DeploymentModuleIdentifier) o;

        if (!applicationName.equals(that.applicationName)) return false;
        if (distinctName != null ? !distinctName.equals(that.distinctName) : that.distinctName != null) return false;
        if (!moduleName.equals(that.moduleName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = applicationName.hashCode();
        result = 31 * result + moduleName.hashCode();
        result = 31 * result + (distinctName != null ? distinctName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeploymentModuleIdentifier{" +
                "applicationName='" + applicationName + '\'' +
                ", moduleName='" + moduleName + '\'' +
                ", distinctName='" + distinctName + '\'' +
                '}';
    }
}
