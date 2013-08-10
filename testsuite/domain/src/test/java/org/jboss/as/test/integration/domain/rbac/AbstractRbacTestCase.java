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

package org.jboss.as.test.integration.domain.rbac;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfiguration;
import org.jboss.as.test.integration.management.rbac.RbacAdminCallbackHandler;
import org.junit.AfterClass;

/**
 * Base class for RBAC tests.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public abstract class AbstractRbacTestCase {

    private static final Map<String, ModelControllerClient> nonLocalAuthclients = new HashMap<String, ModelControllerClient>();
    private static final Map<String, ModelControllerClient> localAuthClients = new HashMap<String, ModelControllerClient>();

    private static final Map<String, String> SASL_OPTIONS = Collections.singletonMap("SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");

    @AfterClass
    public static void cleanUpClients() {

        try {
            cleanUpClients(nonLocalAuthclients);
        } finally {
            cleanUpClients(localAuthClients);
        }

    }

    private static void cleanUpClients(Map<String, ModelControllerClient> clients) {

        for (ModelControllerClient client : clients.values()) {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }

    }


    public ModelControllerClient getClientForUser(String userName, boolean allowLocalAuth,
                                                  JBossAsManagedConfiguration clientConfig) throws UnknownHostException {
        Map<String, ModelControllerClient> clients = allowLocalAuth ? localAuthClients : nonLocalAuthclients;
        ModelControllerClient result = clients.get(userName);
        if (result == null) {
            result = createClient(userName, allowLocalAuth, clientConfig);
            clients.put(userName, result);
        }
        return result;
    }

    private ModelControllerClient createClient(String userName, boolean allowLocalAuth,
                                               JBossAsManagedConfiguration clientConfig) throws UnknownHostException {

        return ModelControllerClient.Factory.create(clientConfig.getHostControllerManagementProtocol(),
                clientConfig.getHostControllerManagementAddress(),
                clientConfig.getHostControllerManagementPort(),
                new RbacAdminCallbackHandler(userName),
                allowLocalAuth ? Collections.<String, String>emptyMap() : SASL_OPTIONS);
    }

    public static void removeClientForUser(String userName, boolean allowLocalAuth) throws IOException {
        Map<String, ModelControllerClient> clients = allowLocalAuth ? localAuthClients : nonLocalAuthclients;
        ModelControllerClient client = clients.remove(userName);
        if (client != null) {
            client.close();
        }
    }

}
