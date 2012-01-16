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
package org.jboss.as.jdr;

import static org.jboss.as.jdr.JdrMessages.MESSAGES;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 *  Wrapper for {@link org.jboss.as.controller.client.ModelControllerClient}
 *  used for easier jython consumption.
 *
 *  @author Jesse Jaggars
 *  @author Mike M. Clark
 */
public class ModelControllerClientProxy {

    public ModelControllerClient client;

    /**
     * Creates a proxy to wrap a {@link org.jboss.as.controller.client.ModelControllerClient}
     * to ease passing into python code.
     *
     * @param client <code>ModelControllerClient</code> being wrapped
     * @throws IllegalArgumentException if <code>client</code> is <code>null</code>
     */
    public ModelControllerClientProxy(ModelControllerClient client) {
        if (client == null) {
            throw MESSAGES.varNull("client");
        }
        this.client = client;
    }

    public ModelNode execute(ModelNode request) throws java.io.IOException {
        return client.execute(request);
    }
}
