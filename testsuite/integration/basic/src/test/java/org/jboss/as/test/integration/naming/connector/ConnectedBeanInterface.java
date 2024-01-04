/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.connector;

import jakarta.ejb.Remote;
import javax.management.remote.JMXServiceURL;


/**
 * @author baranowb
 * @author Eduardo Martins
 *
 */
@Remote
public interface ConnectedBeanInterface {

    int getMBeanCountFromConnector(JMXServiceURL jmxServiceURL) throws Exception;

    int getMBeanCountFromJNDI(String rmiServerJndiName) throws Exception;
}
