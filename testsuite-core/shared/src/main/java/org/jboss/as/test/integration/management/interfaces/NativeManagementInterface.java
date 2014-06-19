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

package org.jboss.as.test.integration.management.interfaces;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.rbac.RbacAdminCallbackHandler;
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class NativeManagementInterface implements ManagementInterface {
    private static final Map<String, String> SASL_OPTIONS = Collections.singletonMap("SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");

    private final ModelControllerClient client;

    public NativeManagementInterface(ModelControllerClient client) {
        this.client = client;
    }

    @Override
    public ModelNode execute(ModelNode operation) {
        try {
            return client.execute(operation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ManagementInterface create(String host, int port, final String username, final String password) {
        try {
            ModelControllerClient client = ModelControllerClient.Factory.create(host, port,
                    new RbacAdminCallbackHandler(username, password),  SASL_OPTIONS);
            return new NativeManagementInterface(client);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
