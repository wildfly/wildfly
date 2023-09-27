/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars.ra16inoutmultianno;

import jakarta.resource.spi.ConfigProperty;
import jakarta.resource.spi.ConnectionDefinition;
import jakarta.resource.spi.ManagedConnection;

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
