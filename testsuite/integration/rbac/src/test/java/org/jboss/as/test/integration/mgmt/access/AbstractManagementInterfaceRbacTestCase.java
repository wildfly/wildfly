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

package org.jboss.as.test.integration.mgmt.access;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.interfaces.ManagementInterface;
import org.junit.AfterClass;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public abstract class AbstractManagementInterfaceRbacTestCase {
    private static final Map<String, ManagementInterface> clients = new HashMap<String, ManagementInterface>();

    @ContainerResource
    private ManagementClient managementClient;

    @AfterClass
    public static void cleanUpClients() {
        for (ManagementInterface client : clients.values()) {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
        clients.clear();
    }

    public ManagementInterface getClientForUser(String userName) {
        ManagementInterface result = clients.get(userName);
        if (result == null) {
            result = createClient(userName);
            clients.put(userName, result);
        }
        return result;
    }

    protected abstract ManagementInterface createClient(String userName);

    public static void removeClientForUser(String userName) {
        ManagementInterface client = clients.remove(userName);
        if (client != null) {
            client.close();
        }
    }

    protected final ManagementClient getManagementClient() {
        return managementClient;
    }
}
