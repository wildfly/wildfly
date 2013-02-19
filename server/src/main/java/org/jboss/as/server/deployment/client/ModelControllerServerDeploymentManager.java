/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
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
package org.jboss.as.server.deployment.client;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.standalone.impl.AbstractServerDeploymentManager;
import org.jboss.dmr.ModelNode;

/**
 * {@link org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager} the uses a {@link org.jboss.as.controller.ModelController}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class ModelControllerServerDeploymentManager extends AbstractServerDeploymentManager {

    private final ModelControllerClient client;

    public ModelControllerServerDeploymentManager(final ModelController controller) {
        this.client = controller.createClient(Executors.newCachedThreadPool());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Future<ModelNode> executeOperation(Operation executionContext) {
        return client.executeAsync(executionContext, null);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
