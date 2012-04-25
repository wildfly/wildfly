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

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.InitialContext;

import org.xnio.IoUtils;

/**
 * Simple bean to check if we can init everything properly.
 * 
 * @author baranowb
 * 
 */
@Singleton
@Startup
public class ConnectedBean implements ConnectedBeanInterface {

    public void testConnector(String uri) throws Exception {
        JMXConnector connector = null;
        MBeanServerConnection connection = null;
        try {
            String urlString = System.getProperty("jmx.service.url", uri);
            JMXServiceURL serviceURL = new JMXServiceURL(urlString);
            connector = JMXConnectorFactory.connect(serviceURL, null);
            connection = connector.getMBeanServerConnection();
        } finally {
            if (connector != null) {
                IoUtils.safeClose(connector);
            }
        }
    }

    public boolean testDirectLookup(String uri) throws Exception {

        InitialContext ic = new InitialContext();
        Object o = ic.lookup(uri);
        if (o != null) {
            return true;
        } else {
            return false;
        }

    }
}
