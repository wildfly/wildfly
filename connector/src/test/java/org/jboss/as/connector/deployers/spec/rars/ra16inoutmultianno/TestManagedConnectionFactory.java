/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2009, Red Hat Inc, and individual contributors
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
package org.jboss.as.connector.deployers.spec.rars.ra16inoutmultianno;

import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ManagedConnection;

import org.jboss.as.connector.deployers.spec.rars.BaseManagedConnectionFactory;
import org.jboss.as.connector.deployers.spec.rars.TestConnection;
import org.jboss.as.connector.deployers.spec.rars.TestConnectionInterface;

/**
 * TestManagedConnectionFactory
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>
 */
@ConnectionDefinition(connectionFactory = ManagedConnection.class, connectionFactoryImpl = TestManagedConnection.class, connection = TestConnectionInterface.class, connectionImpl = TestConnection.class)
public class TestManagedConnectionFactory extends BaseManagedConnectionFactory {
    private static final long serialVersionUID = 1L;

    @ConfigProperty(type = String.class, defaultValue = "JCA")
    private String myStringProperty;

    /**
     * @param myStringProperty the myStringProperty to set
     */
    public void setMyStringProperty(String myStringProperty) {
        this.myStringProperty = myStringProperty;
    }

    /**
     * @return the myStringProperty
     */
    public String getMyStringProperty() {
        return myStringProperty;
    }
}
