/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote.protocol.versionone;

import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.deployment.ModuleDeployment;

/**
 * @author Jaikiran Pai
 */
abstract class EJBIdentifierBasedMessageHandler extends AbstractMessageHandler {

    protected final DeploymentRepository deploymentRepository;

    EJBIdentifierBasedMessageHandler(final DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    protected EjbDeploymentInformation findEJB(final String appName, final String moduleName, final String distinctName, final String beanName) {
        final DeploymentModuleIdentifier ejbModule = new DeploymentModuleIdentifier(appName, moduleName, distinctName);
        final ModuleDeployment moduleDeployment = this.deploymentRepository.getModules().get(ejbModule);
        if (moduleDeployment == null) {
            return null;
        }
        return moduleDeployment.getEjbs().get(beanName);
    }


}
