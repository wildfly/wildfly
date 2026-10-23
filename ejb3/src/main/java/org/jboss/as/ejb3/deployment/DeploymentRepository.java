/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment;

import java.util.Map;

import org.jboss.ejb.client.EJBModuleIdentifier;

/**
 * @author Radoslav Husar
 */
public interface DeploymentRepository {

    void add(EJBModuleIdentifier moduleId, ModuleDeployment deployment);

    boolean startDeployment(EJBModuleIdentifier moduleId);

    void remove(EJBModuleIdentifier moduleId);

    boolean isSuspended();

    /**
     * Returns all deployments. These deployments may not be in a started state, i.e. not all components might be ready to receive invocations.
     *
     * @return all deployments
     */
    Map<EJBModuleIdentifier, ModuleDeployment> getModules();

    /**
     * Returns all deployments that are in a started state, i.e. all components are ready to receive invocations.
     *
     * @return all started deployments
     */
    Map<EJBModuleIdentifier, ModuleDeployment> getStartedModules();

}
