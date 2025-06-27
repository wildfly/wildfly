/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.service.deployment;

import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * @author Paul Ferraro
 */
public interface WebDeploymentConfiguration extends org.wildfly.clustering.server.deployment.DeploymentConfiguration {

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
