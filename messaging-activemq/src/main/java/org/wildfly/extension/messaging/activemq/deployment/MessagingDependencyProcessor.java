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

package org.wildfly.extension.messaging.activemq.deployment;

import org.jboss.as.ee.weld.WeldDeploymentMarker;
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
 * Processor that add module dependencies for JMS deployments.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class MessagingDependencyProcessor implements DeploymentUnitProcessor {

    /**
     * We include this module so that the CDI producer method for JMSContext is available for the deployment unit.
     */
    public static final ModuleIdentifier AS_MESSAGING = ModuleIdentifier.create("org.wildfly.extension.messaging-activemq");
    public static final ModuleIdentifier JMS_API = ModuleIdentifier.create("javax.jms.api");
    public static final ModuleIdentifier JTS = ModuleIdentifier.create("org.jboss.jts");

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        addDependency(moduleSpecification, moduleLoader, JMS_API);

        if (WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            addDependency(moduleSpecification, moduleLoader, AS_MESSAGING);
            // The messaging-activemq subsystem provides support for injected JMSContext.
            // one of the beans has a @TransactionScoped scope which requires the CDI context
            // provided by Narayana in the org.jboss.jts module.
            // @see CDIDeploymentProcessor
            addDependency(moduleSpecification, moduleLoader, JTS);
        }

    }

    private void addDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader,
                               ModuleIdentifier moduleIdentifier) {
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, moduleIdentifier, false, false, true, false));
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
