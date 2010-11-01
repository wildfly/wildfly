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
package org.jboss.as.web.deployment;

import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.module.ModuleConfig;
import org.jboss.as.deployment.module.ModuleDependencies;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.modules.ModuleIdentifier;

/**
 * Module dependencies processor.
 *
 * @author Emanuel Muckenhuber
 */
public class WarClassloadingDependencyProcessor implements DeploymentUnitProcessor {

    public static final long PRIORITY = DeploymentPhases.MODULE_DEPENDENCIES.plus(300L);

    private static ModuleIdentifier JAVAEE_API_ID = ModuleIdentifier.create("javaee.api");
    private static final ModuleIdentifier JBOSS_WEB = ModuleIdentifier.create("org.jboss.as.jboss-as-web");
    private static final ModuleIdentifier SYSTEM = ModuleIdentifier.create("system");
    private static final ModuleIdentifier LOG = ModuleIdentifier.create("org.jboss.logging");

    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        // Add module dependencies on Java EE apis
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(JAVAEE_API_ID, true, false, false));

        // FIXME we need to revise the exports of the web module, so that we
        // don't export our internals
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(JBOSS_WEB, true, false, false));
        // JFC hack...
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(SYSTEM, true, false, false));
        ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(LOG, true, false, false));
    }

}
