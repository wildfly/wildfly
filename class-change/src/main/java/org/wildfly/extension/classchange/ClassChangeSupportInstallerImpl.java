/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.wildfly.extension.classchange;

import org.jboss.as.server.DeployerChainAddHandler;
import org.jboss.as.server.ServerService;
import org.jboss.as.server.deployment.Phase;
import org.wildfly.extension.classchange.logging.ClassChangeMessages;

public class ClassChangeSupportInstallerImpl implements ClassChangeSupportInstaller {

    public ClassChangeSupportInstallerImpl() {
        try {
            //we want this to fail if Fakereplace is not present
            ClassLoader.getSystemClassLoader().loadClass("org.fakereplace.core.Fakereplace");
            ClassChangeMessages.ROOT_LOGGER.startedWithFakereplace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public void install() {
        DeployerChainAddHandler.addDeploymentProcessor(ServerService.SERVER_NAME, Phase.STRUCTURE, Phase.STRUCTURE_DEPENDENCIES_MANIFEST + 20, new ClassChangeDeploymentUnitProcessor());
        DeployerChainAddHandler.addDeploymentProcessor(ServerService.SERVER_NAME, Phase.FIRST_MODULE_USE, Phase.FIRST_MODULE_USE_PERSISTENCE_CLASS_FILE_TRANSFORMER - 20, new ClassChangeModuleDeploymentUnitProcessor());
    }
}
