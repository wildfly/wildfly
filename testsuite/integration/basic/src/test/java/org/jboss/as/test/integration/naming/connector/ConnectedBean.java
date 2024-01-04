/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.connector;

import jakarta.ejb.Stateless;
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
