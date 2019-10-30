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

package org.jboss.as.test.integration.naming.connector;

import javax.ejb.Stateless;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnection;
import javax.management.remote.rmi.RMIServer;
import javax.naming.InitialContext;

/**
 * Simple bean to check access MBean server count through JMXConnector and JNDI.
 *
 * @author baranowb
 * @author Eduardo Martins
 */
@Stateless
public class ConnectedBean implements ConnectedBeanInterface {

    public int getMBeanCountFromConnector(JMXServiceURL jmxServiceURL) throws Exception {
        final JMXConnector connector = JMXConnectorFactory.connect(jmxServiceURL, null);
        try {
            return connector.getMBeanServerConnection().getMBeanCount();
        } finally {
            if (connector != null) {
                connector.close();
            }
        }
    }

    public int getMBeanCountFromJNDI(String rmiServerJndiName) throws Exception {
        final RMIServer rmiServer = InitialContext.doLookup(rmiServerJndiName);
        final RMIConnection rmiConnection = rmiServer.newClient(null);
        try {
            return rmiConnection.getMBeanCount(null);
        } finally {
            if (rmiConnection != null) {
                rmiConnection.close();
            }
        }
    }
}
