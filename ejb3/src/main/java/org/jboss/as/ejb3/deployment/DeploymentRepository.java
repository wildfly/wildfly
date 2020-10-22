/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import java.util.Map;

/**
 * @author Radoslav Husar
 */
public interface DeploymentRepository {

    void add(DeploymentModuleIdentifier identifier, ModuleDeployment deployment);

    boolean startDeployment(DeploymentModuleIdentifier identifier);

    void addListener(DeploymentRepositoryListener listener);

    void removeListener(DeploymentRepositoryListener listener);

    void remove(DeploymentModuleIdentifier identifier);

    void suspend();

    void resume();

    boolean isSuspended();

    /**
     * Returns all deployments. These deployments may not be in a started state, i.e. not all components might be ready to receive invocations.
     *
     * @return all deployments
     */
    Map<DeploymentModuleIdentifier, ModuleDeployment> getModules();

    /**
     * Returns all deployments that are in a started state, i.e. all components are ready to receive invocations.
     *
     * @return all started deployments
     */
    Map<DeploymentModuleIdentifier, ModuleDeployment> getStartedModules();

}
