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

package org.jboss.as.server.deployment;

import java.util.EnumMap;
import java.util.List;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service wrapper for {@link org.jboss.as.server.deployment.DeployerChains}.
 *
 * @author John Bailey
 */
public class DeployerChainsService implements Service<DeployerChains> {
    private final DeployerChains deployerChains;

    public static void addService(final ServiceTarget serviceTarget, final EnumMap<Phase, List<RegisteredDeploymentUnitProcessor>> phases, final ServiceVerificationHandler verificationHandler) {
        final DeployerChains deployerChains = new DeployerChains(phases);
        serviceTarget.addService(Services.JBOSS_DEPLOYMENT_CHAINS, new DeployerChainsService(deployerChains))
            .addListener(verificationHandler)
            .install();
    }

    public DeployerChainsService(DeployerChains deployerChains) {
        this.deployerChains = deployerChains;
    }

    public void start(StartContext context) throws StartException {
    }

    public void stop(StopContext context) {
    }

    public DeployerChains getValue() throws IllegalStateException, IllegalArgumentException {
        return deployerChains;
    }
}
