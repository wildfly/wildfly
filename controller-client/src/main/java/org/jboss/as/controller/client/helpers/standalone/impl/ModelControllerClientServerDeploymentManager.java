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
package org.jboss.as.controller.client.helpers.standalone.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Future;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;

/**
 * {@link org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager} that uses a {@link org.jboss.as.controller.client.ModelControllerClient}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class ModelControllerClientServerDeploymentManager extends AbstractServerDeploymentManager implements Closeable {

    private final ModelControllerClient client;
    private final boolean closeClient;

    public ModelControllerClientServerDeploymentManager(final ModelControllerClient client, final boolean closeClient) {
        this.client = client;
        this.closeClient = closeClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Future<ModelNode> executeOperation(Operation operation) {
        return client.executeAsync(operation, null);
    }

    @Override
    public void close() throws IOException {
        if(closeClient) {
            client.close();
        }
    }

}
