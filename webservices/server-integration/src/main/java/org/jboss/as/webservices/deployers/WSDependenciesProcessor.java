/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.webservices.deployers;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * A DUP that sets the WS dependencies
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSDependenciesProcessor implements DeploymentUnitProcessor {

    public static final ModuleIdentifier JBOSSWS_API = ModuleIdentifier.create("org.jboss.ws.api");
    public static final ModuleIdentifier JBOSSWS_SPI = ModuleIdentifier.create("org.jboss.ws.spi");
    public static final ModuleIdentifier[] JAVAEE_APIS = {
            ModuleIdentifier.create("javax.jws.api"),
            ModuleIdentifier.create("javax.xml.soap.api"),
            ModuleIdentifier.create("javax.xml.ws.api")
    };

    private final boolean addJBossWSDependencies;

    public WSDependenciesProcessor(boolean addJBossWSDependencies) {
        this.addJBossWSDependencies = addJBossWSDependencies;
    }

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (addJBossWSDependencies) {
            moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, JBOSSWS_API, false, true, true, false));
            moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, JBOSSWS_SPI, false, true, true, false));
        }
        for(ModuleIdentifier api : JAVAEE_APIS) {
            moduleSpec.addSystemDependency(new ModuleDependency(moduleLoader, api, false, false, true, false));
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }

}
