/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.injection.deployment;

import static jakarta.jms.JMSContext.AUTO_ACKNOWLEDGE;
import static org.wildfly.extension.messaging.activemq.injection.deployment.DefaultJMSConnectionFactoryBinding.COMP_DEFAULT_JMS_CONNECTION_FACTORY;

import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSPasswordCredential;
import jakarta.jms.JMSSessionMode;

import org.jboss.metadata.property.PropertyReplacer;

/**
 * Data structure containing the Jakarta Messaging information that can be annotated on an injected JMSContext.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
class JMSInfo {

    private final String connectionFactoryLookup;
    private final String userName;
    private final String password;
    private final int sessionMode;

    JMSInfo(JMSConnectionFactory connectionFactory, JMSPasswordCredential credential, JMSSessionMode sessionMode) {
        PropertyReplacer propertyReplacer = JMSCDIExtension.propertyReplacer;
        if (connectionFactory != null) {
            connectionFactoryLookup = propertyReplacer.replaceProperties(connectionFactory.value());
        } else {
            connectionFactoryLookup = COMP_DEFAULT_JMS_CONNECTION_FACTORY;
        }
        if (credential != null) {
            userName = propertyReplacer.replaceProperties(credential.userName());
            password = propertyReplacer.replaceProperties(credential.password());
        } else {
            userName = null;
            password = null;
        }
        if (sessionMode != null) {
            this.sessionMode = sessionMode.value();
        } else {
            this.sessionMode = AUTO_ACKNOWLEDGE;
        }
    }

    String getConnectionFactoryLookup() {
        return connectionFactoryLookup;
    }

    String getUserName() {
        return userName;
    }

    String getPassword() {
        return password;
    }

    int getSessionMode() {
        return sessionMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JMSInfo jmsInfo = (JMSInfo) o;

        if (sessionMode != jmsInfo.sessionMode) return false;
        if (connectionFactoryLookup != null ? !connectionFactoryLookup.equals(jmsInfo.connectionFactoryLookup) : jmsInfo.connectionFactoryLookup != null)
            return false;
        if (password != null ? !password.equals(jmsInfo.password) : jmsInfo.password != null) return false;
        if (userName != null ? !userName.equals(jmsInfo.userName) : jmsInfo.userName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = connectionFactoryLookup != null ? connectionFactoryLookup.hashCode() : 0;
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + sessionMode;
        return result;
    }
}
