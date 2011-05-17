/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.arquillian.service;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

public class ArquillianDependencyProcessor {

    private static ModuleIdentifier ARQUILLIAN_JUNIT_ID = ModuleIdentifier.create("org.jboss.arquillian.junit");
    private static ModuleIdentifier SHRINKWRAP_ID = ModuleIdentifier.create("org.jboss.shrinkwrap.api");
    private static ModuleIdentifier JUNIT_ID = ModuleIdentifier.create("org.junit");

    private final DeploymentUnit deploymentUnit;

    ArquillianDependencyProcessor(DeploymentUnit deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
    }

    void deploy() {
        final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (moduleSpec != null) {
            ModuleLoader moduleLoader = Module.getBootModuleLoader();
            moduleSpec.addDependency(new ModuleDependency(moduleLoader, ARQUILLIAN_JUNIT_ID, false, false, false));
            moduleSpec.addDependency(new ModuleDependency(moduleLoader, SHRINKWRAP_ID, false, false, false));
            moduleSpec.addDependency(new ModuleDependency(moduleLoader, JUNIT_ID, false, false, false));
        }
    }
}
