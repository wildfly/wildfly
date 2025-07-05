/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;

/**
 * Defines the configuration of a deployment.
 * TODO Relocate to SPI module
 * @author Paul Ferraro
 */
public interface WebDeploymentConfiguration {

    /**
     * Returns the target server name of this deployment
     * @return a server name
     */
    String getServerName();

    /**
     * Returns the target host name of this deployment
     * @return a host name
     */
    String getHostName();

    /**
     * Returns the name of this deployment
     * @return a deployment name
     */
    default String getDeploymentName() {
        DeploymentUnit unit = this.getDeploymentUnit();
        DeploymentUnit parentUnit = unit.getParent();
        return (parentUnit != null) ? String.join(".", List.of(parentUnit.getName(), unit.getName())) : unit.getName();
    }

    /**
     * Returns the deployment module
     * @return the deployment module
     */
    default Module getModule() {
        return this.getDeploymentUnit().getAttachment(Attachments.MODULE);
    }

    /**
     * The deployment unit with which this session manager factory is to be associated.
     * @return a deployment unit
     */
    DeploymentUnit getDeploymentUnit();
}
