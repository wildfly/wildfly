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

package org.jboss.as.deployment.chain;

import org.jboss.as.deployment.Phase;
import org.jboss.as.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.deployment.module.ModuleConfigProcessor;
import org.jboss.as.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.naming.ModuleContextProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.deployment.processor.ServiceActivatorDependencyProcessor;
import org.jboss.as.deployment.processor.ServiceActivatorProcessor;

/**
 * Service activator which installs the various service required for jar deployments.
 *
 * @author John E. Bailey
 */
public class JarDeploymentActivator {

    /**
     * Activate the services required for service deployments.
     *
     * @param deploymentChain The deployment chain
     */
    public void activate(final DeploymentChain deploymentChain) {
        deploymentChain.addProcessor(new ManifestAttachmentProcessor(), Phase.MANIFEST_ATTACHMENT_PROCESSOR);
        deploymentChain.addProcessor(new AnnotationIndexProcessor(),  Phase.ANNOTATION_INDEX_PROCESSOR);
        deploymentChain.addProcessor(new ModuleDependencyProcessor(), Phase.MODULE_DEPENDENCY_PROCESSOR);
        deploymentChain.addProcessor(new ModuleConfigProcessor(), Phase.MODULE_CONFIG_PROCESSOR);
        deploymentChain.addProcessor(new ModuleDeploymentProcessor(), Phase.MODULE_DEPLOYMENT_PROCESSOR);
        deploymentChain.addProcessor(new ModuleContextProcessor(), Phase.MODULE_CONTEXT_PROCESSOR);
        deploymentChain.addProcessor(new ServiceActivatorDependencyProcessor(), Phase.SERVICE_ACTIVATION_DEPENDENCY_PROCESSOR);
        deploymentChain.addProcessor(new ServiceActivatorProcessor(), Phase.SERVICE_ACTIVATOR_PROCESSOR);
    }
}
