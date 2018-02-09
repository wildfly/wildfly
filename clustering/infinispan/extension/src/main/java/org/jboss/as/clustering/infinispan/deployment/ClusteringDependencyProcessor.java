/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * {@link DeploymentUnitProcessor} that adds the clustering api to the deployment classpath.
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class ClusteringDependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleIdentifier API = ModuleIdentifier.create("org.wildfly.clustering.api");
    private static final ModuleIdentifier MARSHALLING_API = ModuleIdentifier.create("org.wildfly.clustering.marshalling.api");
    private static final ModuleIdentifier SINGLETON_API = ModuleIdentifier.create("org.wildfly.clustering.singleton");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, API, true, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, MARSHALLING_API, true, false, false, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, SINGLETON_API, true, false, false, false));
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}