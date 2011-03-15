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

package org.jboss.as.controller.client.helpers.domain.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

import org.jboss.as.controller.client.helpers.domain.DeploymentPlan;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanResult;
import org.jboss.as.controller.client.helpers.domain.DomainDeploymentManager;
import org.jboss.as.controller.client.helpers.domain.DuplicateDeploymentNameException;
import org.jboss.as.controller.client.helpers.domain.InitialDeploymentPlanBuilder;

/**
 * Client-side {@link DomainDeploymentManager}.
 *
 * @author Brian Stansberry
 */
class DomainDeploymentManagerImpl implements DomainDeploymentManager {

    private final DomainClientImpl client;
    private final DeploymentContentDistributor contentDistributor;

    DomainDeploymentManagerImpl(final DomainClientImpl client) {
        assert client != null : "client is null";
        this.client = client;

        this.contentDistributor = new DeploymentContentDistributor() {
            @Override
            public byte[] distributeDeploymentContent(String name, String runtimeName, InputStream stream)
                    throws IOException, DuplicateDeploymentNameException {
                boolean unique = DomainDeploymentManagerImpl.this.client.isDeploymentNameUnique(name);
                if (!unique) {
                    throw new DuplicateDeploymentNameException(name, false);
                }
                return DomainDeploymentManagerImpl.this.client.addDeploymentContent(stream);
            }
            @Override
            public byte[] distributeReplacementDeploymentContent(String name, String runtimeName, InputStream stream)
                    throws IOException {
                return DomainDeploymentManagerImpl.this.client.addDeploymentContent(stream);
            }
        };
    }

    @Override
    public Future<DeploymentPlanResult> execute(DeploymentPlan plan) {
        return this.client.execute(plan);
    }

    @Override
    public InitialDeploymentPlanBuilder newDeploymentPlan() {
        return InitialDeploymentPlanBuilderFactory.newInitialDeploymentPlanBuilder(this.contentDistributor);
    }

}
