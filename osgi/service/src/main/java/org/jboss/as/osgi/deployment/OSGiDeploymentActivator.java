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

package org.jboss.as.osgi.deployment;

import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;

/**
 * Installs the various {@link DeploymentUnitProcessor}s required for OSGi deployments.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 07-Oct-2010
 */
public class OSGiDeploymentActivator {
    /**
     * Activate the services required for service deployments.
     */
    public void activate(final DeploymentProcessorTarget processorTarget) {
        processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_OSGI_MANIFEST, new OSGiManifestStructureProcessor());
        processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_OSGI_BUNDLE_INFO, new OSGiBundleInfoParseProcessor());
        processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_OSGI_XSERVICE_PROPERTIES, new OSGiXServiceParseProcessor());
        processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_OSGI_DEPLOYMENT, new BundleDeploymentProcessor());
        processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_OSGI_DEPLOYMENT, new BundleInstallProcessor());
        processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_OSGI_MODULE, new ModuleRegisterProcessor());
    }
}
