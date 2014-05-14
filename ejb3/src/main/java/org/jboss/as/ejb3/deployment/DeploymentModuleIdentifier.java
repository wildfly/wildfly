/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.deployment;

import org.jboss.as.ejb3.logging.EjbLogger;

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
            throw EjbLogger.ROOT_LOGGER.paramCannotBeNull("Application name");
        }
        if (moduleName == null) {
            throw EjbLogger.ROOT_LOGGER.paramCannotBeNull("Module name");
        }
        if (distinctName == null) {
            throw EjbLogger.ROOT_LOGGER.paramCannotBeNull("Distinct name");
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
