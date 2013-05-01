/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.managedbean.processors;

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
import org.jboss.modules.filter.PathFilters;

/**
 * Deployment processor which adds the java EE APIs to EE deployments
 * <p/>
 *
 * @author John E. Bailey
 * @author Jason T. Greene
 * @author Stuart Douglas
 */
public class JavaEEDependencyProcessor implements DeploymentUnitProcessor {

    private static ModuleIdentifier HIBERNATE_VALIDATOR_ID = ModuleIdentifier.create("org.hibernate.validator");

    private static ModuleIdentifier JBOSS_INVOCATION_ID = ModuleIdentifier.create("org.jboss.invocation");
    private static ModuleIdentifier JBOSS_AS_EE = ModuleIdentifier.create("org.jboss.as.ee");

    private static final ModuleIdentifier[] JAVA_EE_API_MODULES = {
            ModuleIdentifier.create("javax.activation.api"),
            ModuleIdentifier.create("javax.annotation.api"),
            ModuleIdentifier.create("javax.ejb.api"),
            ModuleIdentifier.create("javax.el.api"),
            ModuleIdentifier.create("javax.enterprise.api"),
            ModuleIdentifier.create("javax.inject.api"),
            ModuleIdentifier.create("javax.interceptor.api"),
            ModuleIdentifier.create("javax.json.api"),
            ModuleIdentifier.create("javax.jms.api"),
            ModuleIdentifier.create("javax.jws.api"),
            ModuleIdentifier.create("javax.mail.api"),
            ModuleIdentifier.create("javax.management.j2ee.api"),
            ModuleIdentifier.create("javax.persistence.api"),
            ModuleIdentifier.create("javax.resource.api"),
            ModuleIdentifier.create("javax.rmi.api"),
            ModuleIdentifier.create("javax.security.auth.message.api"),
            ModuleIdentifier.create("javax.security.jacc.api"),
            ModuleIdentifier.create("javax.servlet.api"),
            ModuleIdentifier.create("javax.servlet.jsp.api"),
            ModuleIdentifier.create("javax.transaction.api"),
            ModuleIdentifier.create("javax.validation.api"),
            ModuleIdentifier.create("javax.ws.rs.api"),
            ModuleIdentifier.create("javax.websocket.api"),
            ModuleIdentifier.create("javax.xml.bind.api"),
            ModuleIdentifier.create("javax.xml.soap.api"),
            ModuleIdentifier.create("javax.xml.ws.api"),
            ModuleIdentifier.create("javax.api")
    };


    /**
     * Add the EE APIs as a dependency to all deployments
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        // TODO: Post 7.0, we have to rethink this whole hibernate dependencies that we add to user deployments
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, HIBERNATE_VALIDATOR_ID, false, false, true, false));

        //add jboss-invocation classes needed by the proxies
        ModuleDependency invocation = new ModuleDependency(moduleLoader, JBOSS_INVOCATION_ID, false, false, false, false);
        invocation.addImportFilter(PathFilters.is("org.jboss.invocation.proxy.classloading"), true);
        moduleSpecification.addSystemDependency(invocation);

        ModuleDependency ee = new ModuleDependency(moduleLoader, JBOSS_AS_EE, false, false, false, false);
        ee.addImportFilter(PathFilters.is("org.jboss.as.ee.component.serialization"), true);
        moduleSpecification.addSystemDependency(ee);


        //we always add all Java EE API modules, as the platform spec requires them to always be available
        //we do not just add the javaee.api module, as this breaks excludes

        for (final ModuleIdentifier moduleIdentifier : JAVA_EE_API_MODULES) {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleIdentifier, false, false, true, false));
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
