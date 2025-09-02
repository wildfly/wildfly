/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.deployment;

import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.server.deployment.DeploymentConfiguration;

/**
 * Encapsulates the configuration of a web deployment.
 * @author Paul Ferraro
 */
public interface WebDeploymentConfiguration extends DeploymentConfiguration {

    /**
     * Returns the deployment unit of this web deployment.
     * @return the deployment unit of this web deployment.
     */
    DeploymentUnit getDeploymentUnit();

    @Override
    default String getDeploymentName() {
        DeploymentUnit unit = this.getDeploymentUnit();
        DeploymentUnit parentUnit = unit.getParent();
        return (parentUnit != null) ? String.join(".", List.of(parentUnit.getName(), unit.getName())) : unit.getName();
    }

    @Override
    default ClassLoader getClassLoader() {
        return this.getDeploymentUnit().getAttachment(Attachments.MODULE).getClassLoader();
    }
}
